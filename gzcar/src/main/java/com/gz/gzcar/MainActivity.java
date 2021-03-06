package com.gz.gzcar;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.gprinter.aidl.GpService;
import com.gprinter.service.GpPrintService;
import com.gz.gzcar.Database.CarInfoTable;
import com.gz.gzcar.Database.MoneyTable;
import com.gz.gzcar.Database.TrafficInfoTable;
import com.gz.gzcar.Database.UserTable;
import com.gz.gzcar.device.LedModule;
import com.gz.gzcar.device.camera;
import com.gz.gzcar.module.carInfoProcess;
import com.gz.gzcar.module.delayTask;
import com.gz.gzcar.server.DelFileServer;
import com.gz.gzcar.server.DownLoadServer;
import com.gz.gzcar.server.SendService;
import com.gz.gzcar.settings.SettingActivity;
import com.gz.gzcar.utils.DateUtils;
import com.gz.gzcar.utils.L;
import com.gz.gzcar.utils.PrintBean;
import com.gz.gzcar.utils.PrintUtils;
import com.gz.gzcar.utils.SPUtils;
import com.gz.gzcar.weight.MyPullText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.gz.gzcar.MyApplication.daoConfig;
import static com.gz.gzcar.MyApplication.settingInfo;

public class MainActivity extends BaseActivity {

    private com.gz.gzcar.server.FileUtils mFileUtils = new com.gz.gzcar.server.FileUtils();
    DbManager db = x.getDb(daoConfig);
    public TrafficInfoTable outPortLog = new TrafficInfoTable();
    public String waitEnterCarNumber = "";
    // public FileUtils picFileManage = new FileUtils();
    public String loginUserName;
    //摄像机IP
    camera inCamera = new camera(this, "in", settingInfo.getString(AppConstants.IN_CAMERA_IP), true);
    camera outCamera = new camera(this, "out", settingInfo.getString(AppConstants.OUT_CAMERA_IP), true);
    //camera inAssistCamera = new camera(this, "in", settingInfo.getString(AppConstants.SUPPORT_IN_IP),false);
    //camera outAssistCamera = new camera(this, "out", settingInfo.getString(AppConstants.SUPPORT_OUT_IP),false);
    //实始化车辆处理模块
    carInfoProcess carProcess = new carInfoProcess(db, inCamera, outCamera);
    TextView plateTextIn; //入口车牌
    TextView plateTextOut;        //出口车牌
    ImageView plateImageIn;       //入口图片
    ImageView plateImageOut;      //出口图片
    ImageView videoStreamIn;      //入口视频
    ImageView videoStreamOut;     //出口视频
    Button buttonManualPassIn;    //手动入场
    Button buttonAgainIdentIn;    //入口重新识别
    Button buttonAgainIdentOut;   //出口重新识别
    Button buttonManualInOpen;    //入口手动起杆
    Button ButtonManualOutOpen;//选车出场
    public TextView chargeCarNumber;        //收费信息车号
    public TextView chargeCarType;          //收费信息车类型
    TextView chargeParkTime;         //收费信息停车时长
    TextView chargeMoney;            //收费信息收费金额
    Button ButtonEnterCharge;              //确认收费按钮
    Context context;

    //状态信息
    TextView textViewAllPlace;       //总车位
    TextView textViewEmptyPlace;     //空闲车位
    TextView textViewInCarCount;     //入场数量
    TextView textViewOutCarCount;    //出场数量
    //当班信息
    TextView textViewUserName;       //操作员
    TextView textViewLoginTime;      //登录长
    TextView textViewSumCar;         //当前班费车辆
    TextView textViewSumMoney;       //当班收费金额
    TextToSpeech mSpeech = null;
    @Bind(R.id.main_setting)
    Button mainSetting;

    private AlertDialog dialog;
    private byte[] outPortPicBuffer;
    private byte[] inPortPicBuffer;

    private delayTask delayServer;  //显示屏延时服务

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playTTS("初始化连接");
        setContentView(R.layout.activity_main);

        playTTS("正在初始化系统");
        new initLogin().execute();
        boolean isClear = MyApplication.settingInfo.getBoolean(AppConstants.IS_CLEAR_OLD_DATA);
        if (isClear)
            new CheckInfo().execute();
        context = MainActivity.this;
        if (MyApplication.settingInfo == null) {
            MyApplication.settingInfo = new SPUtils(MainActivity.this, "config");
        }
        //注册线程通讯
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        plateTextIn = (TextView) findViewById(R.id.textView_PlateIn);
        plateTextOut = (TextView) findViewById(R.id.textView_PlateOut);
        plateImageIn = (ImageView) findViewById(R.id.imageView_PicPlateIn);
        plateImageOut = (ImageView) findViewById(R.id.imageView_PicPlateOut);
        videoStreamIn = (ImageView) findViewById(R.id.imageView_videoPlateIn);
        videoStreamOut = (ImageView) findViewById(R.id.imageView_videoPlateOut);
        chargeCarNumber = (TextView) findViewById(R.id.chargeCarNumber);
        chargeCarType = (TextView) findViewById(R.id.chargeCarType);
        chargeParkTime = (TextView) findViewById(R.id.chargeParkTime);
        chargeMoney = (TextView) findViewById(R.id.chargeMoney);

        buttonManualPassIn = (Button) findViewById(R.id.button_manual_Pass_In);
        buttonManualPassIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new manualPassInFunc().execute();
            }
        });
        buttonAgainIdentIn = (Button) findViewById(R.id.button_againIdent_In);
        buttonAgainIdentIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                againIdentInFunc();
            }
        });
        buttonManualInOpen = (Button) findViewById(R.id.button_manual_Open_In);
        buttonManualInOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new manualInOpenFunc(plateTextIn.getText().toString()).execute();
            }
        });

        buttonAgainIdentOut = (Button) findViewById(R.id.button_againIdent_Out);
        buttonAgainIdentOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                againIdentOutFunc();
            }
        });
        ButtonManualOutOpen = (Button) findViewById(R.id.button_manual_Open_Out);
        ButtonManualOutOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new manualOutOpenFunc(chargeCarNumber.getText().toString(), chargeParkTime.getText().toString()).execute();
            }
        });
        ButtonEnterCharge = (Button) findViewById(R.id.enterCharge);
        ButtonEnterCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new enterChangeFunc(chargeParkTime.getText().toString(), outPortLog).execute();
            }
        });
        //状态信息
        textViewAllPlace = (TextView) findViewById(R.id.textViewAllPlace);       //总车位
        textViewEmptyPlace = (TextView) findViewById(R.id.textViewEmptyPlace);     //空闲车位
        textViewInCarCount = (TextView) findViewById(R.id.textViewInCarCount);     //入场数量
        textViewOutCarCount = (TextView) findViewById(R.id.textViewOutCarCount);    //出场数量
        //当班信息
        textViewUserName = (TextView) findViewById(R.id.textViewUserName);       //操作员
        textViewLoginTime = (TextView) findViewById(R.id.textViewloginTime);      //登录长
        textViewSumCar = (TextView) findViewById(R.id.textViewSumCar);         //当前班费车辆
        textViewSumMoney = (TextView) findViewById(R.id.textViewSumMoney);       //当班收费金额

        outPortLog.setReceivable(0.0);
        outPortLog.setCar_no("");
        outPortLog.setCar_type("");
        outPortLog.setStall_time(-3);
        //起动传输服务
        startmyserver();
        Intent delayThread = new Intent(MainActivity.this, delayTask.class);
        bindService(delayThread, conn, Service.BIND_AUTO_CREATE);
        //起动定时刷新显示任务
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message msg = new Message();
                    msg.what = 2;
                    myHandler.sendMessage(msg);
                }
            }
        }, 3000);
        playTTS("欢迎使用智能停车场系统");
        //显示登陆
        showLogin();

        initPrint();

    }

    private void initPrint() {
        connection();
        getUsbDeviceList();
        buffer=new StringBuffer();
        for(int c=0;c<usblist.size();c++){
            buffer.append(usblist.get(c));
        }
        showLog("buffer="+buffer.toString());

    }

    private boolean playTTS(String str) {
        if (mSpeech == null) {
            mSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    showLog("TTS 初始化");
                }
            });
            return false;
        }
        mSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null);

        return true;
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            delayServer = ((delayTask.ServicesBinder) service).getService();
            delayServer.initCamera(inCamera, outCamera, 10);
            new upStatusInfoDisp().execute();
        }
    };

    /**
     * 启动我的服务
     */
    public void startmyserver() {


        Intent i = new Intent(MainActivity.this, DelFileServer.class);
        startService(i);

        Intent intent = new Intent(MainActivity.this, SendService.class);
        startService(intent);
        Intent intentDon = new Intent(MainActivity.this, DownLoadServer.class);
        startService(intentDon);
    }

    class initLogin extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            makeUser();
            addMoneyBaseData();
            if (MyApplication.settingInfo == null) {
                MyApplication.settingInfo = new SPUtils(MainActivity.this, "config");
            }
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Thread() {
            @Override
            public void run() {
                super.run();
                addMoneyBaseData();
            }
        }.start();

    }

    // 生成收费表
    private void addMoneyBaseData() {
        MoneyTable m;
        List<MoneyTable> all = null;
        try {
            all = db.findAll(MoneyTable.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
        if (all == null || all.size() < 1) {
            for (int i = 0; i < 48; i++) {
                m = new MoneyTable();
                m.setFee_code(String.valueOf(i + 1));
                m.setFee_detail_code(null);
                m.setMoney(i / 2 + 1);
                m.setFee_name("临时车");
                m.setCar_type_name("临时车");
                m.setParked_min_time(i * 30);
                m.setParked_max_time((i + 1) * 30);
                try {
                    db.save(m);
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 生成管理员基本帐号
    private void makeUser() {
        try {
            List<UserTable> all = db.findAll(UserTable.class);
            if (all == null || all.size() < 1) {
                UserTable user = new UserTable();
                user.setUserName("管理员");
                user.setPassword("123456p");
                user.setType("system");
                db.save(user);
                user.setUserName("操作员");
                user.setPassword("1");
                user.setType("common");
                db.save(user);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    class findUserList extends AsyncTask<Void, Void, Integer> {
        MyPullText mUser;
        ArrayList<String> userName;

        public findUserList(MyPullText mUser) {
            this.mUser = mUser;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                userName = new ArrayList<>();
                List<UserTable> all = db.selector(UserTable.class).orderBy("id", true).findAll();
                if (all != null) {
                    for (int i = 0; i < all.size(); i++) {
                        userName.add(all.get(i).getUserName());
                    }
                }

            } catch (DbException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mUser.setPopList(userName);
            Log.e("ende", "userName.size==" + userName.size());
            if (userName.size() > 0) {
                mUser.setText(userName.get(0));
            }
        }
    }

    private void showLogin() {
        View view = LayoutInflater.from(this).inflate(R.layout.login_diglog, null);
        dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view, 0, 0, 0, 0);
        dialog.setCancelable(false);
        dialog.show();
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = 500;
        params.height = 400;
        dialog.getWindow().setAttributes(params);
        final MyPullText mUser = (MyPullText) view.findViewById(R.id.login_user);
        final EditText mPasswordView = (EditText) view.findViewById(R.id.login_password);
        Button login = (Button) view.findViewById(R.id.login_sign_in_button);
        new findUserList(mUser).execute();
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = mUser.getText().toString();
                String password = mPasswordView.getText().toString();
                if (TextUtils.isEmpty(user)) {
                    t.showShort(MainActivity.this, "请选择用户名");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    t.showShort(MainActivity.this, "密码不能为空");
                    return;
                }
                new match(user, password).execute();
            }
        });
    }

    class match extends AsyncTask<Void, Void, String> {
        private String userName;
        private String password;

        public match(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                List<UserTable> all = db.selector(UserTable.class).where("userName", "=", userName).and("password", "=", password).findAll();
                if (all.size() > 0) {
                    String type = all.get(0).getType();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    String dateStr = dateFormat.format(new Date());
                    try {
                        Date nowStartDate = dateFormat.parse(dateStr);
                        long count = db.selector(TrafficInfoTable.class).where("in_time", ">=", nowStartDate).count();
                        MyApplication.settingInfo.putLong("inCarCount", count);
                        count = db.selector(TrafficInfoTable.class).where("out_time", ">=", nowStartDate).count();
                        MyApplication.settingInfo.putLong("inCarCount", count);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    loginUserName = userName;
                    //上次非本用户或用户退出,则清空数据
                    userName = MyApplication.settingInfo.getString("userName");
                    MyApplication.settingInfo.putString("userName", userName);
                    if (!MyApplication.settingInfo.getBoolean("loginStatus") || !userName.equals(loginUserName)) {
                        MyApplication.settingInfo.putString("userName", loginUserName);
                        MyApplication.settingInfo.putBoolean("loginStatus", true);
                        MyApplication.settingInfo.putLong("inCarCount", 0);
                        MyApplication.settingInfo.putLong("outCarCount", 0);
                        MyApplication.settingInfo.putLong("chargeCarNumer", 0);
                        MyApplication.settingInfo.putString("chargeMoney", "0.00");
                        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                        Date nowDate = new Date();
                        String nowDateStr = format.format(nowDate);
                        MyApplication.settingInfo.putString("loginTime", nowDateStr);
                    }
                    return type;
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String type) {
            {
                //进入主页面
                if (type.isEmpty()) {
                    t.showShort(context, "用户名或密码错误");
                    return;
                } else if (type.equals("system")) {
                    mainSetting.setVisibility(View.VISIBLE);
                    new upStatusInfoDisp().execute();
                } else {
                    mainSetting.setVisibility(View.GONE);
                    new upStatusInfoDisp().execute();
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }
    }

    //更新状态信息
    class upStatusInfoDisp extends AsyncTask<Void, Void, Long> {
        String[] str = new String[10];

        protected Long doInBackground(Void... params) {
            showLog("刷新车位显示数据");
            long emptyCount;    //空闲车位
            //设定总车位
            long value = MyApplication.settingInfo.getLong("allCarPlace");
            str[0] = String.format("总车位：%d个", value);
            emptyCount = value;
            //设定空闲车位
            try {
                emptyCount = value - db.selector(TrafficInfoTable.class).where("status", "=", "已入").count();
            } catch (DbException e) {
                e.printStackTrace();
            }
            if (emptyCount < 0) {
                emptyCount = 0;
            }
            str[1] = String.format("空闲车位：%d个", emptyCount);
            value = MyApplication.settingInfo.getLong("inCarCount");
            str[2] = String.format("当班入场：%d车次", value);
            value = MyApplication.settingInfo.getLong("outCarCount");
            str[3] = String.format("当班出场：%d车次", value);
            str[4] = "操作员：" + MyApplication.settingInfo.getString("userName");
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            try {
                if (MyApplication.settingInfo.getString("loginTime") != null) {
                    String loginTimeStr = MyApplication.settingInfo.getString("loginTime");
                    Date loginTime = format.parse(loginTimeStr);
                    Date nowDate = new Date();
                    long loginTimeMinute = (nowDate.getTime() - loginTime.getTime()) / 60 / 1000;
                    long day = loginTimeMinute / (24 * 60);
                    long hour = (loginTimeMinute % (24 * 60)) / 60;
                    long minute = loginTimeMinute % 60;
                    str[5] = String.format("登陆：%d天%d小时%d分钟", day, hour, minute);
                } else {
                    str[5] = String.format("登陆：%d天%d小时%d分钟", 0, 0, 0);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
            long chargeNum = MyApplication.settingInfo.getLong("chargeCarNumer");
            str[6] = String.format("收费车辆：%d辆", chargeNum);
            str[7] = String.format("收费金额：" + MyApplication.settingInfo.getString("chargeMoney") + "元");
            String dispIP = MyApplication.settingInfo.getString(AppConstants.DISPLAY_IP);
            LedModule.udpLedDispaly(dispIP, 5005, str[1]);
            Log.i("log", "刷新车位显示UI");
            return emptyCount;
        }

        @Override
        protected void onPostExecute(Long emypyCount) {
            textViewAllPlace.setText(str[0]);
            textViewEmptyPlace.setText(str[1]);
            textViewInCarCount.setText(str[2]);
            textViewOutCarCount.setText(str[3]);
            textViewUserName.setText(str[4]);
            textViewLoginTime.setText(str[5]);
            textViewSumCar.setText(str[6]);
            textViewSumMoney.setText(str[7]);
            if (delayServer != null) {
                delayServer.display("in", "空位:" + emypyCount, "欢迎光临", "\\DH时\\DM分", "车牌识别 一车一杆 减速慢行", 15);//显示
                delayServer.display("out", "空位:" + emypyCount, "欢迎光临", "\\DH时\\DM分", "车牌识别 一车一杆 减速慢行", 15);//显示
            }
        }
    }

    //确认收费
    class enterChangeFunc extends AsyncTask<Void, Void, Integer> {
        TrafficInfoTable inLog;
        String parkTime;

        public enterChangeFunc(String parkTime, TrafficInfoTable inLog) {
            this.parkTime = parkTime;
            this.inLog = inLog;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (parkTime.indexOf("无入场记录") > 0 || parkTime.length() < 1) {
                return 1;
            }
            //如开启0元收费自动放行，则点本按钮无效
            if (inLog.getReceivable() == 0) {
                boolean tempCarFree = MyApplication.settingInfo.getBoolean("tempCarFree");
                if (!tempCarFree) {
                    return 2;
                }
            }
            if (carProcess.saveOutTempCar(inLog.getCar_no(), outPortPicBuffer, inLog.getReceivable(), inLog.getReceivable(), inLog.getStall_time())) {
                outCamera.playAudio(camera.AudioList.get("一路顺风"));
                outCamera.ledDisplay(2, "一路平安，请出场");
            }

            if (inLog.getReceivable() > 0) {
                // 打印
                inLog.setOut_user(MyApplication.settingInfo.getString("userName"));
                print();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case 0:
                    t.showShort(context, "收费完成");
                    playTTS("收费完成");
                    break;
                case 1:
                    t.showShort(context, "无可收费车辆");
                    playTTS("无可收费车辆");
                    break;
                case 2:
                    t.showShort(context, "该车无需收费，已放行！");
                    playTTS("该车无需收费，已放行！");
                    break;
                default:
                    break;
            }
            //更新出口收费信息
            chargeCarNumber.setText("");
            chargeCarType.setText("");
            chargeParkTime.setText("");
            chargeMoney.setText("待通行");
            new upStatusInfoDisp().execute();
        }
    }

    private void print() {

        boolean isPrint = MyApplication.settingInfo.getBoolean("isPrintCard");
        if (isPrint) {
            Gson gson = new Gson();
            PrintBean printBean = new PrintBean();
            printBean.carNumber = outPortLog.getCar_no();
            printBean.inTime = DateUtils.date2StringDetail(outPortLog.getIn_time());
            if (outPortLog.getReceivable() == null){
                printBean.money = 0.00;
            }else {
                printBean.money = outPortLog.getReceivable();
                printBean.outTime = DateUtils.date2StringDetail(outPortLog.getOut_time());
                long timeLong = outPortLog.getStall_time();
                printBean.parkTime = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                printBean.type = outPortLog.getCar_type();
                String json = gson.toJson(printBean);
                PrintUtils.printParkfee(this,MyApplication.mGpService,json, outPortLog.getOut_user(), MyApplication.settingInfo.getString("companyName"));

            }


//            PrintUtils.print(this, json, outPortLog.getOut_user(), MyApplication.settingInfo.getString("companyName"));
        }

    }

    //无牌入场
    class manualPassInFunc extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            byte[] picBuffer = inCamera.CapturePic();
            if (picBuffer == null) {
                return -1;

            }
            try {
                carProcess.saveInNoPlateCar(picBuffer);
                return 0;
            } catch (DbException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case 0:
                    t.showShort(context, "已完成无牌入场!");
                    new upStatusInfoDisp().execute();
                    break;
                case -1:
                    t.showShort(context, "拍照失败，请重新操作");
                    break;
            }
        }
    }

    //重新识别入场
    private void againIdentInFunc() {
        t.showShort(context, "入口重新识别中......");
        inCamera.againIdent();
        inCamera.ledDisplay(2, "欢迎光临");
    }

    //重新识别出场
    private void againIdentOutFunc() {
        t.showShort(context, "出口重新识别中......");
        outCamera.againIdent();
        outCamera.ledDisplay(2, "欢迎光临");

    }

    //入口确认起杆
    class manualInOpenFunc extends AsyncTask<Void, Void, Integer> {
        String carNumber;

        public manualInOpenFunc(String carNumber) {
            this.carNumber = carNumber;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (waitEnterCarNumber.length() < 1) {
                return -1;
            }
            //byte[] picBuffer = inCamera.CapturePic();
            //if (picBuffer == null) {
            //    return -2;
            // } else {
            try {
                inCamera.playAudio(camera.AudioList.get("欢迎光临"));
                inCamera.ledDisplay(2, "欢迎光临 " + carNumber + " 请入场");
                carProcess.saveInTempCar(carNumber, inPortPicBuffer);
                return 0;
            } catch (DbException e) {
                e.printStackTrace();
            }
            //}
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case 0:
                    plateTextIn.setText("待通行");
                    playTTS(carNumber + "入场");
                    t.showShort(context, "已完成确认通行");
                    new upStatusInfoDisp().execute();
                    break;
                case -1:
                    t.showShort(context, "无待通行车辆");
                    playTTS("无待通行车辆");
                    break;
                case -2:
                    playTTS("拍照失败，请重新操作");
                    t.showShort(context, "拍照失败，请重新操作");
                    break;
            }
        }
    }

    //出口手免费通行
    class manualOutOpenFunc extends AsyncTask<Void, Void, Integer> {
        String carNumber;
        String ParkTime;

        public manualOutOpenFunc(String carNumber, String ParkTime) {
            this.carNumber = carNumber;
            this.ParkTime = ParkTime;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (ParkTime.indexOf("无入场记录") > 0 || carNumber.length() == 0) {
                //拍照
                byte[] picBuffer = outCamera.CapturePic();
                if (carNumber.length() == 0) {
                    carNumber = "无牌";
                }
                carProcess.saveOutFreeCar(carNumber, picBuffer);
                outCamera.playAudio(camera.AudioList.get("一路顺风"));
                outCamera.ledDisplay(2, carNumber + "一路平安,请出场");
            } else {
                //outPortLog.setReceivable(0.0);
                //outPortLog.setCar_type("免费车");
                carProcess.saveOutTempCar(carNumber, outPortPicBuffer, outPortLog.getReceivable(), 0.0, outPortLog.getStall_time());
                outCamera.playAudio(camera.AudioList.get("一路顺风"));
                outCamera.ledDisplay(2, carNumber + "一路平安,请出场");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            //更新出口收费信息
            chargeCarNumber.setText("");
            chargeCarType.setText("");
            chargeParkTime.setText("");
            chargeMoney.setText("待通行");
            playTTS("免费出厂");
            new upStatusInfoDisp().execute();
        }
    }

    @Override
    public void onBackPressed() {
        playTTS("该程序禁止退出");
        t.showShort(this, "该程序禁止退出!");
    }

    //处理车牌识别事件
    class processPlateEvent extends AsyncTask<Void, Void, Integer> {
        public camera.PlateInfo info;
        public Bitmap bmp;

        public processPlateEvent(camera.PlateInfo info, Bitmap bmp) {
            this.info = info;
            this.bmp = bmp;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            //查询最近通行记录，如果通行时间小于设定时间则禁止通行，防止重复识别
            try {
                TrafficInfoTable log = db.selector(TrafficInfoTable.class).where("car_no", "=", info.getPlateNumber()).orderBy("update_time", true).findFirst();
                if (log != null) {
                    long delay = new Date().getTime() - log.getUpdateTime().getTime();
                    if (delay < MyApplication.settingInfo.getInt("enterDelay") * 60 * 1000) {
                        if (delay > 5 * 1000) {
                            return -1;
                        } else {
                            return -2;
                        }
                    }
                }
                if (info.getName().equals("in")) {
                    //入口处理
                    carProcess.processCarInFunc(info.getPlateNumber(), info.getCarPicdata());
                    return 1;
                } else if (info.getName().equals("out")) {
                    //出口处理
                    if (carProcess.processCarOutFunc(info.getPlateNumber(), info.getCarPicdata(), 5000)) {
                        return 2;
                    }
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case -1:
                    t.showShort(context, "该车出频繁，请稍后通行");
                    //显示
                    if (info.getName().equals("in")) {
                        //入口处理
                        playTTS("入口 同车号多次识别");
                        inCamera.ledDisplay(info.getPlateNumber(), "多次识别", "请尽快通行", "如返场请等候");
                        inCamera.ledDisplay(1, info.getPlateNumber() + " 多次识别 请尽快通行 如返场请等候");
                    } else if (info.getName().equals("out")) {
                        //出口处理
                        playTTS("出口 同车号多次识别");
                        outCamera.ledDisplay(info.getPlateNumber(), "多次识别", "请尽快通行", "如入场时间较短请等候");
                        outCamera.ledDisplay(1, info.getPlateNumber() + " 多次识别" + " 请尽快通行" + " 如入场时间较短请等候");
                    }
                    new upStatusInfoDisp().execute();
                    break;
                case -2:
                    t.showShort(context, "系统时间错误");
                    break;
                case 0:
                    playTTS(info.getPlateNumber() + "出场");
                    break;
                case 1:
                    playTTS(info.getPlateNumber() + "进场");
                    new upStatusInfoDisp().execute();
                    break;
                case 2:
                    //更新出口收费信息
                    chargeCarNumber.setText(outPortLog.getCar_no());
                    chargeCarType.setText(outPortLog.getCar_type());
                    //停车时长
                    long timeLong = outPortLog.getStall_time();
                    if (timeLong == -1) {
                        chargeParkTime.setText("无入场记录");
                        playTTS("入口 同车号多次识别");
                    } else if (timeLong == -2) {
                        chargeParkTime.setText("系统时间错误");
                        playTTS("系统时间错误");
                    } else if (timeLong == -3) {
                        chargeParkTime.setText("待通行");
                    } else {
                        String stall_time = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                        chargeParkTime.setText("停车：" + stall_time);
                    }
                    //收费
                    chargeMoney.setText(String.format("收费：%.1f元", outPortLog.getReceivable()));

                    outCamera.ledDisplay(chargeCarNumber.getText().toString(),
                            chargeCarType.getText().toString(),
                            chargeParkTime.getText().toString(),
                            chargeMoney.getText().toString());
                    if (outPortLog.getReceivable() > 0) {
                        outCamera.ledDisplay(2, chargeCarNumber.getText().toString() + " " + chargeParkTime.getText().toString() + " " + chargeMoney.getText().toString());
                        playTTS(info.getPlateNumber() + chargeParkTime.getText() + chargeMoney.getText());
                    } else {
                        outCamera.ledDisplay(2, chargeCarNumber.getText().toString() + " " + chargeParkTime.getText().toString());
                        playTTS(info.getPlateNumber() + chargeParkTime.getText());
                    }
                    new upStatusInfoDisp().execute();
                    break;
            }
        }
    }

    public Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 2) {
                new upStatusInfoDisp().execute();
                return;
            }
            camera.PlateInfo info = (camera.PlateInfo) msg.obj;
            Bitmap bmp = BitmapFactory.decodeByteArray(info.getCarPicdata(), 0, info.getCarPicdata().length);
            switch (info.msgType) {
                case PLATE:
                    showLog("event:" + info.msgType + info.getPlateNumber());
                    //设置显示入口车号和图片
                    if (info.getName().equals("in")) {
                        plateTextIn.setText(info.getPlateNumber());
                        if (info.getPlateColor().equals("黄色")) {
                            plateTextIn.setBackgroundColor(Color.YELLOW);
                        } else {
                            plateTextIn.setBackgroundColor(Color.BLUE);
                        }
                        plateImageIn.setImageBitmap(bmp);
                        plateImageIn.invalidate();
                        //缓存入口图处片
                        inPortPicBuffer = info.getCarPicdata();
                    }
                    //设置显示出口车号和图片
                    if (info.getName().equals("out")) {
                        plateTextOut.setText(info.getPlateNumber());
                        if (info.getPlateColor().equals("黄色")) {
                            plateTextOut.setBackgroundColor(Color.YELLOW);
                        } else {
                            plateTextOut.setBackgroundColor(Color.BLUE);
                        }
                        plateImageOut.setImageBitmap(bmp);
                        plateImageOut.invalidate();
                        //缓存出口图片
                        outPortPicBuffer = info.getCarPicdata();
                    }
                    new processPlateEvent(info, bmp).execute();
                    break;
                case PIC:
                    showLog(info.getPlateNumber());
                    //手动起杆捕捉图片
                    if (info.getName().equals("in")) {
                        plateTextIn.setText(info.getPlateNumber());
                        plateTextIn.setBackgroundColor(Color.BLUE);
                        plateImageIn.setImageBitmap(bmp);
                        plateImageIn.invalidate();
                    }
                    //设置显示出口车号和图片
                    if (info.getName().equals("out")) {
                        plateTextOut.setText(info.getPlateNumber());
                        plateTextIn.setBackgroundColor(Color.BLUE);
                        plateImageOut.setImageBitmap(bmp);
                        plateImageOut.invalidate();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void helloEventBus(camera.PlateInfo info) {
        //解码车辆抓拍图片
        Bitmap bmp = BitmapFactory.decodeByteArray(info.getCarPicdata(), 0, info.getCarPicdata().length);
        switch (info.msgType) {
            case STREAM:
                //设置显示入口视频
                if (info.getName().equals("in")) {
                    videoStreamIn.setImageBitmap(bmp);
                    videoStreamIn.invalidate();
                }
                //设置显示出口视频
                if (info.getName().equals("out")) {
                    videoStreamOut.setImageBitmap(bmp);
                    videoStreamOut.invalidate();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @OnClick({R.id.button_manual_Pass_Out,
            R.id.main_setting,
            R.id.main_search,
            R.id.main_change,
            R.id.enterCharge})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_manual_Pass_Out://选车出场
                Intent intent = new Intent(this, SelectPassOut.class);
                startActivityForResult(intent, 101);
                outCamera.againIdent();
                outCamera.ledDisplay(2, "欢迎光临");
                break;
            case R.id.main_setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.main_search:
                startActivity(new Intent(this, SrarchActivity.class));
                break;
            case R.id.main_change:
                ask();
                break;
        }
    }

    private void ask() {
        View view = LayoutInflater.from(this).inflate(R.layout.ask_diglog, null);
//        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(view, 0, 0, 0, 0);
        dialog.setCancelable(true);

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = 500;
        params.height = 400;
//        params.alpha = 0.5f;//dialog的透明度
//        params.dimAmount = 1.0f;//窗体颜色 0为不变色 1为黑色
        dialog.getWindow().setAttributes(params);
        Button cencle = (Button) view.findViewById(R.id.ask_cencle);
        Button ok = (Button) view.findViewById(R.id.ask_ok);

        dialog.show();

        cencle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //退出
                settingInfo.putBoolean("loginStatus", false);
                showLogin();
            }
        });
    }

    class syncProcessSelectOut extends AsyncTask<Void, Void, Integer> {
        private int id;

        public syncProcessSelectOut(int id) {
            this.id = id;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (id >= 0) {
                byte[] picBuffer = outCamera.CapturePic();
                carProcess.processManualSelectOut(id, picBuffer);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            //停车时长
            long timeLong = outPortLog.getStall_time();
            if (timeLong == -1) {
                chargeParkTime.setText("无入场记录");
                playTTS("该车无入场记录");
            } else if (timeLong == -2) {
                playTTS("系统时间错误");
                chargeParkTime.setText("系统时间错误");
            } else if (timeLong == -3) {
                chargeParkTime.setText("待通行");
            } else {
                //更新出口收费信息
                chargeCarNumber.setText(outPortLog.getCar_no());
                chargeCarType.setText(outPortLog.getCar_type());
                String stall_time = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                chargeParkTime.setText("停车：" + stall_time);
                chargeMoney.setText(String.format("收费：%.1f元", outPortLog.getReceivable()));
                outCamera.ledDisplay(chargeCarNumber.getText().toString(),
                        chargeCarType.getText().toString(),
                        chargeParkTime.getText().toString(),
                        chargeMoney.getText().toString());
                outCamera.ledDisplay(2, chargeCarNumber.getText().toString() + " " + chargeParkTime.getText().toString() + " " + chargeMoney.getText().toString());
                playTTS(chargeCarNumber.getText().toString() + chargeParkTime.getText() + chargeMoney.getText());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        showLog("requestCode:" + requestCode + "   resultCode:" + resultCode);
        switch (resultCode) {
            case 1:
                int id = data.getIntExtra("id", -1);
                new syncProcessSelectOut(id).execute();
                break;
            default:
                break;
        }
    }

    public void showLog(String msg) {
        Log.i("MainActivity", msg);
        mFileUtils.witefile(msg, DateUtils.date2String(new Date()));
    }

    class CheckInfo extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String s = DateUtils.date2String(new Date());
                Date date = DateUtils.string2Date(s);
                List<CarInfoTable> all = db.selector(CarInfoTable.class).where("stop_date", "<", date).findAll();
                if (all != null) {
                    for (int i = 0; i < all.size(); i++) {
                        int id = all.get(i).getId();
                        db.deleteById(CarInfoTable.class, id);
                        L.showlogError("过期数据:" + all.get(i).getCar_no());
                    }
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // ----------------------------------------------打印开始(不能动)------------------------------------------------
    private PrinterServiceConnection mConn = null;
    private StringBuffer buffer;
    private List<String> usblist=new ArrayList<String>();

    /**
     * 绑定服务连接枢纽
     */
    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("chenghao", "onServiceDisconnected() called");
            MyApplication.mGpService = null;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("chenghao", "onServiceConnected");
            MyApplication.mGpService = GpService.Stub.asInterface(service);

            try {
                MyApplication.mGpService.openPort(0, 2, buffer.toString(), 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 绑定服务
     */
    private void connection() {
        mConn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE); // bindService
        Log.i("chenghao", "调用connection");

    }

    /**
     * 获取当前的usb设备
     */
    public void getUsbDeviceList(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        if(count > 0)
        {
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                String devicename = device.getDeviceName();
                if(checkUsbDevicePidVid(device)){
                    usblist.add(devicename);
                }
            }
        }
        else
        {
            String noDevices ="没有usb设备";
            usblist.add(noDevices);
        }
    }
    boolean checkUsbDevicePidVid(UsbDevice dev) {
        int pid = dev.getProductId();
        int vid = dev.getVendorId();
        boolean rel = false;
        if ((vid == 34918 && pid == 256) || (vid == 1137 && pid == 85)
                || (vid == 6790 && pid == 30084)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 512)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 768)
                || (vid == 26728 && pid == 1024)|| (vid == 26728 && pid == 1280)
                || (vid == 26728 && pid == 1536)) {
            rel = true;
        }
        return rel;
    }



    // -----------------------------------------------打印结束-----------------------------------------------

}


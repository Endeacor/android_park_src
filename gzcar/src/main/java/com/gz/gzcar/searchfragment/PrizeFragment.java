package com.gz.gzcar.searchfragment;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.gz.gzcar.AppConstants;
import com.gz.gzcar.BaseFragment;
import com.gz.gzcar.Database.TrafficInfoTable;
import com.gz.gzcar.Database.UserTable;
import com.gz.gzcar.MyApplication;
import com.gz.gzcar.R;
import com.gz.gzcar.utils.CsvWriter;
import com.gz.gzcar.utils.DateUtils;
import com.gz.gzcar.utils.L;
import com.gz.gzcar.utils.PrintAllBean;
import com.gz.gzcar.utils.PrintUtils;
import com.gz.gzcar.weight.MyPullText;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Endeavor on 2016/8/8.
 * <p/>
 * 收费记录
 */
public class PrizeFragment extends BaseFragment {


    @Bind(R.id.et_money_carnumber)
    EditText mCarNumber;
    @Bind(R.id.et_money_starttime)
    TextView mStartTime;
    @Bind(R.id.et_money_endtime)
    TextView mEndTime;
    @Bind(R.id.btn_money_search)
    Button mSearch;
    @Bind(R.id.money_recyclerview)
    RecyclerView rcy;
    @Bind(R.id.tv_money)
    TextView mMoney;
    @Bind(R.id.tv_count)
    TextView mCount;
    @Bind(R.id.tv_receivable)
    TextView mReceivable;

    @Bind(R.id.seach_money_progerssbar)
    ProgressBar progerssbar;
    @Bind(R.id.search_money_spinner)
    MyPullText mSpinner;


    private DbManager db = x.getDb(MyApplication.daoConfig);
    private List<TrafficInfoTable> allData = new ArrayList<>();
    private List<TrafficInfoTable> sumAll;
    private MyAdapter myAdapter = new MyAdapter();
    private int pageIndex = 0;
    private int TAG = 3;
    private String searchNumber;
    private String searchStart;
    private String searchEnd;
    private String searchUser;

    //c7edcc
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search_money, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String end = DateUtils.getCurrentDataDetailStr();
        String start = DateUtils.date2StringDetail(new Date(new Date().getTime() - 24 * 60 * 60 * 1000));
        mStartTime.setText(start);
        mEndTime.setText(end);
    }

    @Override
    public void onResume() {
        super.onResume();

        new UserTask().execute();

        L.showlogError("收费记录 :  onResume()执行了....");

    }

    class UserTask extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> userList = new ArrayList<>();
            try {
                List<UserTable> all = db.selector(UserTable.class).orderBy("id", true).findAll();
                for (int i = 0; i < all.size(); i++) {
                    userList.add(all.get(i).getUserName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                t.showShort(getActivity(), "操作员初始化异常");
            }
            userList.add("全部");
            return userList;

        }

        @Override
        protected void onPostExecute(ArrayList<String> userList) {
            super.onPostExecute(userList);

            mSpinner.setPopList(userList);
            mSpinner.setTextSize(15);
            String userName = MyApplication.settingInfo.getString(AppConstants.USER_NAME, "");
            mSpinner.setText(userName);


            TAG = 3;
            pageIndex = 0;
            allData.clear();
            myAdapter.notifyDataSetChanged();
            searchUser = mSpinner.getText();
            initViews();
            initdata();
        }
    }

    private void initViews() {

        //时间选择器
        initDetailTime(getActivity(), mStartTime, mEndTime);

        final LinearLayoutManager lm = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        rcy.setLayoutManager(lm);

        rcy.setAdapter(myAdapter);
        rcy.setOnScrollListener(new RecyclerView.OnScrollListener() {

            int oldY = -1;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                L.showlogError("dy==" + dy);
                oldY = dy;

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int lastVisibleItemPosition = lm.findLastVisibleItemPosition();
                if (newState == 0 && lastVisibleItemPosition == (allData.size() - 1)) {
                    pageIndex += 1;
                    loadMore(pageIndex);

                }
            }
        });


    }

    private void initdata() {
        if (!TextUtils.isEmpty(searchNumber)) {
            TAG = 2;
        }
        searchStart = mStartTime.getText().toString().trim();
        searchEnd = mEndTime.getText().toString().trim();
        searchUser = mSpinner.getText();
        loadMore(pageIndex);
        new SumMoney().execute();

    }

    private void loadMore(int pageIndex) {
        L.showlogError("TAG===" + TAG);

        switch (TAG) {
            case 0:
                search0(pageIndex);// 时间查
                break;
            case 2:
                search2(pageIndex);// 车号&时间查询
                break;
            case 3:
                search3(pageIndex);// 时间&用户查
                break;
            case 4:
                search4(pageIndex);// 车牌&时间&用户查
                break;
        }

    }

    private void search4(int pageIndex) {
        L.showlogError("====车牌&时间&用户查===");
        try {
            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                    .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                    .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                    .and("car_no", "like", "%" + searchNumber + "%")
                    .and("receivable", ">", 0)
                    .and("status", "=", "已出")
                    .and("out_user", "=", searchUser)
                    .limit(15)
                    .offset(15 * pageIndex)
                    .orderBy("update_time", true)
                    .findAll();
            if (all != null && all.size() > 0) {
                allData.addAll(all);
                myAdapter.notifyDataSetChanged();
            } else {
                t.showShort(getActivity(), "没有更多数据了");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void search3(int pageIndex) {
        L.showlogError("====时间&用户查===");
        try {

            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                    .where("update_time", ">=", DateUtils.string2DateDetail(searchStart))
                    .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                    .and("receivable", ">", 0)
                    .and("status", "=", "已出")
                    .and("out_user", "=", searchUser)
                    .limit(15)
                    .offset(15 * pageIndex)
                    .orderBy("update_time", true)
                    .findAll();
            if (all != null && all.size() > 0) {
                allData.addAll(all);
                myAdapter.notifyDataSetChanged();
            } else {
                t.showShort(getActivity(), "没有更多数据了");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void search2(int pageIndex) {
        L.showlogError("====车号&时间查询===车号:" + searchNumber);
        try {

            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                    .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                    .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                    .and("car_no", "like", "%" + searchNumber + "%")
                    .and("receivable", ">", 0)
                    .and("status", "=", "已出")
                    .limit(15)
                    .offset(15 * pageIndex)
                    .orderBy("update_time", true)
                    .findAll();
            if (all != null && all.size() > 0) {
                allData.addAll(all);
                myAdapter.notifyDataSetChanged();
            } else {
                t.showShort(getActivity(), "没有更多数据了");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void search0(int pageIndex) {
        L.showlogError("====时间查询===");
        try {
            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                    .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                    .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                    .and("status", "=", "已出")
                    .and("receivable", ">", 0)
                    .limit(15)
                    .offset(15 * pageIndex)
                    .orderBy("update_time", true)
                    .findAll();
            if (all != null && all.size() > 0) {
                allData.addAll(all);
                myAdapter.notifyDataSetChanged();
            } else {
                t.showShort(getActivity(), "没有更多数据了");
            }

        } catch (DbException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @OnClick({R.id.et_money_starttime, R.id.et_money_endtime, R.id.btn_money_search, R.id.prize_print, R.id.search_money_export})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.et_money_starttime:
                startTimeShow();
                break;
            case R.id.et_money_endtime:
                endTimeShow();
                break;

            case R.id.btn_money_search:

                search();

                break;

            case R.id.search_money_export:
                progerssbar.setVisibility(View.VISIBLE);
                searchStart = mStartTime.getText().toString().trim();
                searchEnd = mEndTime.getText().toString().trim();
                new ExportTask().execute();
                break;

            case R.id.prize_print:
                requestPrint();
                break;
        }
    }

    private void requestPrint() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_request_print, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view, 0, 0, 0, 0);
        dialog.setCancelable(true);

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = 500;
        params.height = 400;
        dialog.getWindow().setAttributes(params);
        dialog.show();

        Button cancle = (Button) view.findViewById(R.id.requset_cencle);
        Button ok = (Button) view.findViewById(R.id.requset_ok);

        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                print();
            }
        });
    }

    private void print() {
        boolean isPrint = MyApplication.settingInfo.getBoolean("isPrintCard");
        L.showlogError("是否打印:" + isPrint);
        if (!isPrint) {
            t.showShort(getActivity(), "请到系统设置中打开打印权限");
            return;
        }
        if (sumAll == null || sumAll.size() < 1) {
            t.showShort(getActivity(), "暂无数据");
            return;
        }
        double toteMoney = 0;
        double reMoney = 0;
        long time = 0;
        for (int i = 0; i < sumAll.size(); i++) {

            double money = sumAll.get(i).getActual_money();
            toteMoney += money;
        }
        for (int i = 0; i < sumAll.size(); i++) {
            double re = sumAll.get(i).getReceivable();
            reMoney += re;
        }
        for (int i = 0; i < sumAll.size(); i++) {
            long stall_time = sumAll.get(i).getStall_time();
            time += stall_time;
        }

        String user = mSpinner.getText();
        String start = mStartTime.getText().toString().trim();
        String end = mEndTime.getText().toString().trim();
        PrintAllBean printAllBean = new PrintAllBean();
        printAllBean.operator = user;
        printAllBean.starttime = start;
        printAllBean.endtime = end;
        printAllBean.stoptime = String.format("%d时%d分", time / 60, time % 60);
        printAllBean.carallnum = String.valueOf(sumAll.size());
        printAllBean.receivable = String.valueOf(reMoney);
        printAllBean.RealPrice = String.valueOf(toteMoney);

        Gson gson = new Gson();
        String json = gson.toJson(printAllBean);
        PrintUtils.printonduty(getActivity(),MyApplication.mGpService,json);
//        PrintUtils.printAll(getActivity(), json);
    }

    class ExportTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            CsvWriter cw = null;

            try {
                L.showlogError("--- 开始查询数据库 ---");
                List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<=", dateFormatDetail.parse(searchEnd))
                        .and("receivable", ">", 0)
                        .orderBy("update_time", true)
                        .findAll();
                if (all != null && all.size() > 0) {
                    SimpleDateFormat format = new SimpleDateFormat("yy年MM月dd日HH时mm分");
                    String current = format.format(System.currentTimeMillis());
                    String fileName = "/收费记录表" + current + ".csv";
                    String usbDir = "/storage/uhost";
                    String usbDir1 = "/storage/uhost1";

                    L.showlogError("fileName===" + fileName);

                    String[] title = new String[]{"车号", "车辆类型", "入场时间", "出场时间",
                            "应收费用", "实收费用", "停车时长", "入场操作员", "出场操作员"};
                    try {
                        cw = new CsvWriter(usbDir1 + fileName, ',', Charset.forName("GBK"));
                        cw.writeRecord(title);
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            L.showlogError("--- 1号位未找到U盘,开始检索2号位 ---");
                            cw = new CsvWriter(usbDir + fileName, ',', Charset.forName("GBK"));
                            cw.writeRecord(title);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            L.showlogError("--- 2号位未找到U盘,停止... ---");
                            return -2;
                        }
                    }


                    TrafficInfoTable traffic;
                    for (int i = 0; i < all.size(); i++) {
                        traffic = all.get(i);
                        String car_type = traffic.getCar_type();
                        String car_no = traffic.getCar_no();
                        String in_time = DateUtils.date2StringDetail(traffic.getIn_time());
                        if (TextUtils.isEmpty(in_time))
                            in_time = "无入场记录";
                        String in_user = traffic.getIn_user();
                        String out_time = DateUtils.date2StringDetail(traffic.getOut_time());
                        if (TextUtils.isEmpty(out_time)) {
                            if (traffic.getStatus().equals("已出")) {
                                out_time = "异常出场";
                            } else {
                                out_time = "未出场";
                            }
                        }
                        String out_user = traffic.getOut_user();
                        Double receivable = traffic.getReceivable();
                        Double actual_money = traffic.getActual_money();
                        long timeLong = traffic.getStall_time();
                        String stall_time = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                        String[] carInfo = new String[]{car_no, car_type, in_time, out_time,
                                receivable + "元", actual_money + "元", stall_time, in_user, out_user};

                        cw.writeRecord(carInfo);
                        L.showlogError("数据写入成功 数据:id==" + traffic.getId());
                    }
                    Thread.sleep(1000);
                    return all.size();
                } else {
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            } finally {
                if (cw != null)
                    cw.close();
            }
        }


        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            int i = integer.intValue();
            if (i == -1) {
                t.showShort(getActivity(), "导出失败");
            } else if (i == -2) {
                t.showShort(getActivity(), "请先插入U盘");

            } else if (i == 0) {
                t.showShort(getActivity(), "当前时间段内无数据");
            } else {

                t.showShort(getActivity(), "导出完成,共" + integer.toString() + "条");
            }

            progerssbar.setVisibility(View.GONE);
        }
    }

    private void search() {
        searchNumber = mCarNumber.getText().toString().trim();
        searchStart = mStartTime.getText().toString().trim();
        searchEnd = mEndTime.getText().toString().trim();
        searchUser = mSpinner.getText();
        pageIndex = 0;
        allData.clear();
        myAdapter.notifyDataSetChanged();

        if ("全部".equals(searchUser)) {
            if (TextUtils.isEmpty(searchNumber)) {
                TAG = 0;// 时间查
                loadMore(pageIndex);
                new SumMoney().execute();

            } else {
                TAG = 2;// 车牌&时间查
                loadMore(pageIndex);
                new SumMoney().execute();

            }
        } else {
            if (TextUtils.isEmpty(searchNumber)) {
                TAG = 3;// 时间&用户查
                loadMore(pageIndex);
                new SumMoney().execute();

            } else {
                TAG = 4;// 车牌&时间&用户查
                loadMore(pageIndex);
                new SumMoney().execute();
            }
        }

    }

    class SumMoney extends AsyncTask<Void, Void, Void> {

        private double toteMoney = 0;
        private double reMoney = 0;

        @Override
        protected Void doInBackground(Void... params) {

            switch (TAG) {
                case 0:
                    sumSearch0();// 时间查
                    break;
                case 2:
                    sumSearch2();// 车号&时间查询
                    break;
                case 3:
                    sumSearch3();// 时间&用户查
                    break;
                case 4:
                    sumSearch4();// 车牌&时间&用户查
                    break;
            }

            if (sumAll == null) {
                return null;
            }

            for (int i = 0; i < sumAll.size(); i++) {

                double money = sumAll.get(i).getActual_money();
                toteMoney += money;
            }
            for (int i = 0; i < sumAll.size(); i++) {
                double re = sumAll.get(i).getReceivable();
                reMoney += re;
            }


            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (sumAll != null) {

                mCount.setText("车辆总数:" + sumAll.size() + " 辆");
                mReceivable.setText("应收总计:" + reMoney + " 元");
                mMoney.setText("实收总计:" + toteMoney + " 元");
            }

        }

        private void sumSearch0() {
            try {
                sumAll = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                        .and("status", "=", "已出")
                        .and("receivable", ">", 0)
                        .findAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sumSearch2() {
            L.showlogError("====车号&时间查询===车号:" + searchNumber);
            try {
                sumAll = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                        .and("car_no", "like", "%" + searchNumber + "%")
                        .and("receivable", ">", 0)
                        .and("status", "=", "已出")
                        .findAll();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sumSearch3() {
            L.showlogError("====时间&用户查===");
            try {
                sumAll = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                        .and("receivable", ">", 0)
                        .and("status", "=", "已出")
                        .and("out_user", "=", searchUser)
                        .findAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sumSearch4() {
            L.showlogError("====车牌&时间&用户查===");
            try {
                sumAll = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">=", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", new Date(dateFormatDetail.parse(searchEnd).getTime() + 60 * 1000))
                        .and("car_no", "like", "%" + searchNumber + "%")
                        .and("receivable", ">", 0)
                        .and("status", "=", "已出")
                        .and("out_user", "=", searchUser)
                        .findAll();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private class MyAdapter extends RecyclerView.Adapter<MyHolder> {

        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.item_search_free, parent, false);
            MyHolder myHolder = new MyHolder(itemView);
            return myHolder;
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {

            final TrafficInfoTable free = allData.get(position);
            holder.mId.setText(position + 1 + "");
            holder.mCarNum.setText(free.getCar_no());
            holder.mMoney.setText(free.getActual_money() + "");
            holder.mReceivable.setText(free.getReceivable() + "");
            long timeLong = free.getStall_time();
            if (timeLong == -1) {
                holder.mParkingtime.setText("无入场记录");
            } else if (timeLong == -2) {
                holder.mParkingtime.setText("系统时间错误");
            } else if (timeLong == -2) {
                holder.mParkingtime.setText("待通行");
            } else {
                String stall_time = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                holder.mParkingtime.setText(stall_time);
            }
            holder.mType.setText(free.getCar_type());
            Date inTime = free.getIn_time();
            if (inTime != null) {
                holder.mInTime.setText(dateFormatDetail.format(inTime));
            }
            if (free.getOut_time() != null) {

                holder.mOuttime.setText(dateFormatDetail.format(free.getOut_time()));
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), ImageDetailActivity.class);
                    intent.putExtra("in_image", free.getIn_image() + "");
                    intent.putExtra("out_image", free.getOut_image() + "");

                    intent.putExtra("carNumber", free.getCar_no() + "");

                    if (free.getCar_type() == null)
                        intent.putExtra("carType", "未知");
                    else
                        intent.putExtra("carType", free.getCar_type());

                    if (free.getStall() == null)
                        intent.putExtra("stall", "无");//占用车位
                    else
                        intent.putExtra("stall", free.getStall() + "");//占用车位
                    if (free.getReceivable() == null)
                        intent.putExtra("receivable", "未出场");//应收费用
                    else
                        intent.putExtra("receivable", free.getReceivable() + "");//应收费用
                    if (free.getActual_money() == null)
                        intent.putExtra("actual_money", "未出场");//实收费用
                    else
                        intent.putExtra("actual_money", free.getActual_money() + "");//实收费用
                    intent.putExtra("status", free.getStatus() + "");  //通行状态
                    if (free.getOut_user() == null)
                        intent.putExtra("out_user", "未知");
                    else
                        intent.putExtra("out_user", free.getOut_user() + "");
                    if (free.getIn_user() == null)
                        intent.putExtra("in_user", "未知");
                    else
                        intent.putExtra("in_user", free.getIn_user() + "");


                    intent.putExtra("in_time", DateUtils.date2StringDetail(free.getIn_time()));
                    if (free.getOut_time() == null)
                        intent.putExtra("out_time", "未出场");
                    else
                        intent.putExtra("out_time", DateUtils.date2StringDetail(free.getOut_time()));
                    //停车时长
                    long timeLong = free.getStall_time();
                    if (timeLong == -1) {
                        intent.putExtra("stall_time", "无入场记录");
                    } else if (timeLong == -2) {
                        intent.putExtra("stall_time", "系统时间错误");//停车时长
                    } else if (timeLong == -2) {
                        intent.putExtra("stall_time", "待通行");//停车时长
                    } else {
                        String stall_time = String.format("%d时%d分", timeLong / 60, timeLong % 60);
                        intent.putExtra("stall_time", stall_time);//停车时长
                    }
                    intent.putExtra("update_time", DateUtils.date2StringDetail(free.getUpdateTime()));
                    intent.putExtra("tag", 1);
                    startActivity(intent);
                }
            });

        }

        @Override
        public int getItemCount() {
            return allData.size();
        }

    }


    class MyHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.item_free_id)
        TextView mId;
        @Bind(R.id.item_free_num)
        TextView mCarNum;
        @Bind(R.id.item_free_type)
        TextView mType;
        @Bind(R.id.item_free_intime)
        TextView mInTime;
        @Bind(R.id.item_free_outtime)
        TextView mOuttime;
        @Bind(R.id.item_free_parkingtime)
        TextView mParkingtime;
        @Bind(R.id.item_free_money)
        TextView mMoney;
        @Bind(R.id.item_free_receivable)
        TextView mReceivable;

        public MyHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

}

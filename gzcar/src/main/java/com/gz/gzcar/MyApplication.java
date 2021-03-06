package com.gz.gzcar;

import android.app.Application;

import com.gprinter.aidl.GpService;
import com.gz.gzcar.server.CrashHandler;
import com.gz.gzcar.utils.GetImei;
import com.gz.gzcar.utils.InitUtils;
import com.gz.gzcar.utils.SPUtils;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.Bugly;

import org.xutils.DbManager;
import org.xutils.db.table.TableEntity;
import org.xutils.x;

/**
 * Created by Endeavor on 2016/8/18.
 */
public class MyApplication extends Application {
    private static MyApplication instance;

    public static GpService mGpService= null;



    public static int app_handler_in_out_record_download=1000*5;//通行记录
    public static int app_handler_down_tempfee=1000*5;//临时车收费
    public static int app_handler_down_info_stall=1000*5;//下传车位表
    public static int app_handler_down_info_vehicle=1000*5;//下传固定车信息表
    public static int app_handler_down_record_stall_vehicle=1000*5;//下传车位和车辆绑定表
    public static String devID ;
//    public static String Baseurl="http://221.204.11.69:3002/api/v1/";
    public static String mDBName = "tenement_passing_manager.db";
    public static DbManager.DaoConfig daoConfig;
//    public String carchLogId = "14ed7a7d64";
    public  DbManager db = null;
    public static SPUtils settingInfo;

    public static MyApplication getInstance() {
        if (instance == null) {
            instance = new MyApplication();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        devID = GetImei.getphoneimei(getApplicationContext());
        settingInfo = new SPUtils(this,"config");
        instance = this;
        //发布的时候一定把下面的解开
        CrashHandler.getInstance().init(getApplicationContext());
        Bugly.init(getApplicationContext(), "a3890485f9", false);
        InitUtils.init();
        x.Ext.init(this);
        daoConfig = new DbManager.DaoConfig()
                .setDbName(mDBName)
                .setDbVersion(1)
                .setAllowTransaction(true)
                .setTableCreateListener(new DbManager.TableCreateListener() {
                    @Override
                    public void onTableCreated(DbManager db, TableEntity<?> table) {
                        //Toast.makeText(getApplicationContext(), table.getName() + "创建了...", Toast.LENGTH_SHORT).show();

                    }
                })
                .setDbOpenListener(new DbManager.DbOpenListener() {
                    @Override
                    public void onDbOpened(DbManager db) {
                        // 开启WAL, 对写入加速提升巨大
                        db.getDatabase().enableWriteAheadLogging();
//                        Toast.makeText(getApplicationContext(), "数据库打开了...", Toast.LENGTH_SHORT).show();
                    }
                })
                .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                    @Override
                    public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                        //
//                        Toast.makeText(getApplicationContext(), "数据库升级了...", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

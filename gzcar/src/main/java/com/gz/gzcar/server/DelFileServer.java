package com.gz.gzcar.server;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 神保佑无 BUG
 * <p/>
 * <p/>程浩
 */
public class DelFileServer extends Service {

    //是否打印log
    private  boolean showlog=false;
    private FileUtils fileUtils;
    //sd卡的底线
    private long sdfilesize=500;
    //sd卡的路径
    private String path="/sdcard/capture";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fileUtils=new FileUtils();
        showlog("开启删除文件服务");
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(1);
                try {
                    Thread.sleep(1000*60*60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread=new Thread(runnable);
        thread.start();

    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(fileUtils.ifdelmyfile(sdfilesize)){
                fileUtils.delmyfile(path,sdfilesize);
            }
        }
    };

    private void showlog(String msg){
        if (showlog)
            Log.i("chenghao",msg);
    }
}

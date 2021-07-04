package com.example.daath.travelApp;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import com.my.greenDao.dao.DaoMaster;
import com.my.greenDao.dao.DaoSession;

import io.rong.imkit.RongIM;


public class App extends Application {

    private static App mInstance;
    private static DaoMaster daoMaster;
    private static DaoSession daoSession;
    private static String DB_NAME = "app.db";

    @Override
    public void onCreate() {
        super.onCreate();



        if (getApplicationInfo().packageName.equals(getCurProcessName(getApplicationContext())) ||
                "io.rong.push".equals(getCurProcessName(getApplicationContext()))) {


            RongIM.init(this);
        }

        if (mInstance == null) {
            mInstance = this;
        }

    }

    /**
     * 获得当前进程的名字
     *
     * @param context
     * @return 进程号
     */
    public static String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }


    public static DaoMaster getDaoMaster(Context context) {
        if (daoMaster == null) {
            DaoMaster.OpenHelper helper = new DaoMaster.DevOpenHelper(context, DB_NAME, null);
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
        return daoMaster;
    }

    public static DaoSession getDaoSession(Context context) {
        if (daoSession == null) {
            if (daoMaster == null) {
                daoMaster = getDaoMaster(context);
            }
            daoSession = daoMaster.newSession();
        }
        return daoSession;
    }
}

package kr.co.kworks.socket_server_test;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

import kr.co.kworks.socket_server_test.BuildConfig;

public class Logger {
    private static Logger instance;

    private Logger() {
    }


    public static Logger getInstance() {
        if(instance == null) instance = new Logger();
        return instance;
    }

    public void info(String tag, String str) {

        if (BuildConfig.IS_PRODUCTION) {
            // operation
        } else {
            // debug
            Log.i(tag, str);
        }
    }

    public void info(String str) {

        if (BuildConfig.IS_PRODUCTION) {
            // operation
            Utils.get().writeLog(str, "");
        } else {
            // debug
            Log.i("this", str);
        }
    }

    public void error(String str, Exception e) {
        if (BuildConfig.IS_PRODUCTION) {
            // operation
            StringWriter sw = new StringWriter();

            if(e != null) e.printStackTrace(new PrintWriter(sw));
            Utils.get().writeLog(str+": "+sw, "");
        } else {
            // debug
            Log.e("error", str, e);
        }
    }

    public void error(String str, Throwable e) {
        if (BuildConfig.IS_PRODUCTION) {
            // operation
            StringWriter sw = new StringWriter();

            if(e != null) e.printStackTrace(new PrintWriter(sw));
            Utils.get().writeLog(str+": "+sw, "");
        } else {
            // debug
            Log.e("error", str, e);
        }
    }
}

package kr.co.kworks.socket_server_test;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private static Utils me;

    public static final String PACKAGE_NAME = "kr.co.kworks.socket_server_test";

    // ID
    public static final int SCHEDULE_BROADCAST_ID = 9001;
    public static final int WALK_DETECT_FOREGROUND_NOTIFICATION_ID = 1001;

    // Action
    public static final String ACTION_REPORT = PACKAGE_NAME+".report";

    // Channel Name
    public static final String LOCATION_SERVICE_CHANNEL_ID = "LOCATION_SERVICE";

    public static final String LOG_FOLDER_NAME = "CAR";

    public static Utils get() {
        if (me == null) {
            me = new Utils();
        }
        return me;
    }

    /**
     * String 빈값 체크
     *
     * @param text
     * @return
     */
    public boolean isEmptyString(String text) {
        return (text == null || text.trim().equals("null") || text.trim().length() <= 0 || text.trim().equals("0"));
    }

    public String getSDPath(Context mContext) {
        String ext = Environment.getExternalStorageState();
        String SDPath = "";
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SDPath = mContext.getExternalFilesDir(null).getAbsolutePath();
            } else {
                SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
        } else {
            SDPath = Environment.MEDIA_UNMOUNTED;
        }
        return SDPath;
    }

    public String Decrypt(String text, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = new byte[16];
        byte[] b = key.getBytes("UTF-8");
        int len = b.length;
        if (len > keyBytes.length) len = keyBytes.length;
        System.arraycopy(b, 0, keyBytes, 0, len);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] results = cipher.doFinal(Base64.decode(text, 0));
        return new String(results, "UTF-8");
    }

    public String Encrypt(String text, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = new byte[16];
        byte[] b = key.getBytes("UTF-8");

        int len = b.length;

        if (len > keyBytes.length) len = keyBytes.length;

        System.arraycopy(b, 0, keyBytes, 0, len);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] results = cipher.doFinal(text.getBytes("UTF-8"));

        return Base64.encodeToString(results, 0);
    }

    public String getAbsolutePath(Context mContext) {
        String sDownloadDir = "";
        try {
            String SDPath = getSDPath(mContext);
            String absolutePath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                absolutePath = mContext.getExternalFilesDir(null).getAbsolutePath();
            } else {
                absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
            if ("unmounted".equals(SDPath)) {
                sDownloadDir = absolutePath + "2";
            } else {
                sDownloadDir = absolutePath;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sDownloadDir;
    }

    public ArrayList<String> getUpdateFile(String sUpdateFileName) {
        ArrayList<String> result = new ArrayList<>();
        Reader reader = null;
        BufferedReader fin = null;
        File file = new File(sUpdateFileName);
        try {
            if (file.exists()) {
                reader = new InputStreamReader(new FileInputStream(sUpdateFileName), "UTF-8");
                fin = new BufferedReader(reader);
                String ss;
                while ((ss = fin.readLine()) != null) {
                    if (ss.indexOf("=") > 0) {
                        continue;
                    }
                    result.add(ss);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean isServiceRunningCheck(final Context context, String serviceNm) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceNm.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isServiceRunningCheck(final Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean serviceStart(Context mContext, Class aClass) {
        boolean chk = false;
        try {
            if (!isServiceRunningCheck(mContext, aClass)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mContext.startForegroundService(new Intent(mContext, aClass));
                } else {
                    mContext.startService(new Intent(mContext, aClass));
                }
            }
            chk = true;
        } catch (Exception e) {
            Utils.get().writeLog("service Exception!! / " + e, "");
            e.printStackTrace();
        }
        return chk;
    }

    public boolean serviceStop(Context mContext, String service, Class aClass) {
        boolean chk = false;
        try {
            if (isServiceRunningCheck(mContext, aClass)) {
                mContext.stopService(new Intent(mContext, aClass));
                chk = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chk;
    }

    /**
     * 파일에 로그쌓기
     *
     * @param str
     */
    public void writeLog(String str, String sCode) {
        if (true) {
            String FileName;
            String sFolder;

            TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
            Locale currentLocale = new Locale("KOREAN", "KOREA");

            SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMdd", currentLocale);
            dFormat.setTimeZone(tz);

            Date m_strTime = new Date();
            String m_DateF1 = dFormat.format(m_strTime);

            dFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS", currentLocale);
            dFormat.setTimeZone(tz);

            String m_DateF = dFormat.format(m_strTime);

            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String dir = null;
            if (!isEmptyString(sCode)) {
                dir = File.separator + "Download" + File.separator + LOG_FOLDER_NAME + File.separator + sCode + File.separator;
            } else {
                dir = File.separator + "Download" + File.separator + LOG_FOLDER_NAME + File.separator;
            }
            String srcPath = basePath + dir;

            m_DateF = m_DateF + "->" + str;
            FileName = srcPath + LOG_FOLDER_NAME + "_" + m_DateF1 + "_1.txt";
            sFolder = srcPath;
            File desti = new File(sFolder);
            if (!desti.exists()) {
                Log.i("this", "desti - not Exist");
                desti.mkdirs();
            }
            FileOutputStream fos = null;
            PrintStream ps = null;
            try {
                fos = new FileOutputStream(FileName, true);
                ps = new PrintStream(fos);
                ps.println(m_DateF);
                System.out.println(m_DateF);
                Log.i("this", m_DateF);
            } catch (Exception ex) {
                Log.e("this", "writeLog", ex);
            } finally {
                try {
                    if(ps != null) ps.close();
                    if(fos != null) fos.close();
                } catch (IOException e) {
                    Log.e("this", "writeLog - IOException", e);
                }
            }
        }
    }

    public void writeLog(String str, int sCode) {
        if (true) {
            String FileName;
            String sFolder;

            TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
            Locale currentLocale = new Locale("KOREAN", "KOREA");

            SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMdd", currentLocale);
            dFormat.setTimeZone(tz);

            Date m_strTime = new Date();
            String m_DateF1 = dFormat.format(m_strTime);

            dFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS", currentLocale);
            dFormat.setTimeZone(tz);

            String m_DateF = dFormat.format(m_strTime);

            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String dir = File.separator + "Download" + File.separator + LOG_FOLDER_NAME + File.separator;
            String srcPath = basePath + dir;

            m_DateF = m_DateF + "->" + str;
            FileName = srcPath + LOG_FOLDER_NAME + "_" + m_DateF1 + "a" + sCode + ".txt";
            sFolder = srcPath;
            File desti = new File(sFolder);
            if (!desti.exists()) {
                desti.mkdirs();
            }
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(FileName, true));
                ps.println(m_DateF);
                System.out.println(m_DateF);
            } catch (Exception ex) {
//                ex.printStackTrace();
            }
        }
    }

    // add character to string
    public static String addChar(String str, char ch, int position) {
        return str.substring(0, position) + ch + str.substring(position);
    }

    public Point getScreenSize(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Point size = new Point();
        ((Activity)context).getWindowManager().getDefaultDisplay().getSize(size);

        Point realSize = getRealScreenSize((Activity) context);

        if(realSize.y - size.y < 300) {
            return size;
        }

        ((Activity)context).getWindowManager().getDefaultDisplay().getSize(size);

        return realSize;
    }

    public Point getRealScreenSize(Activity _activity) {
        Display display;
        Point display_size;
        WindowManager wm = (WindowManager) _activity.getSystemService(_activity.WINDOW_SERVICE);

        display = wm.getDefaultDisplay();
        display_size = new Point();
        display.getRealSize(display_size); // or getSize(size)

        return display_size;
    }



}

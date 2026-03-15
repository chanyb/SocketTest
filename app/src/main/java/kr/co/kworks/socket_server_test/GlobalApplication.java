package kr.co.kworks.socket_server_test;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


public class GlobalApplication extends Application implements ViewModelStoreOwner{
    private static volatile GlobalApplication instance;
    private static String TAG = "activity";
    Display display;
    Point display_size;
    InputMethodManager imm;
    private ViewModelStore viewModelStore;

    public static GlobalApplication getContext() {
        return instance;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    private enum State {
        None, Foreground, Background
    }

    private State state;
    private int running;

    public interface Listener {
        void onBecameForeground();

        void onBecameBackground();
    }

    private Listener stateListener;
    public static Activity currentActivity;

    private void getScreenSize(Activity _activity) {
        //1
        //display = _activity.getWindowManager().getDefaultDisplay();  // in

        //2
        WindowManager wm = (WindowManager) _activity.getSystemService(_activity.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        display_size = new Point();
        display.getRealSize(display_size); // or getSize(size)
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        instance = this;
        state = State.None;
        imm = (InputMethodManager) getSystemService(GlobalApplication.INPUT_METHOD_SERVICE);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        viewModelStore = new ViewModelStore();
    }

    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            ActivityLifecycleCallbacks.super.onActivityPreCreated(activity, savedInstanceState);
            GlobalApplication.currentActivity = activity;
            Logger.getInstance().info("GlobalApplication: set currentActivity");
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            getScreenSize(activity);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            Logger.getInstance().info("onActivityStarted: " + activity);
            GlobalApplication.currentActivity = activity;
            if (++running == 1 && !activity.isChangingConfigurations()) {
                state = State.Foreground;
                if (stateListener != null) stateListener.onBecameForeground();
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Logger.getInstance().info("onActivityResumed: " + activity);
            GlobalApplication.currentActivity = activity;
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            Logger.getInstance().info("onActivityPaused: " + activity);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            Logger.getInstance().info("onActivityStopped: " + activity);
            if (--running == 0 && !activity.isChangingConfigurations()) {
                state = State.Background;
                if (stateListener != null) stateListener.onBecameBackground();
            }
            if (GlobalApplication.currentActivity != null) {
                if (activity.getClass() == GlobalApplication.currentActivity.getClass()) {
                    GlobalApplication.currentActivity = null;
                    Logger.getInstance().error("GlobalApplication: currentAcc null", null);
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            Logger.getInstance().info("onActivitySaveInstanceState: " + activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Logger.getInstance().info("onActivityDestroyed: " + activity);
        }
    };

    public boolean isEqual(Activity a, Class b) {
        return (a.getClass().getName().equals(b.getName()));
    }

    public boolean isBackground() {
        return state == State.Background;
    }

    public boolean isForeground() {
        return state == State.Foreground;
    }

    public void addListener(Listener listener) {
        this.stateListener = listener;
    }

    public void removeListsner() {
        stateListener = null;
    }


    public boolean isServiceRunningCheck(Class serviceClass) {
        ActivityManager manager = (ActivityManager) GlobalApplication.getContext().getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void createNotificationChannel(String channelId, int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, importance); // id, name, importance
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 2000, 1000, 2000});
            channel.setSound(null, null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = GlobalApplication.getContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean isNotificationChannelEnabled(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = GlobalApplication.getContext().getSystemService(NotificationManager.class);
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);

            if (channel != null) {
                return true;
            }
            return false;
        }

        return true;
    }

    public void showSoftInput(EditText editText) {
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public void hideSoftInput(EditText editText) {
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public int getRealHeight() {
        return display_size.y;
    }

    public int getGuiHeight() {
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        int deviceHeightDp = config.screenHeightDp;

        return dpToPx(deviceHeightDp);
    }

    public int getGuiWidth() {
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        int deviceWidthDp = config.screenWidthDp;

        return dpToPx(deviceWidthDp);
    }

    private int dpToPx(float dp) {
        Resources resources = getContext().getResources();
        float density = resources.getDisplayMetrics().density;
        int px = (int) Math.ceil(dp * density);
        return px;
    }

    public void cancelAlarm(Context contexet, Class broadCastReceiverClass, int id) {
        AlarmManager alarmManager = (AlarmManager) contexet.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(contexet, broadCastReceiverClass);
        PendingIntent alarmPendingIntent = null;
        intent.setAction(Utils.ACTION_REPORT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmPendingIntent = PendingIntent.getBroadcast(contexet, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            alarmPendingIntent = PendingIntent.getBroadcast(contexet, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        alarmManager.cancel(alarmPendingIntent);
    }

    public String saveBitmapToGallery(Bitmap bitmap, String fileName, String fileDesc) {
        return MediaStore.Images.Media.insertImage(GlobalApplication.getContext().getContentResolver(), bitmap, fileName, fileDesc);
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        Bitmap bitmap = null;
        ExifInterface exif = null;
//        DeviceRotationManager.ROTATE_STATE rotateState = null;
//        int orientation = Utils.PREFERENCE_INT_DEFAULT;
        // exif 읽기
//        try {
//            exif = new ExifInterface(uri.getPath());
//            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//            switch (orientation) {
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    // exif 확인 결과 기본 사진은 90도 회전되어있지만, 내가 읽어온 비트맵은 회전되지 않은 상태이므로 90도 회전해주기 위해 아래와같이 설정.
//                    rotateState = DeviceRotationManager.ROTATE_STATE.LANDSCAPE_REVERSE;
//                    break;
//                case 180:
//                    rotateState = DeviceRotationManager.ROTATE_STATE.PORTRAIT;
//                    break;
//                case 270:
//                    rotateState = DeviceRotationManager.ROTATE_STATE.PORTRAIT_REVERSE;
//                    break;
//                case 0:
//                    rotateState = DeviceRotationManager.ROTATE_STATE.LANDSCAPE;
//                    break;
//            }
//
//        } catch (IOException e) {
//            rotateState = DeviceRotationManager.ROTATE_STATE.PORTRAIT;
//            Log.i("this", "exif 정보 없음");
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContext().getContentResolver(), uri));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
//                bitmap = rotateBitmap(rotateState, bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

//    private Bitmap rotateBitmap(DeviceRotationManager.ROTATE_STATE captureRotate, Bitmap bitmap) {
//        Matrix rotateMatrix = new Matrix();
//
//        if (captureRotate == DeviceRotationManager.ROTATE_STATE.LANDSCAPE) rotateMatrix.postRotate(270);
//        else if (captureRotate == DeviceRotationManager.ROTATE_STATE.LANDSCAPE_REVERSE) rotateMatrix.postRotate(90);
//        else if (captureRotate == DeviceRotationManager.ROTATE_STATE.PORTRAIT) rotateMatrix.postRotate(0);
//        else if (captureRotate == DeviceRotationManager.ROTATE_STATE.PORTRAIT_REVERSE) rotateMatrix.postRotate(180);
//
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, false);
//    }

    public void deletePhotoInGallery(Uri uri) {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, //the album it in
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.TITLE
        };

        Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
        if(cursor == null || cursor.getCount() == 0) return ;
        cursor.moveToFirst();

        int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA);
        String data = cursor.getString(dataIdx);
        File photo = new File(data);
        if (photo.exists()) {
            photo.delete();
        }
    }

    public void deletePhotoInGallery2(Uri uri) {
        ContentResolver contentResolver = getContext().getContentResolver();

        try {
            // 미디어 스토어에서 사진 삭제
            int deletedRows = contentResolver.delete(uri, null, null);

            if (deletedRows > 0) {
                Log.i("Delete", "Photo deleted from gallery.");
            } else {
                Log.i("Delete", "Failed to delete photo.");
            }
        } catch (SecurityException e) {
            Log.e("Delete", "SecurityException - Permission Denied.", e);
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void hideStatusBar(AppCompatActivity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public Bitmap getBitmapFromDrawable(Context context, int resource) {
        Drawable drawable = ContextCompat.getDrawable(context, resource);
        Bitmap bitmap;

        // Drawable이 BitmapDrawable인 경우
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // VectorDrawable 등 다른 Drawable일 경우, Bitmap으로 직접 그리기
            bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }

    public byte[] getByteArrayFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }

    public byte[] getByteArrayFromDrawable(Context context, int resource) {
        Bitmap bitmap = getBitmapFromDrawable(context, resource);
        return getByteArrayFromBitmap(bitmap);
    }
}

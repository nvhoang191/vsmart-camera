package com.example.camera_vsmart;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Base64;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AppUtils {
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private static ProgressDialog progressDialog;

    public static void showCircleProgressDialog(Context mContext) {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setCancelable(false);
        try {
            progressDialog.show();
        } catch (WindowManager.BadTokenException e) {

        }
    }

    public static void setProgressDialogMessage(String message){
        progressDialog.setMessage(message);
    }

    public static void dismissCircleProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            return encoded;
        }
        return "";
    }

    public static String trimRedundantSpace(String str){
        return str.trim().replaceAll(" +", " ");
    }

    public static int convertDptoPixel(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static String getSimpleDateFormatString(Date dateTime){
        return sSimpleDateFormat.format(dateTime);
    }

    public static String getFileDir(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory(), "JUKI");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                DebugLog.d("Cant create");
            }
        }
        return file.getAbsolutePath() + "/" + fileName;
    }
}

package com.example.camera_vsmart.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AppUtils {
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

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
        File file = new File(Environment.getExternalStorageDirectory(), "CAMERA-VSMART");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                DebugLog.d("Can't create file path");
            }
        }
        return file.getAbsolutePath() + "/" + fileName;
    }
}

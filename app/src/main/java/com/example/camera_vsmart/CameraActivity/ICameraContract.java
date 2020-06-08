package com.example.camera_vsmart.CameraActivity;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;

public interface ICameraContract {
    interface IViewListener {
        SurfaceTexture getCameraPreview();
        void onCaptureCompleted(String imgName); // called after capturing
        void onWritePicCompleted(String imgName);
        void onWritePicFailed();
        void onConfigCameraComplete(); // called after configuring camera
        void onCameraFailed();
    }

    interface IPresenterAction {
        void openCamera();
        void closeCamera();
        void startBackgroundThread();
        void stopBackgroundThread();
        void takePhoto();
        /**
         * Chỉ có thể bật các chế độ đèn flash sau khi cài đặt xong cấu hình camera
         */
        void setFlashOff();
        void setFlashOn();
        void setFlashAuto();
        void regionsFocus(Rect touchRect);
    }
}

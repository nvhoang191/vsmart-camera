package com.example.camera_vsmart;

public class AppConstants {
    public class CameraConfig{
        public static final int REQUEST_CAMERA_PERMISSION = 100;

        public static final int STATE_PREVIEW = 0;
        public static final int STATE_WAITING_LOCK = 1;
        public static final int STATE_WAITING_PRECAPTURE = 2;
        public static final int STATE_WAITING_NON_PRECAPTURE = 3;
        public static final int STATE_PICTURE_TAKEN = 4;

        public static final int MODE_FLASH_AUTO = 5;
        public static final int MODE_FLASH_OFF = 6;
        public static final int MODE_FLASH_ON = 7;
    }
}

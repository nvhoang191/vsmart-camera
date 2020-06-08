package com.example.camera_vsmart.CameraActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.example.camera_vsmart.Utils.AppConstants;
import com.example.camera_vsmart.Utils.AppUtils;
import com.example.camera_vsmart.Utils.DataManager;
import com.example.camera_vsmart.Utils.DebugLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import static android.support.v4.app.ActivityCompat.requestPermissions;

public class CameraScreenPresenter implements ICameraContract.IPresenterAction {
    private static final String DATE_FORMAT = "yyMMdd_kkmmss";
    private static final String NAME_HANDLER_THREAD = "CameraBackground";
    private static final int DELAY_WAIT_AF_LOCK = 1500;

    private ICameraContract.IViewListener mViewListener;
    private Context mContext;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Size mCameraSize;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureBuilder;

    private boolean mFlashSupported;
    private int mFlashMode;
    private int mState = AppConstants.CameraConfig.STATE_PREVIEW;

    private ImageReader mImageReader;
    private String mImgName;

    private ArrayList<Size> mImgSizes;

    private boolean mLockedFocus;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private boolean mMeteringAreaAFSupported;

    CameraScreenPresenter(Context context, ICameraContract.IViewListener listener) {
        this.mContext = context;
        this.mViewListener = listener;
    }

    public void openCamera() {
        if (!checkCameraPermission()) {
            return;
        }
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = getCameraInfo(mCameraManager.getCameraIdList());
            assert null != cameraId;
            mCameraManager.openCamera(cameraId, mStateCallback, null);
            DebugLog.d("open camera");
        } catch (CameraAccessException e) {
            e.printStackTrace();
            DebugLog.d(e.getMessage());
        }
    }

    private String getCameraInfo(String[] listCameraId) {
        try {
            DebugLog.d("get cam info");
            for (String cameraId : listCameraId) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (null == map) {
                    continue;
                }

                mImgSizes = new ArrayList<>(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
                Collections.sort(mImgSizes, new CompareSizesByArea());
                initImageReader();

                DebugLog.d(map.toString());
                mCameraSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class));
                DebugLog.d("Camera preview resolution: " + mCameraSize.getWidth() + ":" + mCameraSize.getHeight());

                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                Integer i = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                if (i != null) {
                    mMeteringAreaAFSupported = i >= 1;
                }

                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            DebugLog.d("map.getOutputSizes null");
        }
        return null;
    }

    private void initImageReader() {
        if (mImageReader != null) {
            mImageReader.close();
        }

        int index = mImgSizes.size() - 1;
        if (index < 0) return;
        Size mPictureSize = mImgSizes.get(index);
        mImgSizes.remove(mPictureSize);

        mImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(), ImageFormat.JPEG, 2);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (mImgName != null) return;
                Image image = reader.acquireLatestImage();

                Calendar calendar = Calendar.getInstance();
                DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                mImgName = "IMG_" + dateFormat.format(calendar.getTime()) + ".jpg";
                DebugLog.d("Created file " + mImgName);

                mHandler.post(new ImageSaver(image, createFile(mImgName), mViewListener));
                mViewListener.onWritePicCompleted(mImgName);
            }
        }, mHandler);
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NotNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
            DebugLog.d("Opened Camera");
        }

        @Override
        public void onDisconnected(@NotNull CameraDevice camera) {
            DebugLog.d("Disconnected Camera. Try opening camera again.");
            mViewListener.onCameraFailed();
        }

        @Override
        public void onError(@NotNull CameraDevice camera, int error) {
            mViewListener.onCameraFailed();
        }
    };

    @Override
    public void takePhoto() {
        try {
            DebugLog.d("Lock focus");
            // This is how to tell the camera to lock focus.
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = AppConstants.CameraConfig.STATE_WAITING_LOCK;
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack, mHandler);
            mLockedFocus = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startBackgroundThread() {
        mHandlerThread = new HandlerThread(NAME_HANDLER_THREAD);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        DebugLog.d("Start background thread");
    }

    @Override
    public void stopBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DebugLog.d("Stop background thread");
    }

    @Override
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            DebugLog.d("Closed camera");
        } catch (InterruptedException e) {
            DebugLog.d("Interrupted while trying to lock camera closing");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA}
                    , AppConstants.CameraConfig.REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = mViewListener.getCameraPreview();
            assert texture != null;
            texture.setDefaultBufferSize(mCameraSize.getWidth(), mCameraSize.getHeight());
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureBuilder.addTarget(surface);

            //Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface())
                    , new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession = cameraCaptureSession;
                            DebugLog.d("session created");
                            try {
                                // Auto focus should be continuous for camera preview.
                                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                mCaptureRequest = mCaptureBuilder.build();
                                mCameraCaptureSession.setRepeatingRequest(mCaptureRequest,
                                        mCaptureCallBack, mHandler);
                                mViewListener.onConfigCameraComplete();
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            DebugLog.d("ConfigureFailed");
                            initImageReader();
                            createCameraPreview();
                        }
                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Handler h = new Handler();
    private Runnable r = null;
    private CameraCaptureSession.CaptureCallback mCaptureCallBack
            = new CameraCaptureSession.CaptureCallback() {
        private void process(final CaptureResult result) {
            switch (mState) {
                case AppConstants.CameraConfig.STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case AppConstants.CameraConfig.STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    DebugLog.d("STATE_WAITING_LOCK, AF STATE:" + afState);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN == afState) {
                        if (r != null)
                            h.removeCallbacks(r);
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = AppConstants.CameraConfig.STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    } else if (CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState ||
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                        if (r == null) {
                            r = new Runnable() {

                                @Override
                                public void run() {
                                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                        mState = AppConstants.CameraConfig.STATE_PICTURE_TAKEN;
                                        captureStillPicture();
                                    }
                                    DebugLog.d("Capture image unlock AF");
                                }
                            };
                            h.postDelayed(r, DELAY_WAIT_AF_LOCK);
                        }
                    }
                    break;
                }
                case AppConstants.CameraConfig.STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = AppConstants.CameraConfig.STATE_WAITING_NON_PRECAPTURE;
                    }
                    DebugLog.d("STATE_WAITING_PRECAPTURE");
                    break;
                }
                case AppConstants.CameraConfig.STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = AppConstants.CameraConfig.STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    DebugLog.d("STATE_WAITING_NON_PRECAPTURE");
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private void captureStillPicture() {
        DebugLog.d("captureStillPicture");
        try {
            if (null == mContext || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setFlashModeWhileCapture(captureBuilder);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.abortCaptures();
            mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        if (!mLockedFocus) return;
        DebugLog.d("unlockFocus");
        try {
            // Reset the auto-focus trigger
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack,
                    mHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = AppConstants.CameraConfig.STATE_PREVIEW;
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallBack, mHandler);
            mViewListener.onCaptureCompleted(mImgName);
            mLockedFocus = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the pre-capture sequence to be set.
            mState = AppConstants.CameraConfig.STATE_WAITING_PRECAPTURE;
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack,
                    mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private File createFile(String fName) {
        return new File(AppUtils.getFileDir(fName));
    }

    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;
        private final ICameraContract.IViewListener mListener;

        ImageSaver(Image image, File file, ICameraContract.IViewListener listener) {
            mImage = image;
            mFile = file;
            mListener = listener;
            DebugLog.d(mFile.toString());
        }


        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
                mListener.onWritePicFailed();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            updateExifImage(mFile);
        }

        private void updateExifImage(File file) {
            try {
                Hashtable<String, String> hash = new Hashtable<>();
                Metadata metadata = ImageMetadataReader.readMetadata(file);

                try {
                    JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
                    hash.put(jpegDirectory.getTagName(JpegDirectory.TAG_IMAGE_WIDTH), jpegDirectory.getDescription(JpegDirectory.TAG_IMAGE_WIDTH));
                    hash.put(jpegDirectory.getTagName(JpegDirectory.TAG_IMAGE_HEIGHT), jpegDirectory.getDescription(JpegDirectory.TAG_IMAGE_HEIGHT));

                    ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                    hash.put(exifIFD0Directory.getTagName(ExifIFD0Directory.TAG_MODEL), exifIFD0Directory.getDescription(ExifIFD0Directory.TAG_MODEL));
                    hash.put(exifIFD0Directory.getTagName(ExifIFD0Directory.TAG_MAKE), exifIFD0Directory.getDescription(ExifIFD0Directory.TAG_MAKE));
                    hash.put(exifIFD0Directory.getTagName(ExifIFD0Directory.TAG_X_RESOLUTION), exifIFD0Directory.getDescription(ExifIFD0Directory.TAG_X_RESOLUTION));
                    hash.put(exifIFD0Directory.getTagName(ExifIFD0Directory.TAG_Y_RESOLUTION), exifIFD0Directory.getDescription(ExifIFD0Directory.TAG_Y_RESOLUTION));
                    hash.put(exifIFD0Directory.getTagName(ExifIFD0Directory.TAG_DATETIME), exifIFD0Directory.getDescription(ExifIFD0Directory.TAG_DATETIME));

                    ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    hash.put(exifSubIFDDirectory.getTagName(ExifSubIFDDirectory.TAG_FLASH), exifSubIFDDirectory.getDescription(ExifSubIFDDirectory.TAG_FLASH));

                    FileTypeDirectory fileTypeDirectory = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
                    hash.put(fileTypeDirectory.getTagName(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE), fileTypeDirectory.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE));

                    FileSystemDirectory fileSystemDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
                    hash.put(fileSystemDirectory.getTagName(FileSystemDirectory.TAG_FILE_NAME), fileSystemDirectory.getDescription(FileSystemDirectory.TAG_FILE_NAME));
                    hash.put(fileSystemDirectory.getTagName(FileSystemDirectory.TAG_FILE_SIZE), fileSystemDirectory.getDescription(FileSystemDirectory.TAG_FILE_SIZE));
                } catch (NullPointerException e) {
                    DebugLog.d(e.getMessage());
                }
                DataManager.getInstance().setExifImage(hash);
                DebugLog.d("Exif Image: " + DataManager.getInstance().getExifImage());


            } catch (IOException | ImageProcessingException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void setFlashOn() {
        if (!mFlashSupported) return;
        try {
            mFlashMode = AppConstants.CameraConfig.MODE_FLASH_ON;
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack, mHandler);
            DebugLog.d("Set flash ON");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFlashOff() {
        if (!mFlashSupported) return;
        try {
            mFlashMode = AppConstants.CameraConfig.MODE_FLASH_OFF;
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack, mHandler);
            DebugLog.d("Set flash OFF");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFlashAuto() {
        if (!mFlashSupported) return;
        try {
            mFlashMode = AppConstants.CameraConfig.MODE_FLASH_AUTO;
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack, mHandler);
            DebugLog.d("Set flash AUTO");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setFlashModeWhileCapture(CaptureRequest.Builder requestBuilder) {
        if (!mFlashSupported) {
            return;
        }
        switch (mFlashMode) {
            case AppConstants.CameraConfig.MODE_FLASH_AUTO: {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                break;
            }
            case AppConstants.CameraConfig.MODE_FLASH_ON: {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            }
            case AppConstants.CameraConfig.MODE_FLASH_OFF: {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            }
        }
    }

    private Size getOptimalPreviewSize(Size[] sizes) {
        if (sizes == null) return null;
        Point displaySize = new Point();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getSize(displaySize);
        Size optimalSize = null;
        double ratio = (double) displaySize.x / displaySize.y;
        double minDiff = Double.MAX_VALUE;
        double newDiff;
        for (Size size : sizes) {
            newDiff = Math.abs((double) size.getHeight() / size.getWidth() - ratio);
            if (newDiff < minDiff) {
                optimalSize = size;
                minDiff = newDiff;
            }
        }
        return optimalSize;
    }

    @Override
    public void regionsFocus(Rect touchRect) {
        MeteringRectangle focusAreaTouch = new MeteringRectangle(touchRect, MeteringRectangle.METERING_WEIGHT_MAX - 1);
        try {
            CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NotNull CameraCaptureSession session, @NotNull CaptureRequest request, @NotNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    if (request.getTag() == "FOCUS_TAG") {
                        //the focus trigger is complete -
                        //resume repeating (preview surface will get frames), clear AF trigger
                        try {
                            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                            mCameraCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallBack,
                                    mHandler);
                            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallBack, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void onCaptureFailed(@NotNull CameraCaptureSession session, @NotNull CaptureRequest request, @NotNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    DebugLog.d("Manual AF failure: " + failure);
                }
            };

            //first stop the existing repeating request
            mCameraCaptureSession.stopRepeating();

            //cancel any existing AF trigger (repeated touches, etc.)
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            mCameraCaptureSession.capture(mCaptureBuilder.build(), captureCallbackHandler, mHandler);

            //Now add a new AF trigger with focus region
            if (mMeteringAreaAFSupported) {
                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
            }
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCaptureBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

            //then we ask for a single request (not repeating!)
            mCameraCaptureSession.capture(mCaptureBuilder.build(), captureCallbackHandler, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

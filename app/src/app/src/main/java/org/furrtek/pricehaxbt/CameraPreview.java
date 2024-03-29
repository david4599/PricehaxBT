package org.furrtek.pricehaxbt;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.IOException;

import android.content.res.Configuration;

public class CameraPreview extends SurfaceView implements Callback {
    private AutoFocusCallback autoFocusCallback;
    private Camera mCamera;
    private SurfaceHolder mHolder = getHolder();
    private PreviewCallback previewCallback;
    private boolean oldAutoFocusMode;
    private Context context;
    private int cameraId;


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCameraDisplayOrientation((Activity) this.context, this.cameraId, this.mCamera);
    }

    public CameraPreview(Context context, Camera camera, PreviewCallback previewCb, AutoFocusCallback autoFocusCb) {
        super(context);
        this.context = context;

        this.mCamera = camera;
        this.previewCallback = previewCb;
        this.autoFocusCallback = autoFocusCb;
        this.mHolder.addCallback(this);
        this.mHolder.setType(3);

        this.cameraId = getBackCameraId();

        // The old mode uses the onAutoFocus function and makes a timer to try to focus periodically.
        // The drawback is the hunting of the camera.
        // This can be "smoothed" by using either FOCUS_MODE_CONTINUOUS_PICTURE or FOCUS_MODE_CONTINUOUS_VIDEO parameters instead (if available).
        Camera.Parameters params = this.mCamera.getParameters();

        this.oldAutoFocusMode = false;
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        else {
            this.oldAutoFocusMode = true;
        }

        this.mCamera.setParameters(params);
    }

    public boolean isOldAutoFocusMode() {
        return this.oldAutoFocusMode;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (this.mCamera != null) {
                this.mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException e) {
            Log.d("DBG", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (this.mHolder.getSurface() != null) {
            try {
                this.mCamera.stopPreview();
            } catch (Exception e) {
            }
            try {
                setCameraDisplayOrientation((Activity) this.context, this.cameraId, this.mCamera);

                this.mCamera.setPreviewDisplay(this.mHolder);
                this.mCamera.setPreviewCallback(this.previewCallback);
                this.mCamera.startPreview();

                if (this.oldAutoFocusMode) {
                    this.mCamera.autoFocus(this.autoFocusCallback);
                }

            } catch (Exception e2) {
                Log.d("DBG", "Error starting camera preview: " + e2.getMessage());
            }
        }
    }

    // Adapted from Android SDK API sample code
    // https://github.com/Miserlou/Android-SDK-Samples/blob/master/ApiDemos/src/com/example/android/apis/graphics/CameraPreview.java
    private int getBackCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();

        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }

        return 0;
    }

    // Sample code from Android Developer documentation
    // https://developer.android.com/reference/android/hardware/Camera#setDisplayOrientation%28int%29
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
    }
}

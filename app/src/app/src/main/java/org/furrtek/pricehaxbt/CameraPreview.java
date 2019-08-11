package org.furrtek.pricehaxbt;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
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


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkOrientation(newConfig);
    }

    private void checkOrientation(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.mCamera.setDisplayOrientation(90);
        } else {
            this.mCamera.setDisplayOrientation(0);
        }
    }

    public CameraPreview(Context context, Camera camera, PreviewCallback previewCb, AutoFocusCallback autoFocusCb) {
        super(context);
        this.mCamera = camera;
        this.previewCallback = previewCb;
        this.autoFocusCallback = autoFocusCb;
        this.mHolder.addCallback(this);
        this.mHolder.setType(3);
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
                if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    this.mCamera.setDisplayOrientation(90);
                }
                this.mCamera.setPreviewDisplay(this.mHolder);
                this.mCamera.setPreviewCallback(this.previewCallback);
                this.mCamera.startPreview();
                this.mCamera.autoFocus(this.autoFocusCallback);
            } catch (Exception e2) {
                Log.d("DBG", "Error starting camera preview: " + e2.getMessage());
            }
        }
    }
}

package com.xiaoge.utils.cameracompat;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.text.TextUtils;

import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.open;

/**
 * Created by zhanglei on 17-2-17.
 *
 */

class CameraPreLollipop {

    private Camera camera;
    private Camera.Parameters parameters;
    CameraPreLollipop(){
    }

    private void openCamera() {
        try {
            camera = open();
            parameters = camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean openFlashLight(){
        try {
            openCamera();

            if (camera == null) {
                return false;
            }

            List flashModes = parameters.getSupportedFlashModes();

            if (flashModes == null || flashModes.size() <= 0) {
                parameters.setFlashMode(FLASH_MODE_TORCH);
            } else if (flashModes.contains(FLASH_MODE_TORCH)) {
                parameters.setFlashMode(FLASH_MODE_TORCH);
            } else if (flashModes.contains(FLASH_MODE_ON)) {
                parameters.setFlashMode(FLASH_MODE_ON);
            }

            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    boolean closeFlashLight(){
        if (camera == null) {
            openCamera();
        }

        if (camera == null) {
            return false;
        }

        if (parameters == null) {
            return false;
        }

        String flashMode = parameters.getFlashMode();
        if (TextUtils.equals(flashMode, FLASH_MODE_TORCH)) {
            try {
                parameters.setFlashMode(FLASH_MODE_OFF);
                camera.setParameters(parameters);
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
                parameters = null;
            } catch (RuntimeException e) {
                e.printStackTrace();
                try {
                    if (camera != null) {
                        camera.setPreviewCallback(null);
                        camera.stopPreview();
                        camera.release();
                        camera = null;
                        parameters = null;
                    }
                } catch (Exception ignore) {
                    return false;
                }
            }
        }
        return true;
    }

}

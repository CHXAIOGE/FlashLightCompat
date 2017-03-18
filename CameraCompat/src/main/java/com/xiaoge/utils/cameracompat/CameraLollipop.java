package com.xiaoge.utils.cameracompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;

/**
 * Created by zhanglei on 17-2-17.
 *
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraLollipop {

    private CameraManager manager;
    private Context context;
    private String cameraId;

    private CameraDevice mCameraDevice;
    private CaptureRequest mFlashlightRequest;
    private CaptureRequest.Builder mBuilder;
    private CameraCaptureSession mSession;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Handler mHandler;

    private boolean mFlashlightEnabled;

    private static final String TAG = "CameraLollipop";

    CameraLollipop(Context context) {
        this.context = context;
        getService(context);
    }

    private void getService(Context context) {
        try {
            manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        } catch (Exception ignore) {
        }
    }

    boolean openFlashLight() {
        if (context == null) {
            return false;
        }
        try {
            if (cameraId == null) {
                cameraId = getCameraIdWithFlashLight();
            }

            if (cameraId != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setTorchMode(cameraId, true);
                } else {
                    setFlashlight(true);
                }
            }
        } catch (Exception ignore) {
            return false;
        }
        return true;
    }

    boolean closeFlashLight() {
        if (context == null) {
            return false;
        }

        try {
            if (cameraId == null) {
                cameraId = getCameraIdWithFlashLight();
            }

            if (cameraId != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setTorchMode(cameraId, false);
                } else {
                    setFlashlight(false);
                }
            }
        } catch (Exception ignore) {
            return false;
        }
        return true;

    }

    private String getCameraIdWithFlashLight() {
        try {
            String[] ids = manager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(id);
                Boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null && flashAvailable
                        && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (Exception ignore) {

        }
        return null;
    }

    private synchronized void startHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mHandler = new Handler(thread.getLooper());
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            postUpdateFlashlight();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice == camera) {
                releaseResource();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private final CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (session.getDevice() == mCameraDevice) {
                mSession = session;
            } else {
                session.close();
            }
            postUpdateFlashlight();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    public void killFlashlight() {
        boolean enabled;
        synchronized (this) {
            enabled = mFlashlightEnabled;
        }
        if (enabled) {
            mHandler.post(mKillFlashlightRunnable);
        }
    }

    private void openCameraSession() throws CameraAccessException {
        mSurfaceTexture = new SurfaceTexture(0, false);
        Size size = getSmallestSize(mCameraDevice.getId());
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mSurface = new Surface(mSurfaceTexture);
        ArrayList<Surface> outputs = new ArrayList<>(1);
        outputs.add(mSurface);
        mCameraDevice.createCaptureSession(outputs, mSessionStateCallback, mHandler);
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "doesn't support any outputSize!");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    private void updateFlashlight(boolean status) {
        try {
            boolean enabled;
            synchronized (this) {
                enabled = mFlashlightEnabled && !status;
                if (enabled) {
                    if (mCameraDevice == null) {
                        openCameraDevice();
                        return;
                    }
                    if (mSession == null) {
                        openCameraSession();
                        return;
                    }

                    if (mFlashlightRequest == null) {
                        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(
                                CameraDevice.TEMPLATE_PREVIEW);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                        builder.addTarget(mSurface);
                        mFlashlightRequest = builder.build();
                        mSession.capture(mFlashlightRequest, null, mHandler);
                    }
                } else {
                    releaseResource();
                }
            }
        } catch (CameraAccessException | IllegalStateException | UnsupportedOperationException ignore) {
        }
    }

    private void releaseResource() {
        if (mBuilder != null) {
            mBuilder.removeTarget(mSurface);
            mBuilder = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        mCameraDevice = null;
        mSession = null;
        mFlashlightRequest = null;
        if (mSurface != null) {
            mSurface.release();
            mSurfaceTexture.release();
        }
        mSurface = null;
        mSurfaceTexture = null;
    }

    private void openCameraDevice() throws CameraAccessException {
        if (context != null && ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.openCamera(getCameraIdWithFlashLight(), mStateCallback, mHandler);
    }

    private void postUpdateFlashlight() {
        startHandler();
        mHandler.post(mUpdateFlashlightRunnable);
    }

    private synchronized void setFlashlight(boolean enabled) {
        if (mFlashlightEnabled != enabled) {
            mFlashlightEnabled = enabled;
            postUpdateFlashlight();
        }
    }

    private final Runnable mUpdateFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            updateFlashlight(false);
        }
    };

    private final Runnable mKillFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                mFlashlightEnabled = false;
            }
            updateFlashlight(true);
        }
    };
}

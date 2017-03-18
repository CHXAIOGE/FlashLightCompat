package com.xiaoge.utils.cameracompat;

import android.content.Context;
import android.os.Build;

/**
 * Created by zhanglei on 17-2-17.
 *
 */

public class CameraCompat {

    private CameraLollipop cameraLollipop;
    private CameraPreLollipop cameraPreLollipop;

    public CameraCompat(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraLollipop = new CameraLollipop(context);
        }
        cameraPreLollipop = new CameraPreLollipop();
    }

    public boolean openFlashLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(!cameraLollipop.openFlashLight()) {
                return cameraPreLollipop.openFlashLight();
            }
        } else {
            return cameraPreLollipop.openFlashLight();
        }
        return true;
    }

    public boolean closeFlashLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(!cameraLollipop.closeFlashLight()) {
                return cameraPreLollipop.closeFlashLight();
            }
        } else {
            return cameraPreLollipop.closeFlashLight();
        }
        return true;
    }

}

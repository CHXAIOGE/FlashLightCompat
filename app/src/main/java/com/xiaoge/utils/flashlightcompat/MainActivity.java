package com.xiaoge.utils.flashlightcompat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.xiaoge.utils.cameracompat.CameraCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CameraCompat cameraCompat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.close).setOnClickListener(this);
        cameraCompat = new CameraCompat(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.open){
            cameraCompat.openFlashLight();
        }else {
            cameraCompat.closeFlashLight();
        }
    }
}

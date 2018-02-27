package com.skylight.listener;


import android.content.Intent;

import com.skylight.camera.status.CAMERA_STATUS;
import com.skylight.mode.CameraType;


public interface ICameraStatusChangedListener {
    public void onStatusUpdate(CAMERA_STATUS status, CameraType type, Intent intent);
}

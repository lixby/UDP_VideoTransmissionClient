package com.skylight.camera.status;

import android.content.Intent;

/**
 * Description:
 * Author: Created by lixby on 17-12-19.
 */

public interface ICameraStatusChangedListener {

    public void onStatausUpdate(CAMERA_STATUS status, CameraType type, Intent intent);

}

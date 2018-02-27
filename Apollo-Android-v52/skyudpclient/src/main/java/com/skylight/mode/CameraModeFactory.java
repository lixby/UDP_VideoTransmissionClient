package com.skylight.mode;

/**
 * Created by xiaobin on 2017/11/29.
 * Description: this class is a factory for create CameraMode.
 */

public class CameraModeFactory {

    public static <T extends CameraMode> T createMode(Class<T> clz){

        CameraMode cameraMode=null;
        try {
            cameraMode= (CameraMode) Class.forName(clz.getName()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (T)cameraMode;
    }


}

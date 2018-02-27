package com.skylight.camera.status;

/**
 * Description:
 * Author: Created by lixby on 17-12-19.
 */

public enum CAMERA_STATUS {

    CAMERA_ATTACHED                   (1,"Device attached"),
    CAMERA_DETACHED                   (2,"Device dttached"),
    CAMERA_NEED_PERMISSION            (3,"Device need permission"),
    CAMERA_PERMISSION_REFUSEED        (4,"device attached"),
    CAMERA_INIT_SUCCESS               (5,"device attached"),
    CAMERA_INIT_FAILURE               (6,"device attached");


    public final int code;
    public final String msg;

    CAMERA_STATUS(int code,String msg){
        this.code=code;
        this.msg=msg;

    }

}

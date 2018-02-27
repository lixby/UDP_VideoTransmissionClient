package com.skylight.command;

import com.skylight.command.callback.ICmdHandlerCallback;
import com.skylight.mode.CameraMode;

/**
 * Created by Administrator on 2017/11/27.
 */

public abstract class CommandHandler<T> {


    /*Command waiting to be executed*/
    protected  static final int CMD_WAIT_EXECUTE=0;
    /*Command to stop execution*/
    protected  static final int CMD_STOP_EXECUTE=-1;


    /**
     * Command execution status
     * 0:Continue sending the command
     * -1:stop sending the command
     */
    private int  status= CMD_WAIT_EXECUTE;

    /**Command execution params*/
    public T params;

    /**Specific orders executed*/
    public CameraMode cameraMode;

    public void setParams(T params) {
        this.params = params;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void stop(){
        this.status=CMD_STOP_EXECUTE;
    }

    public void setCameraMode(CameraMode cameraMode) {
        this.cameraMode = cameraMode;
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public CommandHandler() {

    }

    public CommandHandler(CameraMode cameraMode) {
        this.cameraMode = cameraMode;
    }


    /**execute command*/
    public abstract void execute();

    public ICmdHandlerCallback cmdHandlerCallback;

    public ICmdHandlerCallback getCmdHandlerCallback() {
        return cmdHandlerCallback;
    }

    /**
     * Specific callbacks for command execution result
     */
    public void setCommandResponseCallBack(ICmdHandlerCallback commandResponseCallBack) {
        this.cmdHandlerCallback = commandResponseCallBack;
    }



}

package com.skylight.camera.manager;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.skylight.camera.status.CAMERA_STATUS;
import com.skylight.client.tcp.handlercallback.TcpIpStatusHandlerCallback;
import com.skylight.command.callback.CmdStatus;
import com.skylight.listener.CameraBatteryCallBack;
import com.skylight.listener.ICameraStatusChangedListener;
import com.skylight.mode.CameraMode;
import com.skylight.mode.CameraModeFactory;
import com.skylight.mode.CameraType;
import com.skylight.mode.WifiCameraMode;
import com.skylight.mode.camera.ProductionInformation;
import com.skylight.util.Logger;

import java.util.ArrayList;


/**
 *
 *Description: CameraManager class to establish a bridge between users and devices,
 * users do not need to be specific about the implementation, you only need to use
 * the API and the upper application layer API can be.
 * you can use it like this:
 *
 * <pre>
 * CameraManager cameraManager=CameraManager.instance(getApplicationContext());
 * </pre>
 *
 *
 */

public class CameraManager implements ICameraStatusChangedListener {


    private static final String VERSION="camera-manager-v1.0.01";

    private boolean isConnected;
    private ProductionInformation information;

    /**Camera status changed listeners*/
    private ArrayList<ICameraStatusChangedListener> statusListeners = new ArrayList<ICameraStatusChangedListener>();
    private ArrayList<ICameraStatusChangedListener> sendStatuslisteners = null;

    private ArrayList<CameraBatteryCallBack> batteryListeners = new ArrayList<CameraBatteryCallBack>();

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static CameraManager instance;
    private Context context;
    private CameraMode mode;

    private final Object obj = new Object();

    public static synchronized CameraManager instance(Context context) {
        if (instance == null) {
            Logger.enable(true);
            instance = new CameraManager(context);
            Logger.i("CameraManager_version = "+ VERSION);
        }
        return instance;
    }

    private CameraManager(Context context) {
        this.context = context;

    }

    @Override
    public void onStatusUpdate(CAMERA_STATUS status, CameraType type, Intent intent) {
        switch (status) {
            case CAMERA_ATTACHED:
                onCameraStatusChange(status, type, intent);
                createCameraMode(type,intent);

                break;
            case CAMERA_DETACHED:
                release();
                onCameraStatusChange(status, type, intent);
                break;
            case CAMERA_NEED_PERMISSION:

                break;
            case CAMERA_PERMISSION_REFUSEED:
                onCameraStatusChange(status, type, intent);

                break;
        }

    }

    /**Create Camera Mode*/
    private void createCameraMode(final CameraType type, final Intent intent){
        switch (type) {
            case USBCAMERA:// Init camera usb device

                break;
            case WIFICAMERA://Init camera wifi device
                this.information=new ProductionInformation();
                this.mode= CameraModeFactory.createMode(WifiCameraMode.class);
                this.mode.initialized(intent, information, new TcpIpStatusHandlerCallback() {
                    @Override
                    public void inItSuccess() {
                        isConnected=true;
                        information.setConnected(true);
                        onCameraStatusChange(CAMERA_STATUS.CAMERA_INIT_SUCCESS, type, intent);
                    }

                    @Override
                    public void inItFailed() {
                        onCameraStatusChange(CAMERA_STATUS.CAMERA_INIT_FAILURE, type, intent);
                        isConnected=true;
                        mode=null;
                    }

                    @Override
                    public void disConnected() {
                        onCameraStatusChange(CAMERA_STATUS.CAMERA_DETACHED, type, intent);
                        release();

                    }

                    @Override
                    public void responseStatus(CmdStatus status) {

                    }
                });
                break;
            default:
                break;
        }

    }


    /**Camera status change callback for UI*/
    private void onCameraStatusChange(final CAMERA_STATUS stataus, final CameraType type, final Intent intent) {
        synchronized (obj) {
            if (statusListeners.size() > 0) {
                sendStatuslisteners = (ArrayList<ICameraStatusChangedListener>) statusListeners.clone();
            }
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sendStatuslisteners != null) {
                    for (ICameraStatusChangedListener onStatusChangedListener : sendStatuslisteners) {
                        if (onStatusChangedListener != null) {
                            onStatusChangedListener.onStatusUpdate(stataus, type, intent);
                        }
                    }
                }
            }
        });

    }

    /**Add listener for device status changes*/
    public void addOnStatusChangedListener(ICameraStatusChangedListener onStatusChangedListener) {
        synchronized (obj) {
            if (!statusListeners.contains(onStatusChangedListener)) {
                statusListeners.add(onStatusChangedListener);
            }
        }

    }

    /**Delete listener for device status changes*/
    public void removeStatusChangedListener(ICameraStatusChangedListener callBack) {
        synchronized (obj) {
            if (statusListeners.contains(callBack)) {
                statusListeners.remove(callBack);
            }
        }

    }

    /************************************Add api***************************************************/


    public boolean isConnected() {
        if(!isConnected){
            String status="disconnected";
            Logger.d("CAMERA_STATUS:"+ status);
        }
        return isConnected;
    }

    public ProductionInformation getInformation() {
        return information;
    }

    /**Set Camera battery automatically callback interface*/
    public void setCamBatteryAutomaticCallBack(CameraBatteryCallBack callBack){
        batteryListeners.add(callBack);
        if(mode!=null){
            mode.addAutomaticBatteryListener(callBack);
        }

    }

    /**Cancel Camera battery automatically callback interface*/
    public void cancelCamBatteryAutomaticCallBack(CameraBatteryCallBack callBack){
        if(batteryListeners.contains(callBack)){
            batteryListeners.remove(callBack);
        }
        if(mode!=null){
            mode.removeAutomaticBatteryListener(callBack);
        }

    }

    /**Restore Camera battery automatic callback*/
    private void setAutomaticCamBatteryCallBack(){
        if(batteryListeners.size()>0){
            for (CameraBatteryCallBack callBack : batteryListeners) {
                if (callBack != null) {
                    mode.addAutomaticBatteryListener(callBack);
                }
            }
        }

    }

    /** Release all resources */
    public void release() {
        if(mode!=null){
            mode.destroy();
        }
        if (isConnected()) {
            isConnected = false;
            information.clear();
        }

    }


}

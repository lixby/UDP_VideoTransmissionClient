package com.skylight.mode;

/**
 * Created by Administrator on 2017/10/19.
 */

import android.os.Handler;
import android.os.Looper;

import com.skylight.client.tcp.handlercallback.StreamCmdHandlerCallback;
import com.skylight.command.callback.ICmdHandlerCallback;
import com.skylight.listener.CameraBatteryCallBack;
import com.skylight.listener.IFwUpdataCallBack;
import java.util.ArrayList;
import java.util.Observable;

public class CameraMode<T, V, E> extends Observable {

    protected Handler mainHandler = new Handler(Looper.getMainLooper());
    protected Object obj = new Object();

    protected ArrayList<CameraBatteryCallBack> batteryListeners = new ArrayList<>();
    protected ArrayList<CameraBatteryCallBack> sendBatteryListeners = null;

    public Handler getMainHandler() {
        return mainHandler;
    }

    public boolean isSendHeartbeat() {
        return false;
    }

    /**Start video stream*/
    public Object obtainStream(StreamCmdHandlerCallback callback) {
        return null;
    }

    /**Stop video stream*/
    public Object releaseStream(ICmdHandlerCallback callback) {
        return null;
    }

    public Object getLensParam() {
        return null;
    }

    public Object getStreamInfo() {
        return null;
    }

    public Object setLensParam(String lens) {
        return null;
    }

    public Object setCameraTime(String time) {
        return null;
    }

    public Object setIQParams(int iso, int awb, int ev, int st) {
        return null;
    }

    public Object setIQParamsByJson(String param) {
        return null;
    }

    public Object getDefaultIQParams() {
        return null;
    }

    public Object setDeviceId(String id) {
        return null;
    }

    public Object setDeviceSN(String sn) {
        return null;
    }

    public Object getDeviceSN() {
        return null;
    }

    public Object getDelayedTime() {
        return null;
    }

    public Object getBatteryPower() {
        return null;
    }

    public Object setStopChargingPower(String power) {
        return null;
    }

    public Object getMCU_SN() {
        return null;
    }

    public Object setCameraLogPath(String path) {
        return null;
    }

    public Object setWB(String wb) {
        return null;
    }

    public Object getWB() {
        return null;
    }

    public Object setCircle_Calib(String Circle_Calib) {
        return null;
    }

    public Object getCircle_Calib() {
        return null;
    }

    public Object restoreFactory() {
        return null;
    }

    public Object setCameraLogLevel(String level) {
        return null;
    }

    public Object shutDown() {
        return null;
    }

    public Object postAutoShutDown(String min) {
        return null;
    }

    public Object postAutoShutDownStart() {
        return null;
    }

    public Object postAutoShutDownStop() {
        return null;
    }

    public Object updateCameraFrameware(String filePath, String md5) {
        return null;
    }

    public Object getUpdateProgress() {
        return null;
    }

    public String getSdkVersion() {
        return null;
    }

    public Object getTemperature() {
        return null;
    }

    public Object sendHeartBeat() {
        return null;
    }

    public Object sendStopReceivingUpdateFile() {
        return null;
    }

    public Object sendSetBackGround() {
        return null;
    }

    public Object setPid(String pid) {
        return null;
    }

    public Object getPid() {
        return null;
    }

    public Object sendQueryUpdateStatus() {
        return null;
    }

    public Object setHW(String hw) {
        return null;
    }

    public Object getHW() {
        return null;
    }

    public Object setCameraCircularLogPath(String path) {
        return null;
    }

    public Object getMCU_HW() {
        return null;
    }

    public Object getDeviceInfo() {
        return null;
    }

    public void onReceiveUpdataProgress(IFwUpdataCallBack callBack) {

    }

    public void onReceiveVideo(Object obj) {

    }

    /**Initialized all data*/
    public Object initialized(T t, V v, E e) {
        return false;
    }

    /**Release all source*/
    public void destroy() {

    }

    /**add camera automatic battery listeners*/
    public void addAutomaticBatteryListener(CameraBatteryCallBack callBack){
        synchronized (obj) {
            if (!batteryListeners.contains(callBack)) {
                batteryListeners.add(callBack);
            }
        }

    }

    /**remove camera automatic battery listeners*/
    public void removeAutomaticBatteryListener(CameraBatteryCallBack callBack) {
        synchronized (obj) {
            if (batteryListeners.contains(callBack)) {
                batteryListeners.remove(callBack);
            }
        }

    }

    /**remove camera automatic battery listeners*/
    protected void clearAllAutomaticBatteryListener() {
        synchronized (obj) {
            if (batteryListeners.size()>0) {
                batteryListeners.clear();
            }
        }

    }

    /**notify camera battery changed*/
    protected void notifyBatteryChanged(final int power){
        synchronized (obj) {
            if (batteryListeners.size() > 0) {
                sendBatteryListeners = (ArrayList<CameraBatteryCallBack>) batteryListeners.clone();
            }
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sendBatteryListeners != null) {
                    for (CameraBatteryCallBack batteryCallBack : sendBatteryListeners) {
                        if (batteryCallBack != null) {
                            batteryCallBack.onBatteryCallBack(power);
                        }
                    }
                }
            }
        });

    }


}

package com.skylight.mode.camera;

public class ProductionInformation {
    private Information camera;
    private Information caseInfo;
    private String cameraSn = "";
    private String caseSn   = "";
    private String fwVer    = "";
    private String lens = "";
    private boolean isStreaming;
    private boolean isFwUpdating;
    private boolean isConnected;
    private int volt;
    private String iq;
    private String logPath;


    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public Information getCameraInfo() {
        return camera;
    }

    public void setCameraInfo(Information cameraInfo) {
        this.camera = cameraInfo;
    }

    public Information getCaseInfo() {
        return caseInfo;
    }

    public void setCaseInfo(Information caseInfo) {
        this.caseInfo = caseInfo;
    }

    public String getCameraSn() {
        return cameraSn;
    }

    public void setCameraSn(String cameraSn) {
        this.cameraSn = cameraSn;
    }

    public String getCaseSn() {
        return caseSn;
    }

    public void setCaseSn(String caseSn) {
        this.caseSn = caseSn;
    }

    public String getFwVer() {
        return fwVer;
    }

    public void setFwVer(String fwVer) {
        this.fwVer = fwVer;
    }

    public String getLens() {
        return lens;
    }

    public void setLens(String lens) {
        this.lens = lens;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    public boolean isFwUpdating() {
        return isFwUpdating;
    }

    public void setFwUpdating(boolean fwUpdating) {
        isFwUpdating = fwUpdating;
    }

    public int getVolt() {
        return volt;
    }

    public void setVolt(int volt) {
        this.volt = volt;
    }

    public String getIq() {
        return iq;
    }

    public void setIq(String iq) {
        this.iq = iq;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ProductionInformation)) return false;
        return camera != null && camera.equals(ProductionInformation.class.cast(obj).camera) &&
                caseInfo != null && caseInfo.equals(ProductionInformation.class.cast(obj).caseInfo);
    }

    public ProductionInformation clone() {
        ProductionInformation result = new ProductionInformation();
        result.camera = camera.clone();
        result.caseInfo = caseInfo.clone();
        return result;
    }

    public boolean isVaild(){
        return  camera != null && camera.isVaild() &&
                caseInfo   != null && caseInfo.isVaild();
    }
    public void clear() {
        camera = null;
        caseInfo = null;
        cameraSn = "";
        caseSn = "";
        fwVer = "";
        lens = "";
        isStreaming = false;
        isFwUpdating = false;
        isConnected = false;
        volt = 0;
        iq = "";
        logPath = "";
    }
}

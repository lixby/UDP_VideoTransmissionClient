package com.skylight.mode.camera;

import android.text.TextUtils;


public class Information {
    private String product = "ION360U";
    private String model;
    private String platform = "andr";
    private String version;

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Information)) return false;
        return TextUtils.equals(product, Information.class.cast(obj).product) &&
                TextUtils.equals(model, Information.class.cast(obj).model) &&
                TextUtils.equals(platform, Information.class.cast(obj).platform) &&
                TextUtils.equals(version, Information.class.cast(obj).version);
    }

    public Information clone(){
        Information result = new Information();
        result.product = product;
        result.model = model;
        result.platform = platform;
        result.version = version;
        return result;
    }

    public boolean isVaild(){
        return  !TextUtils.isEmpty(product) &&
                !TextUtils.isEmpty(model) &&
                !TextUtils.isEmpty(platform) &&
                !TextUtils.isEmpty(version);
    }
}

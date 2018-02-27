package com.skylight.apollo.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Description:
 * Author: Created by lixby on 18-1-15.
 */

public class SharedPreferencesCache {

    private static final String CACHE_NAME="sd_lxb";

    /**The key tcp ip address*/
    public static final String TCPSERVER_IP="tcp_server_ip";


    public static String get(Context cext, String key){
        SharedPreferences chace=cext.getSharedPreferences(CACHE_NAME,Context.BIND_ABOVE_CLIENT);
        return chace.getString(key,null);
    }

    public static void put(Context cext, String key,String value){
        SharedPreferences chace=cext.getSharedPreferences(CACHE_NAME,Context.BIND_ABOVE_CLIENT);
        chace.edit().putString(key,value).commit();
    }


}

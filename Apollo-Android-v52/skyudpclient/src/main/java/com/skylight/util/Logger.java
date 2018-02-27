package com.skylight.util;

import android.util.Log;

public class Logger
{
    public static boolean DEBUG = true;

    public static String  TAG   = "Cm-Lib";

    public static void enable(boolean enable)
    {
        DEBUG=enable;

    }

    public static void d(String msg)
    {
        if (DEBUG)
            Log.d(TAG, msg);
    }

    public static void d(String msg, Throwable t)
    {
        if (DEBUG)
            Log.d(TAG, msg, t);
    }
    
    public static void i(String msg)
    {
    	if (DEBUG)
    		Log.i(TAG, msg);
    }
    
    public static void i(String msg, Throwable t)
    {
    	if (DEBUG)
    		Log.i(TAG, msg, t);
    }

    public static void e(String msg)
    {
        if (DEBUG)
            Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable t)
    {
        if (DEBUG)
            Log.e(TAG, msg, t);
    }

    public static void w(String msg)
    {
        if (DEBUG)
            Log.w(TAG, msg);
    }

    public static void printMemory(String msg)
    {
        d(msg);
        d("maxMemory: " + Runtime.getRuntime().maxMemory()/1024 + "KB");
        d("totalMemory: " + Runtime.getRuntime().totalMemory()/1024 + "KB");
        d("freeMemory: " + Runtime.getRuntime().freeMemory()/1024 + "KB");
        d("nativeHeapSize: " + android.os.Debug.getNativeHeapSize()/1024 + "KB");
        d("nativeHeapAllocatedSize: " + android.os.Debug.getNativeHeapAllocatedSize()/1024 + "KB");
        d("nativeHeapFreeSize: " + android.os.Debug.getNativeHeapFreeSize()/1024 + "KB");
    }
}

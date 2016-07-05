package com.orbbec.NativeNI;

public class NativeMethod {

    static{
       System.loadLibrary("depthColorOutput");
    }

    public native static boolean CoventFromDepthTORGB(  int[] pdepth,int width, int height );
    public native static int GetRGBData(byte[] pByteColor,int[] pIntColor, int width, int height );

}
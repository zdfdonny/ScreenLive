package com.test.screenlive.record;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.test.screenlive.constants.Constant;
import com.test.screenlive.data.H264DataCollecter;
import com.test.screenlive.media.VideoMediaCodec;

public class ScreenRecordThread extends Thread {

    private final static String TAG = "ScreenRecord";

    private Surface mSurface;
    private Context mContext;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;

    private VideoMediaCodec mVideoMediaCodec;

    private WindowManager wm;
    private int screenDensity;


    public ScreenRecordThread(Context context, MediaProjection mp, H264DataCollecter mH264Collecter){
        this.mContext = context;
        this.mMediaProjection = mp;
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;
        mVideoMediaCodec = new VideoMediaCodec(wm, context, mH264Collecter);
    }

    @Override
    public void run() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;
        mVideoMediaCodec.prepare();
        mSurface =  mVideoMediaCodec.getSurface();
        mVirtualDisplay =mMediaProjection.createVirtualDisplay(TAG + "-display", Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);
        mVideoMediaCodec.isRun(true);
        mVideoMediaCodec.getBuffer();
    }

    /**
     * 停止
     * **/
    public void release(){
        mVideoMediaCodec.release();
    }




}

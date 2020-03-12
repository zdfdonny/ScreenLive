package com.test.screenlive.media;

import android.media.MediaCodec;

import com.test.screenlive.data.H264DataCollecter;

public abstract class MediaCodecBase {

    protected MediaCodec mEncoder;

    protected boolean isRun = false;

    public abstract void prepare();

    public abstract void release();

    protected H264DataCollecter mH264Collecter;

}

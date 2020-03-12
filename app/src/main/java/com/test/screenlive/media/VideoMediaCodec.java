package com.test.screenlive.media;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.test.screenlive.constants.Constant;
import com.test.screenlive.data.H264DataCollecter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoMediaCodec extends MediaCodecBase {

    private final static String TAG = "VideoMediaCodec";
    private Surface mSurface;
    private long startTime = 0;
    private int TIMEOUT_USEC = 11000;
    public byte[] configbyte;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.h264";
    private BufferedOutputStream outputStream;
    FileOutputStream outStream;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private long timeStamp = 0;

    private Context context;
    private void createfile(){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     * **/
    public VideoMediaCodec(WindowManager wm, Context context, H264DataCollecter mH264Collecter){
        this.mH264Collecter = mH264Collecter;
        this.context = context;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        //createfile();
        prepare();
    }

    public Surface getSurface(){
        return mSurface;
    }

    public void isRun(boolean isR){
        this.isRun = isR;
    }


    @Override
    public void prepare(){
        try{
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Constant.VIDEO_BITRATE);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Constant.VIDEO_FRAMERATE);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constant.VIDEO_IFRAME_INTER);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, Constant.DEFAULT_CHANNEL_COUNT);
            mediaFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, 25);
            mediaFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / 25);

            mEncoder = MediaCodec.createEncoderByType(Constant.MIME_TYPE);
            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
            timeStamp = System.currentTimeMillis();
            mEncoder.start();
        }catch (IOException e){

        }
    }

    @Override
    public void release() {
        this.isRun = false;
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }

    }


    /**
     * 获取h264数据
     * **/
    public void getBuffer(){

        try
        {
            while(isRun){
                if(mEncoder == null)
                    break;
                if (startTime == 0) {
                    startTime = mBufferInfo.presentationTimeUs * 1000;
                }

                if (System.currentTimeMillis() - timeStamp >= 1000) {
                    timeStamp = System.currentTimeMillis();
                    Bundle params = new Bundle();
                    params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 2);
                    mEncoder.setParameters(params);
                }
                int outputBufferIndex  = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat outputFormat = mEncoder.getOutputFormat();
                    byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(outputFormat);
                    int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                            AVCDecoderConfigurationRecord.length;
                    byte[] finalBuff = new byte[packetLen];
                    Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                            0,
                            true,
                            true,
                            AVCDecoderConfigurationRecord.length);
                    System.arraycopy(AVCDecoderConfigurationRecord, 0,
                            finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);

                    H264Data data = new H264Data(finalBuff, 1, 10);
                    if (mH264Collecter != null){
                        mH264Collecter.collect(data);
                    }
                }

                while (outputBufferIndex >= 0){
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);

//                    MediaFormat bufferFormat = mEncoder.getOutputFormat(outputBufferIndex);

                    byte[] outData = new byte[mBufferInfo.size];
                    outputBuffer.get(outData);
                    if(mBufferInfo.flags == 2){
                        configbyte = new byte[mBufferInfo.size];
                        configbyte = outData;
                    }else if(mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME){
                        byte[] keyframe = new byte[mBufferInfo.size + configbyte.length];
                        System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                        System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
                        H264Data data = new H264Data(keyframe, 1, mBufferInfo.presentationTimeUs*1000);
                        if (mH264Collecter != null){
                            mH264Collecter.collect(data);
                        }
                    }else{
                        H264Data data = new H264Data(outData, 2, mBufferInfo.presentationTimeUs*1000);
                        if (mH264Collecter != null){
                            mH264Collecter.collect(data);
                        }
                    }
                    mEncoder.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                }
            }
        }
        catch (Exception e){

        }
        try {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}

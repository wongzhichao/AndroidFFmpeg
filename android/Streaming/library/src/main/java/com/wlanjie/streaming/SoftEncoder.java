package com.wlanjie.streaming;

import android.content.res.Configuration;

import com.wlanjie.streaming.camera.CameraView;

import java.nio.ByteBuffer;

/**
 * Created by wlanjie on 2016/11/29.
 */

class SoftEncoder extends Encoder {

    public SoftEncoder(Builder builder) {
        super(builder);
    }

    @Override
    boolean openEncoder() {

        setEncoderFps(mBuilder.fps);
        setEncoderGop(mBuilder.fps * 2);
        // Unfortunately for some android phone, the output fps is less than 10 limited by the
        // capacity of poor cheap chips even with x264. So for the sake of quick appearance of
        // the first picture on the player, a spare lower GOP value is suggested. But note that
        // lower GOP will produce more I frames and therefore more streaming data flow.
        setEncoderBitrate(mBuilder.videoBitRate);
        setEncoderPreset(mBuilder.x264Preset);

        return openH264Encoder() &&
                openAacEncoder(mAudioRecord.getChannelCount(), mBuilder.audioSampleRate, mBuilder.audioBitRate);
    }

    @Override
    void closeEncoder() {
        closeH264Encoder();
        closeAacEncoder();
    }

    @Override
    void convertYuvToH264(byte[] data) {
        long pts = System.nanoTime() / 1000 - mPresentTimeUs;
        boolean isFront = mBuilder.cameraView.getFacing() == CameraView.FACING_FRONT;
        NV21EncodeToH264(data,
                mBuilder.previewWidth,
                mBuilder.previewHeight,
                isFront,
                mOrientation == Configuration.ORIENTATION_PORTRAIT ? isFront ? 270 : 90 : 0, pts);
    }

    @Override
    void convertPcmToAac(byte[] aacFrame, int size) {
        encoderPcmToAac(aacFrame);
    }

    /**
     * this method call by jni
     * @param data h264 stream
     * @param pts pts
     * @param isKeyFrame is key frame
     */
    private void onH264EncodedData(byte[] data, int pts, boolean isKeyFrame) {
        muxerH264(data, pts);
    }

    /**
     * this method call by jni
     * @param data aac data
     */
    private void onAacEncodeData(byte[] data) {
        long pts = System.nanoTime() / 1000 - mPresentTimeUs;

//        byte[] aac = new byte[data.length + 7];
//        addADTStoPacket(aac, aac.length);
//        System.arraycopy(data, 0, aac, 7, data.length);
        muxerAac(data, (int) pts);
    }

    /**
     * set libx264 params fps
     * @param fps
     */
    private native void setEncoderFps(int fps);
    private native void setEncoderGop(int gop);
    private native void setEncoderBitrate(int bitrate);
    private native void setEncoderPreset(String preset);
    private native boolean openH264Encoder();
    private native void closeH264Encoder();
    private native boolean openAacEncoder(int channels, int sampleRate, int bitrate);
    private native int encoderPcmToAac(byte[] pcm);
    private native void closeAacEncoder();

    /**
     *
     * @param yuvFrame
     * @param width
     * @param height
     * @param flip
     * @param rotate
     * @param pts
     * @return
     */
    protected native int NV21EncodeToH264(byte[] yuvFrame, int width, int height, boolean flip, int rotate, long pts);
}
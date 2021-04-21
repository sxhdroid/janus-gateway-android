package org.appspot.apprtc;

public interface CodecType {

    @interface Video {
        String VIDEO_CODEC_VP8 = "VP8";
        String VIDEO_CODEC_VP9 = "VP9";
        String VIDEO_CODEC_H264 = "H264";
        String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
        String VIDEO_CODEC_H264_HIGH = "H264 High";
    }

    @interface Audio {
        String AUDIO_CODEC_OPUS = "opus";
        String AUDIO_CODEC_ISAC = "ISAC";
    }
}

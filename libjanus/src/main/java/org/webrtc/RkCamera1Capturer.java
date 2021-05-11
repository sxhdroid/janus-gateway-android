package org.webrtc;


import android.content.Context;

public class RkCamera1Capturer extends CameraCapturer {

    private final boolean captureToTexture;

    public RkCamera1Capturer(String cameraName, CameraEventsHandler eventsHandler, boolean captureToTexture) {
        super(cameraName, eventsHandler, new RkCamera1Enumerator(captureToTexture));
        this.captureToTexture = captureToTexture;
    }

    protected void createCameraSession(CameraSession.CreateSessionCallback createSessionCallback, CameraSession.Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, String cameraName, int width, int height, int frameRate) {
        RkCamera1Session.create(createSessionCallback, events, this.captureToTexture, applicationContext, surfaceTextureHelper, RkCamera1Enumerator.getCameraIndex(cameraName), width, height, frameRate);
    }
}

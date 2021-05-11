/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import org.appspot.apprtc.PeerConnectionClient2.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient2.PeerConnectionParameters;
import org.jetbrains.annotations.NotNull;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SurfaceViewRenderer;

import java.math.BigInteger;
import java.util.Vector;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

/**
 * Activity for JanusVideoRoom setup, call waiting and call view.
 */
public class VideoRoom2Activity extends Activity implements CallFragment.OnCallEvents {
    private static final String TAG = "VideoRoomActivity";

    public static final String EXTRA_SERVERADDR = "org.appspot.apprtc.ROOMURL";
    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_USERID = "org.appspot.apprtc.USERID";
    public static final String EXTRA_URLPARAMETERS = "org.appspot.apprtc.URLPARAMETERS";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
    public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
    public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
    public static final String EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED =
            "org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE";
    public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
    public static final String EXTRA_DISABLE_WEBRTC_AGC_AND_HPF =
            "org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL";
    public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
    public static final String EXTRA_USE_VALUES_FROM_INTENT =
            "org.appspot.apprtc.USE_VALUES_FROM_INTENT";
    public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
    public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
    public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
    public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
    public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
    public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
    public static final String EXTRA_ID = "org.appspot.apprtc.ID";
    public static final String EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG";
    public static final String EXTRA_USE_LEGACY_AUDIO_DEVICE =
            "org.appspot.apprtc.USE_LEGACY_AUDIO_DEVICE";

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;

    private static final int maxVideoRoomUsers = 5;

    private final int[] surfaceViewViewId = new int[] {
            R.id.fullscreen_video_view,
            R.id.pip_video_view3,
            R.id.pip_video_view2,
            R.id.pip_video_view1,
            R.id.pip_video_view0};

    private boolean isBackCamera = false;

    private JanusClient janusClient = null;

    private Toast logToast;
    @Nullable
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled = false;

    // Controls
    private CallFragment callFragment;
    //user info
    private String roomUrl;
    private long roomId;
    private String userId;

    private final Vector<SurfaceViewRenderer> surfaceViewRenderers = new Vector<>();
    private final Vector<BigInteger> positionVector = new Vector<>();

    private BigInteger localHandleId = BigInteger.ZERO;

    @Override
    // TODO(bugs.webrtc.org/8580): LayoutParams.FLAG_TURN_SCREEN_ON and
    // LayoutParams.FLAG_SHOW_WHEN_LOCKED are deprecated.
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_SHOW_WHEN_LOCKED | LayoutParams.FLAG_TURN_SCREEN_ON);
//        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_call_video);

        iceConnected = false;
        // Create UI controls.
        callFragment = new CallFragment();

        final Intent intent = getIntent();
        final EglBase eglBase = EglBase.create();

        for(int i = 0; i < maxVideoRoomUsers ; i++ ) {
            positionVector.add(BigInteger.ZERO);
            SurfaceViewRenderer renderer = findViewById(surfaceViewViewId[i]);
            surfaceViewRenderers.add(renderer);

            renderer.init(eglBase.getEglBaseContext(), null);
            renderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
            if(i == 0) {
                renderer.setOnClickListener(view -> toggleCallControlFragmentVisibility());
            } else {
                renderer.setZOrderMediaOverlay(true);
                renderer.setEnableHardwareScaler(true /* enabled */);
            }
        }

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        // Get Intent parameters.
        roomUrl = intent.getDataString();
        roomId = intent.getLongExtra(EXTRA_ROOMID, 0);
        Log.d(TAG, "Room ID: " + roomId);
        if (roomId == 0) {
            logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        userId = intent.getStringExtra(EXTRA_USERID);

        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

        int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
        int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);

        screencaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            videoWidth = displayMetrics.widthPixels;
            videoHeight = displayMetrics.heightPixels;
        }
        DataChannelParameters dataChannelParameters = null;
        if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
            dataChannelParameters = new DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
                    intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
        }
        peerConnectionParameters =
                new PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true), loopback,
                        tracing, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                        intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
                        intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
                        intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
                        intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false),
                        intent.getBooleanExtra(EXTRA_USE_LEGACY_AUDIO_DEVICE, false), dataChannelParameters);
        Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

        // Create connection parameters.
        String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);
        //add log here
        Log.i("CallActivity","roomUri="+roomUrl);
        Log.i("CallActivity","roomid="+roomId);
        Log.i("CallActivity","urlParameters="+urlParameters);
        //just hack it here

        // Send intent arguments to fragments.
        callFragment.setArguments(intent.getExtras());
        // Activate call and HUD fragments and start the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.commit();

        janusClient = new JanusClient.Builder(this)
                .setEGlBase(eglBase)
                .setVideoWidth(videoWidth)
                .setVideoHeight(videoHeight)
                .setVideoFps(30)
                .setVideoMaxBitrate(8000)
                .setAudioStartBitrate(44)
                .builder();
        janusClient.setOnLiveCallback(new JanusClient.OnLiveCallback() {
            @Override
            public void onLocalRender(@NotNull BigInteger handleId) {
                VideoRoom2Activity.this.onLocalRender(handleId);
            }

            @Override
            public void onRemoteRender(@NotNull BigInteger handleId) {
                VideoRoom2Activity.this.onRemoteRender(handleId);
            }

            @Override
            public void onLeftRoom(@NotNull BigInteger handleId) {
                VideoRoom2Activity.this.onLeftInternal(handleId);
            }

            @Override
            public void onError(@NotNull String errorMessage) {

            }
        });

        if (screencaptureEnabled) {
            startScreenCapture();
        } else {
            startCall();
        }
    }

    @TargetApi(17)
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @TargetApi(21)
    private void startScreenCapture() {
        if (janusClient != null) {
            janusClient.startScreenCapture(CAPTURE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            if (janusClient != null) {
                janusClient.updateMediaProjectionResult(data);
            }
            startCall();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (janusClient != null && !screencaptureEnabled) {
            janusClient.stopVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Video is not paused for screencapture. See onPause.
        if (janusClient != null && !screencaptureEnabled) {
            janusClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        disconnect(true);
        janusClient.release();
        janusClient = null;
        if (logToast != null) {
            logToast.cancel();
        }
        release();
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect(false);
    }

    @Override
    public void onCameraSwitch() {
        if (janusClient != null) {
            janusClient.switchCamera();
            int index = positionVector.indexOf(localHandleId);
            if(isBackCamera) {
                isBackCamera = false;
                setRendererMirror(index);
            } else {
                removeRendererMirror(index);
                isBackCamera = true;
            }
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        surfaceViewRenderers.get(0).setScalingType(scalingType);
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (janusClient != null) {
            janusClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public boolean onToggleMic() {
        if (janusClient != null) {
            micEnabled = !micEnabled;
            janusClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
        } else {
            ft.hide(callFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void startCall() {
        if (janusClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        janusClient.startCall(roomUrl, roomId, userId);
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (janusClient == null) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        //PeerConnectionClient2.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        //setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect(Boolean stopCapture) {
        if (janusClient != null) {
            janusClient.disconnect(stopCapture);
        }
        if (iceConnected) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }

//        for(SurfaceViewRenderer renderer : surfaceViewRenderers) {
//            if (renderer == null)  continue;
//            renderer.clearImage();
//            renderer.setMirror(false);
//            renderer.setVisibility(View.INVISIBLE);
//        }
//        surfaceViewRenderers.clear();

//        finish();
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void swappedFeedToFullscreen(int pipIndex) {
        SurfaceViewRenderer renderer = surfaceViewRenderers.get(pipIndex);

        BigInteger id = positionVector.get(pipIndex);
        janusClient.setVideoRender(id, surfaceViewRenderers.get(0));
        if (positionVector.get(0) == BigInteger.ZERO) {
            renderer.setBackground(null);
            renderer.setVisibility(View.INVISIBLE);
            removeClickListener(pipIndex);
        } else {
            janusClient.setVideoRender(positionVector.get(0), renderer);
        }

        if(id == localHandleId) {
            removeRendererMirror(pipIndex);
            setRendererMirror(0);
        }

        if(positionVector.get(0) == localHandleId) {
            removeRendererMirror(0);
            setRendererMirror(pipIndex);
        }

        positionVector.set(pipIndex, positionVector.get(0));
        positionVector.set(0, id);
    }

    private void onRemoteRender(final BigInteger handleId) {
        for(int i = 0; i < maxVideoRoomUsers; i++) {
            if(positionVector.get(i) == BigInteger.ZERO) {
                positionVector.set(i, handleId);
                SurfaceViewRenderer renderer = surfaceViewRenderers.get(i);
                if(i != 0) renderer.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.border, null));
                renderer.setVisibility(View.VISIBLE);
                janusClient.setVideoRender(handleId, renderer);
                setClickListener(i);
                return;
            }
        }

        Log.d(TAG, "Not enough surfaceView to render the remote stream. handle id is " + handleId);
    }

    private void setRendererMirror(int index) {
        if(isBackCamera) return;

        surfaceViewRenderers.get(index).setMirror(true);
    }

    private void removeRendererMirror(int index) {
        if(isBackCamera) return;

        surfaceViewRenderers.get(index).setMirror(false);
    }

    private void onLocalRender(final BigInteger handleId) { //fixme: localrender is lost, and remoterenders are reach to number of max render
        localHandleId = handleId;
        for(int i = 1; i < maxVideoRoomUsers; i++) {
            if(positionVector.get(i) == BigInteger.ZERO) {
                positionVector.set(i, handleId);

                SurfaceViewRenderer renderer = surfaceViewRenderers.get(i);
                renderer.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.border, null));
                renderer.setVisibility(View.VISIBLE);
                janusClient.setVideoRender(handleId, renderer);
                setRendererMirror(i);
                setClickListener(i);
                return;
            }
        }
    }

    private void setClickListener(final int index) {
        if(index == 0) return;

        surfaceViewRenderers.get(index).setOnClickListener(view -> swappedFeedToFullscreen(index));
    }

    private void removeClickListener(final int index) {
        if(index == 0) return;
        surfaceViewRenderers.get(index).setOnClickListener(null);
    }

    private void onLeftInternal(final BigInteger handleId){
        if(handleId == localHandleId) {
            disconnect(true);
            return;
        }
        for(int index = 0; index < maxVideoRoomUsers; index++) {

            if(positionVector.get(index) != handleId) continue;

            while(index < maxVideoRoomUsers - 1) {
                int step = index == 0 && positionVector.get(index + 1) == localHandleId ? 2 : 1;
                if(positionVector.get(index + step) == BigInteger.ZERO) break;

                janusClient.setVideoRender(positionVector.get(index + step), surfaceViewRenderers.get(index));
                if(positionVector.get(index + step) == localHandleId) {
                    removeRendererMirror(index + step);
                    setRendererMirror(index);
                }
                positionVector.set(index, positionVector.get(index + step));
                index += step;
            }
            janusClient.setVideoRender(handleId, null);
            janusClient.dispose(handleId);
            removeClickListener(index);

            SurfaceViewRenderer renderer = surfaceViewRenderers.get(index);
            renderer.setBackground(null);
            if(index == 0) {
                renderer.clearImage();
            } else {
                renderer.setVisibility(View.INVISIBLE);
            }
            positionVector.set(index, BigInteger.ZERO);
            break;
        }
    }

    private void release() {
        for (SurfaceViewRenderer renderer: surfaceViewRenderers) {
            renderer.clearImage();
            renderer.release();
        }
    }
}


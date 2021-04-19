package org.appspot.apprtc

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import org.appspot.apprtc.janus.JanusCommon.JanusConnectionParameters
import org.appspot.apprtc.janus.JanusRTCEvents2
import org.appspot.apprtc.janus.JanusUtils
import org.json.JSONObject
import org.webrtc.*
import java.io.IOException
import java.math.BigInteger

/**
 * 实现连接Janus进行会议、直播
 *
 * @author ym@163.com
 * @date   4/15/21 3:15 PM
 */
class LiveHelper(private val context: Activity) {

    private val tag = "LiveHelper"

    private var peerConnectionClient: PeerConnectionClient2? = null
    private var videoRoomClient: VideoRoomClient? = null
    private var audioManager: AppRTCAudioManager? = null

    private var mediaProjectionPermissionResultData: Intent? = null

    private var callStartedTimeMs: Long = 0
    private var iceConnected = false

    var onLiveCallback: OnLiveCallback? = null

    init {
        initVideoRoom()
        initAudioManager()
    }

    private fun initVideoRoom() {
        // Create connection client.Use videoRoomClient to connect to Janus Webrtc Gateway.
        videoRoomClient = VideoRoomClient(object : JanusRTCEvents2 {
            override fun onPublisherJoined(handleId: BigInteger) {
                context.runOnUiThread { onPublisherJoinedInternal(handleId) }
            }

            override fun onRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
                context.runOnUiThread { onRemoteJsepInternal(handleId, jsep) }
            }

            override fun onLeft(handleId: BigInteger) {
                context.runOnUiThread { onLiveCallback?.onLeftRoom(handleId) }
            }

            override fun onNotification(notificationMessage: String?) {}

            override fun onChannelClose() {}

            override fun onChannelError(errorMessage: String?) {}
        })
    }

    private fun initPeerConnection(eglBase: EglBase, peerConnectionParameters: PeerConnectionClient2.PeerConnectionParameters,
                 options: PeerConnectionFactory.Options?, config: LiveConfig? = null) {
        // Create peer connection client.
        peerConnectionClient = PeerConnectionClient2(
                context.applicationContext, eglBase, peerConnectionParameters, object : PeerConnectionClient2.PeerConnectionEvents {
            /*
             * Send local peer connection SDP and ICE candidates to remote party.
             * All callbacks are invoked from peer connection client looper thread and
             * are routed to UI thread.
             */
            override fun onLocalDescription(handleId: BigInteger, sdp: SessionDescription) {
                val delta = System.currentTimeMillis() - callStartedTimeMs
                context.runOnUiThread {
                    Log.i(tag, "Sending " + sdp.type + ", delay=" + delta + "ms")
                    if (sdp.type == SessionDescription.Type.OFFER) {
                        videoRoomClient?.publisherCreateOffer(handleId, sdp)
                    } else {
                        videoRoomClient?.subscriberCreateAnswer(handleId, sdp)
                    }
                }
            }

            override fun onIceCandidate(handleId: BigInteger, candidate: IceCandidate?) {
                Log.d(tag, "========onIceCandidate=======")
                context.runOnUiThread {
                    if (candidate != null) {
                        videoRoomClient?.trickleCandidate(handleId, candidate)
                    } else {
                        videoRoomClient?.trickleCandidateComplete(handleId)
                    }
                }
            }

            override fun onIceCandidatesRemoved(handleId: BigInteger, candidates: Array<out IceCandidate>?) {}

            override fun onIceConnected(handleId: BigInteger) {
                val delta = System.currentTimeMillis() - callStartedTimeMs
                context.runOnUiThread {
                    Log.i(tag, "ICE connected, delay=" + delta + "ms")
                    iceConnected = true
                }
            }

            override fun onIceDisconnected(handleId: BigInteger) {
                context.runOnUiThread {
                    Log.i(tag, "ICE disconnected $handleId")
                    iceConnected = false
                    //disconnect();
                }
            }

            override fun onPeerConnectionClosed(handleId: BigInteger?) {}

            override fun onPeerConnectionStatsReady(handleId: BigInteger?, reports: Array<out StatsReport>) {}

            override fun onPeerConnectionError(handleId: BigInteger?, description: String) {
                reportError(description)
            }

            override fun onLocalRender(handleId: BigInteger) {
                context.runOnUiThread { onLiveCallback?.onLocalRender(handleId) }
            }

            override fun onRemoteRender(handleId: BigInteger) {
                context.runOnUiThread { onLiveCallback?.onRemoteRender(handleId) }
            }
        })
        peerConnectionClient!!.createPeerConnectionFactory(options ?: PeerConnectionFactory.Options())
        peerConnectionClient!!.setVideoCapturer(createVideoCapturer(config ?: LiveConfig()))
    }

    private fun initAudioManager() {
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(context.applicationContext)
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(tag, "Starting the audio manager...")
        audioManager!!.start { audioDevice, availableAudioDevices ->
            // This method will be called each time the number of available audio
            // devices has changed.
            onAudioManagerDevicesChanged(audioDevice, availableAudioDevices)
        }
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private fun onAudioManagerDevicesChanged(device: AppRTCAudioManager.AudioDevice,
                                             availableDevices: Set<AppRTCAudioManager.AudioDevice>) {
        Log.d(tag, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device)
        // TODO(henrika): add callback handler.
    }

    private fun createVideoCapturer(config: LiveConfig): VideoCapturer? {
        val videoCapturer: VideoCapturer?
        val videoFileAsCamera = config.videoFileAsCamera
        videoCapturer = if (videoFileAsCamera != null) {
            try {
                FileVideoCapturer(videoFileAsCamera)
            } catch (e: IOException) {
                reportError("Failed to open video file for emulated camera")
                return null
            }
        } else if (config.screenCaptureEnabled) {
            return createScreenCapturer()
        } else if (config.useCamera2) {
            Logging.d(tag, "Creating capturer using camera2 API.")
            createCameraCapturer(Camera2Enumerator(context))
        } else {
            Logging.d(tag, "Creating capturer using camera1 API.")
            createCameraCapturer(Camera1Enumerator(config.captureToTexture))
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera")
            return null
        }
        return videoCapturer
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    @TargetApi(21)
    private fun createScreenCapturer(): VideoCapturer? {
        if (mediaProjectionPermissionResultData == null) {
            reportError("User didn't give permission to capture the screen.")
            return null
        }
        return ScreenCapturerAndroid(mediaProjectionPermissionResultData, object : MediaProjection.Callback() {
            override fun onStop() {
                reportError("User revoked permission to capture the screen.")
            }
        })
    }

    private fun reportError(description: String) {
        context.runOnUiThread {
            disconnectWithErrorMessage(description)
        }
    }

    private fun disconnectWithErrorMessage(errorMessage: String) {
        Log.e(tag, "disconnectWithErrorMessage: $errorMessage")
        context.runOnUiThread { onLiveCallback?.onError(errorMessage) }
    }

    private fun onPublisherJoinedInternal(handleId: BigInteger) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        Log.i(tag, "Creating peer connection, delay=" + delta + "ms")
        peerConnectionClient?.createPeerConnection(handleId)
        Log.i(tag, "Creating OFFER...")
        // Create offer. Offer SDP will be sent to answering client in
        // PeerConnectionEvents.onLocalDescription event.
        peerConnectionClient?.createOffer(handleId)
    }

    private fun onRemoteJsepInternal(handleId: BigInteger, jsep: JSONObject) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        Log.i(tag, "onPublisherRemoteJsepInternal, delay=" + delta + "ms")
        val sessionDescription = JanusUtils.convertJsonToSdp(jsep)
        if (sessionDescription.type == SessionDescription.Type.ANSWER) {
            peerConnectionClient?.setRemoteDescription(handleId, sessionDescription)
            Log.i(tag, "Creating ANSWER...")
        } else if (sessionDescription.type == SessionDescription.Type.OFFER) {
            peerConnectionClient?.subscriberHandleRemoteJsep(handleId, sessionDescription)
        }
    }

    @JvmOverloads
    fun initLive(eglBase: EglBase, peerConnectionParameters: PeerConnectionClient2.PeerConnectionParameters,
                           options: PeerConnectionFactory.Options?, config: LiveConfig? = null) {
        initVideoRoom()
        initAudioManager()
        initPeerConnection(eglBase, peerConnectionParameters, options, config)
    }

    /**
     * 获取截屏请求返回后更新
     */
    fun updateMediaProjectionResult(mediaProjection: Intent) {
        mediaProjectionPermissionResultData = mediaProjection
    }

    @TargetApi(21)
    fun startScreenCapture(requestCode: Int) {
        val mediaProjectionManager = context.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        context.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), requestCode)
    }

    fun startCall(roomUrl: String, roomId: Long, userId: String?, maxVideoRoomUsers: Int) {
        callStartedTimeMs = System.currentTimeMillis()
        val connectionParameters = JanusConnectionParameters(roomUrl, roomId, userId, maxVideoRoomUsers)

        // Start room connection.
        videoRoomClient?.connectToServer(connectionParameters)
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    fun disconnect() {
        videoRoomClient?.disconnectFromServer()
        peerConnectionClient?.close()
        audioManager?.stop()
    }

    fun stopVideoSource() {
        peerConnectionClient?.stopVideoSource()
    }

    fun startVideoSource() {
        peerConnectionClient?.startVideoSource()
    }

    fun switchCamera() {
        peerConnectionClient?.switchCamera()
    }

    fun changeCaptureFormat(width: Int, height: Int, frameRate: Int) {
        peerConnectionClient?.changeCaptureFormat(width, height, frameRate)
    }

    fun setAudioEnabled(enabled: Boolean) {
        peerConnectionClient?.setAudioEnabled(enabled)
    }

    fun setVideoRender(handleId: BigInteger, videoRender: SurfaceViewRenderer?) {
        peerConnectionClient?.setVideoRender(handleId, videoRender)
    }

    fun dispose(handleId: BigInteger) {
        peerConnectionClient?.dispose(handleId)
    }

    fun release() {
        videoRoomClient = null
        peerConnectionClient = null
        audioManager = null
        onLiveCallback = null
        mediaProjectionPermissionResultData = null
    }

    /**
     * 直播配置 VideoSource
     *
     * 四种推流数据源设置判断优先级为：文件直播推流 -> 截屏推流 -> Camera2 -> Camera
     * <br/>默认使用 Camera API，不添加 addCallbackBuffer 的方式设置推流 VideoSource
     */
    class LiveConfig {
        /** 使用Camera API时有效。是否使用 addCallbackBuffer 回调，true不使用，false使用 */
        var captureToTexture: Boolean = true
        /** 是否使用 Camera2 API */
        var useCamera2: Boolean = false
        /** 是否是截屏推流，true：使用截屏推流，false，不使用 */
        var screenCaptureEnabled: Boolean = false
        /** 是否是文件推流，若为文件推流，文件地址不为空 */
        var videoFileAsCamera: String? = null
    }

    interface OnLiveCallback {
        fun onLocalRender(handleId: BigInteger)
        fun onRemoteRender(handleId: BigInteger)
        fun onLeftRoom(handleId: BigInteger)
        fun onError(errorMessage: String)
    }
}
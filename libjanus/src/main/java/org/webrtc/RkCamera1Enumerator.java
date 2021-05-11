package org.webrtc;

import android.hardware.Camera;

import java.util.ArrayList;

import androidx.annotation.Nullable;

/**
 * 解决rk3399无摄像头时，无法通过Camera1 API开启HDMI输入问题
 */
@SuppressWarnings("deprecation")
public class RkCamera1Enumerator extends Camera1Enumerator {

    private static final String TAG = "RkCamera1Enumerator";

    private final boolean captureToTexture;

    public RkCamera1Enumerator() {
        this(true);
    }

    public RkCamera1Enumerator(boolean captureToTexture) {
        super(captureToTexture);
        this.captureToTexture = captureToTexture;
    }

    @Override
    public CameraVideoCapturer createCapturer(String deviceName, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        return new RkCamera1Capturer(deviceName, eventsHandler, this.captureToTexture);
    }

    @Override
    public boolean isFrontFacing(String deviceName) {
        if (Camera.getNumberOfCameras() == 0) {
            return false;
        }
        return super.isFrontFacing(deviceName);
    }

    // Returns device names that can be used to create a new VideoCapturerAndroid.
    @Override
    public String[] getDeviceNames() {
        ArrayList<String> namesList = new ArrayList<>();
        int cameraNumber = Camera.getNumberOfCameras();
        if (cameraNumber == 0) {
            namesList.add(getDeviceName(0));
        } else {
            for (int i = 0; i < cameraNumber; ++i) {
                String name = getDeviceName(i);
                namesList.add(name);
                Logging.d(TAG, "Index: " + i + ". " + name);
            }
        }
        String[] namesArray = new String[namesList.size()];
        return namesList.toArray(namesArray);
    }

    private static @Nullable
    Camera.CameraInfo getCameraInfo(int index) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        try {
            Camera.getCameraInfo(index, info);
        } catch (Exception e) {
//      Logging.e(TAG, "getCameraInfo failed on index " + index, e);
            return null;
        }
        return info;
    }

    // Returns the camera index for camera with name |deviceName|, or throws IllegalArgumentException
    // if no such camera can be found.
    static int getCameraIndex(String deviceName) {
        Logging.d(TAG, "getCameraIndex: " + deviceName);
        for (int i = 0; i < android.hardware.Camera.getNumberOfCameras(); ++i) {
            if (deviceName.equals(getDeviceName(i))) {
                return i;
            }
        }
        return 0;
//    throw new IllegalArgumentException("No such camera: " + deviceName);
    }

    // Returns the name of the camera with camera index.
    static String getDeviceName(int index) {
        Camera.CameraInfo info = getCameraInfo(index);
        if (info == null) {
            // rk3399 无摄像头，返回默认后置摄像头配置
            return "Camera 0" + ", Facing back" + ", Orientation 0";
        }

        String facing =
                (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) ? "front" : "back";
        return "Camera " + index + ", Facing " + facing + ", Orientation " + info.orientation;
    }
}

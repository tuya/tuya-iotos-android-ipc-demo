package com.tuya.ai.ipcsdkdemo.video;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.tuya.smart.aiipc.ipc_sdk.api.Common;
import com.tuya.smart.aiipc.ipc_sdk.api.IParamConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.service.IPCServiceManager;
import com.tuya.smart.aiipc.netconfig.ConfigProvider;

import java.io.IOException;
import java.util.List;

public class VideoCapture {

    private Camera mCamera;
    private byte[] pixelBuffer;
    VideoCodec mCodec;
    private SurfaceTexture mSurfaceTexture;

    private int mChannel;

    public VideoCapture(int channel) {

        pixelBuffer = new byte[1280 * 720 * 3 / 2];
        mCodec = new VideoCodec(mChannel = channel);

    }
    public void startVideoCapture() {
        startPreview();
        mCodec.startCodec();
    }

    private void  openCamera() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private void startPreview() {
//        openCamera();
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        Camera.Parameters p = mCamera.getParameters();
        //根据自己的设置更改
        p.setPreviewFormat(ImageFormat.NV21);
//        p.setPreviewFormat(ImageFormat.YV12);
        p.setPreviewFpsRange(30000, 30000);
        p.setPreviewSize(1280, 720);

        Log.d("Preview", "ccc startPreview1111 ");

        try {
            if (ConfigProvider.getConfig(ConfigProvider.QR_OUTPUT) instanceof SurfaceHolder) {
                mCamera.setPreviewDisplay((SurfaceHolder) ConfigProvider.getConfig(ConfigProvider.QR_OUTPUT));
                ConfigProvider.setConfig(ConfigProvider.QR_OUTPUT, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        IParamConfigManager configManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_PARAM_SERVICE);
        int configRate = configManager.getInt(mChannel, Common.ParamKey.KEY_VIDEO_FRAME_RATE);

//        Optional<int[]> first = p.getSupportedPreviewFpsRange().stream()
//                .filter(ints -> ints.length == 2 && ints[0] == ints[1] && ints[0] == configRate * 1000).findFirst();
//
//        if (first.isPresent()) {
//            p.setPreviewFpsRange(first.get()[0], first.get()[0]);
//        } else {
//            p.setPreviewFpsRange(24000, 24000);
//        }

        List<int[]> supported = p.getSupportedPreviewFpsRange();
        boolean isFound = false;
        for (int[] fps : supported){
            if (fps.length == 2 && fps[0] == fps[1] && fps[0] == configRate * 1000) {
                isFound = true;
                break;
            }
        }

        if (isFound){
            p.setPreviewFpsRange(configRate * 1000, configRate * 1000);
        }else{
            p.setPreviewFpsRange(supported.get(supported.size()-1)[0], supported.get(supported.size()-1)[1]);
        }


//        p.setPreviewFpsRange(30000, 30000);
//        for (int[] ints : p.getSupportedPreviewFpsRange()) {
//            Log.w("Preview", "range");
//            for (int i : ints) {
//                Log.w("Preview", "range: " + i);
//            }
//        }

        mCamera.setParameters(p);
        mSurfaceTexture = new SurfaceTexture(0);

//        try {
//            mCamera.setPreviewTexture(mSurfaceTexture);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        mCamera.addCallbackBuffer(pixelBuffer);

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
//                Log.d("Video", "onPreviewFrame: ");
                //编码
                byte[] pixelData = new byte[1280 * 720 * 3 / 2];
                System.arraycopy(data, 0, pixelData, 0, data.length);
                mCodec.encodeH264(pixelData);
                camera.addCallbackBuffer(pixelBuffer);
            }
        });
        mCamera.startPreview();
        Log.d("Preview", "ccc startPreview2222 ");

    }
}

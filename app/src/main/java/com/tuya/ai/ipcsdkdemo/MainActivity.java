package com.tuya.ai.ipcsdkdemo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.tuya.ai.ipcsdkdemo.audio.FileAudioCapture;
import com.tuya.ai.ipcsdkdemo.video.H264FileVideoCapture;
import com.tuya.ai.ipcsdkdemo.video.VideoCapture;
import com.tuya.smart.aiipc.base.permission.PermissionUtil;
import com.tuya.smart.aiipc.ipc_sdk.IPCSDK;
import com.tuya.smart.aiipc.ipc_sdk.api.Common;
import com.tuya.smart.aiipc.ipc_sdk.api.IDeviceManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IFeatureManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IMediaTransManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IMqttProcessManager;
import com.tuya.smart.aiipc.ipc_sdk.api.INetConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IParamConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.callback.NetConfigCallback;
import com.tuya.smart.aiipc.ipc_sdk.service.IPCServiceManager;
import com.tuya.smart.aiipc.netconfig.ConfigProvider;
import com.tuya.smart.aiipc.netconfig.mqtt.TuyaNetConfig;
import com.tuya.smart.aiipc.trans.TransJNIInterface;

import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IPC_DEMO";

    SurfaceView surfaceView;

    H264FileVideoCapture h264FileMainVideoCapture;

    VideoCapture videoCapture;

    FileAudioCapture fileAudioCapture;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface);
        mHandler = new Handler();

        findViewById(R.id.reset).setOnClickListener(v -> IPCServiceManager.getInstance().reset());

        findViewById(R.id.start_record).setOnClickListener(v -> TransJNIInterface.getInstance().startLocalStorage());

        findViewById(R.id.stop_record).setOnClickListener(v -> TransJNIInterface.getInstance().stopLocalStorage());

        findViewById(R.id.call).setOnClickListener(v -> {

            IDeviceManager iDeviceManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.DEVICE_SERVICE);
            // check register status
            int regStat = iDeviceManager.getRegisterStatus();
            Log.d(TAG, "ccc getting qrcode, register status: " + regStat);
            if (regStat != 2) {
                // get short url for qrcode
                String code = iDeviceManager.getQrCode("168");
                Log.d(TAG, "ccc qrcode: " + code);
            }

            /*
            IMediaTransManager mediaTransManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);

            try {
                InputStream fileStream = getAssets().open("leijun.jpeg");

                byte[] buffer = new byte[2048];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                byte[] file = output.toByteArray();
                mediaTransManager.sendDoorBellCallForPress(file, Common.NOTIFICATION_CONTENT_TYPE_E.NOTIFICATION_CONTENT_JPEG);

            } catch (IOException e) {
                e.printStackTrace();
            }
*/
        });

        PermissionUtil.check(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        }, this::initSurface);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        IPCSDK.closeWriteLog();
    }

    private void initSurface() {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                initSDK();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }

    private void initSDK() {
        IPCSDK.initSDK(this);
//        IPCSDK.openWriteLog(this, "/sdcard/tuya_log/ipc", 3);
        LoadParamConfig();

        INetConfigManager iNetConfigManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.NET_CONFIG_SERVICE);

        iNetConfigManager.config("QR_OUTPUT", surfaceView.getHolder());

        String pid = BuildConfig.PID;
        String uuid = BuildConfig.UUID;
        String authkey = BuildConfig.AUTHOR_KEY;

        iNetConfigManager.setPID(pid);
        iNetConfigManager.setUserId(uuid);
        iNetConfigManager.setAuthorKey(authkey);

        TuyaNetConfig.setDebug(true);

        // Note: network must be ok before enable mqtt active
        ConfigProvider.enableMQTT(true);

        IPCServiceManager.getInstance().setResetHandler(isHardward -> {

            if (mHandler != null) {
                mHandler.postDelayed(() -> {
                    //restart
                    Intent mStartActivity = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (mStartActivity != null) {
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId
                                , mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        Runtime.getRuntime().exit(0);
                    }

                }, 1500);
            }
        });

        NetConfigCallback netConfigCallback = new NetConfigCallback() {

            @Override
            public void configOver(boolean first, String token) {
                Log.d(TAG, "configOver: token: " + token);
                IMediaTransManager transManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);
                IMqttProcessManager mqttProcessManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MQTT_SERVICE);
                IMediaTransManager mediaTransManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);
                IFeatureManager featureManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.FEATURE_SERVICE);

                mqttProcessManager.setMqttStatusChangedCallback(status -> Log.w("onMqttStatus", status + ""));

                IDeviceManager iDeviceManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.DEVICE_SERVICE);
                // set region
                iDeviceManager.setRegion(IDeviceManager.IPCRegion.REGION_CN);

                int ret = transManager.initTransSDK(token, "/data/data/com.tuya.ai.ipcsdkdemo/files/ipc", "/data/data/com.tuya.ai.ipcsdkdemo/files/ipc", pid, uuid, authkey);
                Log.d(TAG, "initTransSDK ret is " + ret);
                featureManager.initDoorBellFeatureEnv();

                runOnUiThread(() -> findViewById(R.id.call).setEnabled(true));


//                int regStat = iDeviceManager.getRegisterStatus();
//                Log.d(TAG, "ccc getting qrcode, register status: " + regStat);
//                if (regStat != 2) {
//                    String code = iDeviceManager.getQrCode(null);
//                    Log.d(TAG, "111 ccc qrcode: " + code);
//                }

                //  start push media
                transManager.startMultiMediaTrans(5);

//                h264FileMainVideoCapture = new H264FileVideoCapture(MainActivity.this, "test.h264");
//                h264FileMainVideoCapture.startVideoCapture(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN);

                // video stream from camera
                videoCapture = new VideoCapture(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN);
                videoCapture.startVideoCapture();

                // audio stream from local file
                fileAudioCapture = new FileAudioCapture(MainActivity.this);
                fileAudioCapture.startFileCapture();

                mediaTransManager.setDoorBellCallStatusCallback(status -> {

                    Log.d(TAG, "doorbell back: " + status);

                });

//                mediaTransManager.addAudioTalkCallback(bytes -> {
//                    Log.d(TAG, "audio callback: " + bytes.length);
//                });

                syncTimeZone();
            }

            @Override
            public void startConfig() {
                Log.d(TAG, "startConfig: ");
            }

            @Override
            public void recConfigInfo() {
                Log.d(TAG, "recConfigInfo: ");
            }

            @Override
            public void onNetConnectFailed(int i, String s) {

            }

            @Override
            public void onNetPrepareFailed(int i, String s) {

            }
        };

        iNetConfigManager.configNetInfo(netConfigCallback);

    }

    private void LoadParamConfig() {
        IParamConfigManager configManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_PARAM_SERVICE);

        configManager.setInt(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN, Common.ParamKey.KEY_VIDEO_WIDTH, 1280);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN, Common.ParamKey.KEY_VIDEO_HEIGHT, 720);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN, Common.ParamKey.KEY_VIDEO_FRAME_RATE, 24);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN, Common.ParamKey.KEY_VIDEO_I_FRAME_INTERVAL, 2);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN, Common.ParamKey.KEY_VIDEO_BIT_RATE, 1024000);

        configManager.setInt(Common.ChannelIndex.E_CHANNEL_AUDIO, Common.ParamKey.KEY_AUDIO_CHANNEL_NUM, 1);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_AUDIO, Common.ParamKey.KEY_AUDIO_SAMPLE_RATE, 8000);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_AUDIO, Common.ParamKey.KEY_AUDIO_SAMPLE_BIT, 16);
        configManager.setInt(Common.ChannelIndex.E_CHANNEL_AUDIO, Common.ParamKey.KEY_AUDIO_FRAME_RATE, 25);
    }

    private static void syncTimeZone() {
        int rawOffset = TransJNIInterface.getInstance().getAppTimezoneBySecond();
        String[] availableIDs = TimeZone.getAvailableIDs(rawOffset * 1000);
        if (availableIDs.length > 0) {
            android.util.Log.d(TAG, "syncTimeZone: " + rawOffset + " , " + availableIDs[0] + " ,  ");
        }
    }
}

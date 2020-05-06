package com.tuya.ai.ipcsdkdemo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.tuya.ai.ipcsdkdemo.audio.FileAudioCapture;
import com.tuya.ai.ipcsdkdemo.video.H264FileVideoCapture;
import com.tuya.ai.ipcsdkdemo.video.VideoCapture;
import com.tuya.smart.aiipc.base.permission.PermissionUtil;
import com.tuya.smart.aiipc.ipc_sdk.IPCSDK;
import com.tuya.smart.aiipc.ipc_sdk.api.Common;
import com.tuya.smart.aiipc.ipc_sdk.api.IFeatureManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IMediaTransManager;
import com.tuya.smart.aiipc.ipc_sdk.api.INetConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IParamConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.service.IPCServiceManager;
import com.tuya.smart.aiipc.netconfig.ConfigProvider;
import com.tuya.smart.aiipc.trans.TransJNIInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

import com.tuya.smart.aiipc.ipc_sdk.api.IMqttProcessManager;
import com.tuya.smart.aiipc.netconfig.mqtt.TuyaNetConfig;

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

        findViewById(R.id.call).setOnClickListener(v -> {
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
        });

        PermissionUtil.check(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECORD_AUDIO
        }, this::initSDK);

    }

    private void initSDK() {
        IPCSDK.initSDK(this);
        LoadParamConfig();

        INetConfigManager iNetConfigManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.NET_CONFIG_SERVICE);

        iNetConfigManager.config("QR_OUTPUT", surfaceView.getHolder());

        iNetConfigManager.setAuthorKey("1ePAjvcKIiHjuwnPZWOJWeEqKGiHbUYw");
        iNetConfigManager.setUserId("tuyaea1abe53672ce6c1");
        iNetConfigManager.setPID("dy5s9aaspstm5qqd");

        TuyaNetConfig.setDebug(true);

        ConfigProvider.enableMQTT(false);

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

        INetConfigManager.NetConfigCallback netConfigCallback = new INetConfigManager.NetConfigCallback() {

            @Override
            public void configOver(boolean first, String token) {
                Log.d(TAG, "configOver: token" + token);
                IMediaTransManager transManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);
                IMqttProcessManager mqttProcessManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MQTT_SERVICE);
                IMediaTransManager mediaTransManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);
                IFeatureManager featureManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.FEATURE_SERVICE);

                mqttProcessManager.setMqttStatusChangedCallback(status -> Log.w("onMqttStatus", status + ""));

                transManager.initTransSDK(token, "/sdcard/", "/sdcard/", "dy5s9aaspstm5qqd", "tuyaea1abe53672ce6c1", "1ePAjvcKIiHjuwnPZWOJWeEqKGiHbUYw");

                featureManager.initDoorBellFeatureEnv();

                runOnUiThread(() -> findViewById(R.id.call).setEnabled(true));

                //推流
                transManager.startMultiMediaTrans();

//                h264FileMainVideoCapture = new H264FileVideoCapture(MainActivity.this, "test.h264");
//                h264FileMainVideoCapture.startVideoCapture(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN);

                //视频流（相机）
                videoCapture = new VideoCapture(Common.ChannelIndex.E_CHANNEL_VIDEO_MAIN);
                videoCapture.startVideoCapture();

                //音频流（本地文件）
                fileAudioCapture = new FileAudioCapture(MainActivity.this);
                fileAudioCapture.startFileCapture();

                mediaTransManager.setDoorBellCallStatusCallback(status -> {
                    /**
                     * 门铃呼叫报警接听状态
                     * status = -1 未知状态
                     * status = 0 接听
                     * status = 1 挂断
                     * status = 2 通话中心跳
                     * {@link Common.DoorBellCallStatus}
                     * */
                    Log.d(TAG, "doorbell back: " + status);

                });

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

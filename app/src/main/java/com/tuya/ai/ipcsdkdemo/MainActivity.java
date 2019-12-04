package com.tuya.ai.ipcsdkdemo;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.tuya.smart.aiipc.base.permission.PermissionUtil;
import com.tuya.smart.aiipc.ipc_sdk.IPCSDK;
import com.tuya.smart.aiipc.ipc_sdk.api.IMediaTransManager;
import com.tuya.smart.aiipc.ipc_sdk.api.IMqttProcessManager;
import com.tuya.smart.aiipc.ipc_sdk.api.INetConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.service.IPCServiceManager;
import com.tuya.smart.aiipc.netconfig.mqtt.TuyaNetConfig;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IPC_DEMO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionUtil.check(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK
        }, this::initSDK);

    }

    private void initSDK() {
        IPCSDK.initSDK(this);

        INetConfigManager iNetConfigManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.NET_CONFIG_SERVICE);

        iNetConfigManager.setAuthorKey("");
        iNetConfigManager.setUserId("");

        TuyaNetConfig.setDebug(true);

        INetConfigManager.NetConfigCallback netConfigCallback = new INetConfigManager.NetConfigCallback() {
            @Override
            public void onNetConnectFailed() {
                Log.w(TAG, "retry");

                iNetConfigManager.configNetInfo(this);
            }

            @Override
            public void configOver(boolean first, String token) {
                Log.d(TAG, "configOver: token" + token);
                IMediaTransManager transManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);
                IMqttProcessManager mqttProcessManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MQTT_SERVICE);

                mqttProcessManager.setMqttStatusChangedCallback(status -> Log.w("onMqttStatus", status + ""));

                transManager.initIoTSDK(token, "", "", "");
                //如果还需要接入IPC，使用initTransSDK 替代 initIoTSDK
//                transManager.initTransSDK(token, "/sdcard/", "/sdcard/", "", "", "");
            }

            @Override
            public void startConfig() {
                Log.d(TAG, "startConfig: ");
            }

            @Override
            public void recConfigInfo() {
                Log.d(TAG, "recConfigInfo: ");
            }
        };

        iNetConfigManager.configNetInfo(netConfigCallback);

    }
}

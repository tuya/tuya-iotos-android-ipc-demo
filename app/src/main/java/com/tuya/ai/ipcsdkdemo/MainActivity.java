package com.tuya.ai.ipcsdkdemo;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.tuya.smart.aiipc.base.permission.PermissionUtil;
import com.tuya.smart.aiipc.ipc_sdk.IPCSDK;
import com.tuya.smart.aiipc.ipc_sdk.api.IMediaTransManager;
import com.tuya.smart.aiipc.ipc_sdk.api.INetConfigManager;
import com.tuya.smart.aiipc.ipc_sdk.service.IPCServiceManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

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
        iNetConfigManager.enableMQTT();

        iNetConfigManager.configNetInfo(new INetConfigManager.NetConfigCallback() {
            @Override
            public void onNetConnectFailed() {

            }

            @Override
            public void configOver(boolean first, String token) {
                Log.d(TAG, "configOver: token" + token);
                IMediaTransManager transManager = IPCServiceManager.getInstance().getService(IPCServiceManager.IPCService.MEDIA_TRANS_SERVICE);

                transManager.setMqttStatusChangedCallback(status -> Log.w("onMqttStatus", status + ""));

                transManager.initIoTSDK(token, "", "", "");
            }

            @Override
            public void startConfig() {
                Log.d(TAG, "startConfig: ");
            }

            @Override
            public void recConfigInfo() {
                Log.d(TAG, "recConfigInfo: ");
            }
        });
    }
}

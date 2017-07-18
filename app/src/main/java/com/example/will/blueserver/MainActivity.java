package com.example.will.blueserver;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.will.peripherallib.BlePeripheral;
import com.example.will.peripherallib.BlePeripheralCallback;
import com.example.will.peripherallib.CharacteristicConfig;
import com.example.will.peripherallib.ServiceConfig;
import com.example.will.peripherallib.common.LocalBluetoothCtrl;

import static com.example.will.blueserver.IWashUUID.*;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
/**
 * android ble 被扫描端(从端)
 * 1.权限问题
 * 2.开启蓝牙
 * 3.开启广播
 * 4........................
 * 5.关闭广播
 * 6.关闭蓝牙
 * */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private Button sendButton;
    private TextView infoText, deviceText;
    private EditText sendEdit;
    private LocalBluetoothCtrl localBluetoothCtrl = LocalBluetoothCtrl.getInstance();
    private BlePeripheral blePeripheral = BlePeripheral.getInstance();

    private static final int RC_LOCATION_PERM = 100;

    private AdvertiseCallback advertiseCallback;
    private AdvertiseData advertiseData;
    private AdvertiseSettings advertiseSettings;
    private boolean hasInitBle = false;

    private String TAG="MainActivity.class";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    infoText.setText("");
                    Log.i(TAG, "handleMessage: getConnectDevice is "+blePeripheral.getConnectDevice());
                    if (blePeripheral.getConnectDevice() != null) {
                        setTitle("连接到" + blePeripheral.getConnectDevice().getAddress());
                        //连接成功 可以发送消息
                    } else {
                        setTitle("断开连接");
                    }
                    break;
                case 1:
//                    infoText.append((String)msg.obj + "\n");
                    break;
                case 2:
                    infoText.append((String) msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initSendButton();
        initPermission();
//        initBluetooth();

    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void initPermission() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Have permissions, do the thing!
//            Toast.makeText(this, "TODO: Location and Contacts things", Toast.LENGTH_LONG).show();
            Log.v("Permission", "has");
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, "我们需要位置权限",
                    RC_LOCATION_PERM, perms);
        }
    }

    private void initView() {
        sendButton = (Button) findViewById(R.id.send_button);
        infoText = (TextView) findViewById(R.id.info_text);
        infoText.setMovementMethod(new ScrollingMovementMethod());
        deviceText = (TextView) findViewById(R.id.device_text);
        String macAddress = android.provider.Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        deviceText.setText(localBluetoothCtrl.getBluetoothAdapter().getName() + "--" + macAddress);
        sendEdit = (EditText) findViewById(R.id.editText);
    }

    private void initSendButton() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strLine = sendEdit.getText().toString();
                if (blePeripheral.sendDataToRemoteDevice(ser1Uuid, ser1Char1Uuid, blePeripheral.getConnectDevice(), strLine.getBytes())) {
                    infoText.append(strLine + "消息发送成功\n");
                } else {
                    infoText.append("消息发送失败\n");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_ble_item:
                localBluetoothCtrl.enableBle(false);
                return true;
            case R.id.disable_ble_item:
                localBluetoothCtrl.disableBle();
                return true;
            case R.id.start_advertise_item:
                if (localBluetoothCtrl.isBLEEnabled()) {
                    if (!hasInitBle) {
                        initBluetooth();
                        hasInitBle = true;
                    }
                    blePeripheral.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
                } else {
                    localBluetoothCtrl.enableBle(true);
                }
                return true;
            case R.id.stop_advertise_item:
                Toast.makeText(MainActivity.this,"关闭广播并且断开连接",Toast.LENGTH_LONG).show();
                blePeripheral.stopAdvertising(advertiseCallback);
                Message m0 = new Message();
                m0.what = 0;
                handler.sendMessage(m0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d("main", "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    private void initBluetooth() {
        blePeripheral.addServices(new ServiceConfig(IWashUUID.ser1Uuid), new CharacteristicConfig[]{new CharacteristicConfig(IWashUUID.ser1Char1Uuid), new CharacteristicConfig(ser1Char2Uuid)});
//        blePeripheral.addServices(new ServiceConfig(ser2Uuid), null);
//        blePeripheral.addServices(new ServiceConfig(ser3Uuid), new CharacteristicConfig[]{new CharacteristicConfig(ser3Char1Uuid)});
//        blePeripheral.addServices(new ServiceConfig(ser4Uuid), new CharacteristicConfig[]{new CharacteristicConfig(ser4CharUuid)});
//        blePeripheral.addServices(new ServiceConfig(ser5Uuid), new CharacteristicConfig[]{new CharacteristicConfig(ser5Char1Uuid)});
//        blePeripheral.addServices(new ServiceConfig(ser6Uuid), new CharacteristicConfig[]{
//                new CharacteristicConfig(ser6Char1Uuid),
//                new CharacteristicConfig(ser6Char2Uuid),
//                new CharacteristicConfig(ser6Char3Uuid),
//                new CharacteristicConfig(ser6Char4Uuid),
//                new CharacteristicConfig(ser6Char5Uuid)
//        });

        blePeripheral.setBlePeripheralCallback(new BlePeripheralCallback() {
            @Override// central 主动连接成功 and 主动断开 ;Peripheral主动断开连接
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.v("设备" + device.getAddress(), "连接成功");
                    Message m0 = new Message();
                    m0.what = 0;
                    handler.sendMessage(m0);
                } else {
                    Log.v("设备" + device.getAddress(), "断开连接");
                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.v("设备", "添加服务");
            }

            @Override// central主动读characteristic,刺激Peripheral
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.v("接收请求", "central主动读characteristic");
            }

            @Override// 可能 central主动写characteristic,报异常了
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.v("接收请求", "写characteristic");
                String strLine = new String(value);
                strLine = device.getAddress() + strLine + "\n";
                Message m = new Message();
                m.what = 2;
                m.obj = strLine;
                handler.sendMessage(m);
            }

            //需要添加 周边被central 订阅的方法返回监听

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.v("接收请求", "onDescriptorReadRequest");
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.v("接收请求", "onDescriptorWriteRequest");
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.v("接收请求", "onExecuteWrite");
            }

            @Override //Peripheral主动发送notifation,central没有订阅就接收不到消息
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.v("接收请求", "Peripheral主动发送notifation");
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                Log.v("接收请求", "onMtuChanged");
            }
        });
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.v("广播", "成功");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.v("广播失败", "参数：" + errorCode);
            }
        };

        advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(IWashUUID.ser1Uuid))
                .build();
        advertiseSettings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTimeout(20)
                .build();

    }

}

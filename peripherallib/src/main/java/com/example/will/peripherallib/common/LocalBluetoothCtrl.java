package com.example.will.peripherallib.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Set;

import static android.content.ContentValues.TAG;


/**
 * @author will4906.
 * @Time 2016/12/10.
 */

public class LocalBluetoothCtrl {


    private String advName = "BLEAdvName";
    private final int MESSAGE_SET_ADVERTISE_NAME = 11086;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_ADVERTISE_NAME:
                    if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                        Log.i("LocalBluetoothCtrl", mBluetoothAdapter.getName());
                        if (!advName.equalsIgnoreCase(mBluetoothAdapter.getName())) {
                            boolean setNameResult = mBluetoothAdapter.setName(advName);
                            Log.i("LocalBluetoothCtrl", "set name result is " + setNameResult);
                        }
                    }
                    Message message = mHandler.obtainMessage();
                    message.what = MESSAGE_SET_ADVERTISE_NAME;
                    mHandler.sendEmptyMessageDelayed(message.what, 1000);
                    break;

            }
            return false;
        }
    });

    /*以下代码运用了设计模式中的单例模式，懒汉方式，静态内部类实现，既保证了线程安全又保证了资源不被损耗*/
    private static class LocalBluetoothCtrlHolder {
        private static final LocalBluetoothCtrl INSTANCE = new LocalBluetoothCtrl();
    }

    public static final LocalBluetoothCtrl getInstance() {
        return LocalBluetoothCtrlHolder.INSTANCE;
    }

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;

    private LocalBluetoothCtrl() {
        if (isBLESupported()) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            setAdvNameByHandler();
        }
    }

    //使用handler设置广播名称
    private void setAdvNameByHandler() {
        Message message = mHandler.obtainMessage();
        message.what = MESSAGE_SET_ADVERTISE_NAME;
        mHandler.sendEmptyMessageDelayed(message.what, 200);
    }

    final String sNewName = "Syntactics";

    /**
     * @param sNewName 准备设置广播的名字
     *                 Android手机set蓝牙广播名称,因为缓存问题,导致失败的处理
     *  可以试一下
     */
    private void setAdvNameByRunnable(final String sNewName) {
        final BluetoothAdapter myBTAdapter = BluetoothAdapter.getDefaultAdapter();
        final long lTimeToGiveUp_ms = System.currentTimeMillis() + 10000;
        if (myBTAdapter != null) {
            String sOldName = myBTAdapter.getName();
            if (!sOldName.equalsIgnoreCase(sNewName)) {
                final Handler myTimerHandler = new Handler();
                if (!myBTAdapter.isEnabled()) {
                    myBTAdapter.enable();//打开蓝牙
                }
                //enable 0.5s之后 设置名字
                myTimerHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (myBTAdapter.isEnabled()) {//如果蓝牙已经打开
                                    myBTAdapter.setName(sNewName);//设置新名字
                                    if (sNewName.equalsIgnoreCase(myBTAdapter.getName()))//如果设置成功 为什么adapter disable??
                                    {
                                        Log.i(TAG, "Updated BT Name to " + myBTAdapter.getName());
                                        myBTAdapter.disable();
                                    }
                                }
                                if (!(sNewName.equalsIgnoreCase(myBTAdapter.getName())) && (System.currentTimeMillis() < lTimeToGiveUp_ms))//如果设置失败,并且在时间范围内 重新调用自身
                                {
                                    myTimerHandler.postDelayed(this, 500);
                                    if (myBTAdapter.isEnabled()) //如果蓝牙可用 没有用不管怎么样都是循环调用自身
                                        Log.i(TAG, "Update BT Name: waiting on BT Enable");//等待BT启用
                                    else
                                        Log.i(TAG, "Update BT Name: waiting for Name (" + sNewName + ") to set in");//等待name设置
                                }
                            }
                        }, 500);
            }
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public BluetoothManager getBluetoothManager() {
        return mBluetoothManager;
    }


    private Context mContext = ContextTool.getContext();

    /**
     * 判断设备是否支持ble功能
     *
     * @return
     */
    public boolean isBLESupported() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 判断安卓设备蓝牙是否已经打开
     *
     * @return 返回蓝牙是否已经开启
     */
    public boolean isBLEEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * 开启蓝牙
     *
     * @param bShowDialog true表示需要提示用户，并经过用户同意，false表示无需经过用户直接开启
     */
    public boolean enableBle(boolean bShowDialog) {
        boolean flag = false;
        if (isBLESupported()) {
            if (bShowDialog) {
                //这种是需要弹出对话框让用户选择是否打开的
                final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(enableIntent);
            } else {
                //这种是不经过用户，直接打开
                return mBluetoothAdapter.enable();
            }
        }
        return flag;
    }

    /**
     * 关闭蓝牙
     */
    public boolean disableBle() {
        boolean flag;
        if (isBLEEnabled()) {
            flag = mBluetoothAdapter.disable();
        } else {
            flag = true;
        }
        return flag;
    }

    /**
     * 获取已连接的设备，很明显可以连接很多台设备
     *
     * @return
     */
    public List<BluetoothDevice> getConnectedDevices(boolean isServer) {
        if (isServer) {
            return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
        } else {
            return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }
    }

    /**
     * 获取已配对的设备
     *
     * @return
     */
    public Set<BluetoothDevice> getBondedDevices() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getBondedDevices();
        } else {
            return null;
        }
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return mBluetoothAdapter.getRemoteDevice(address);
    }

    public BluetoothDevice getRemoteDevice(byte[] address) {
        return mBluetoothAdapter.getRemoteDevice(address);
    }
}

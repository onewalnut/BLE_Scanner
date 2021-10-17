package sysu.imsl.ble_scanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.ParcelUuid;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.*;

import sysu.imsl.ble_scanner.adapter.DeviceAdapter;
import sysu.imsl.ble_scanner.comm.ObserverManager;
import sysu.imsl.ble_scanner.operation.OperationActivity;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import sysu.imsl.ble_scanner.FileUtil;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private BluetoothLeScannerCompat scanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters = new ArrayList<>();

    private Button btn_scan;
    private ListView list_devices;
    private ImageView img_loading;
    private static FileUtil fileUtil;

    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;

    public final static String[] BLE_NAMES = new String[]{"A207-01","A207-02","A207-03","A207-04","A207-05","A207-06","A207-07","A207-08",
            "A207-09","A207-10","A207-11","A207-12","A207-13","A207-14","A207-15","A207-16",
            "A207-17","A207-18","A207-19","A207-20","A207-21","A207-22","A207-23","A207-24",
            "A207-25","A207-26","A207-27","A207-28","A207-29","A207-30","A207-31","A207-32",
            "A207-33","A207-34","A207-35","A207-36","A207-37","A207-38"};

    private Timer timer;
    private static int[] ble_rssi = new int[BLE_NAMES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

    }


    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }


    private void init(){
        scanner = BluetoothLeScannerCompat.getScanner();
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000)
                .setLegacy(false)
                .setUseHardwareBatchingIfSupported(true)
                .build();
        scanFilters.add(new ScanFilter.Builder().setDeviceName(BLE_NAMES[0]).build());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));
        btn_scan.setOnClickListener(this);

        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        fileUtil = new FileUtil();

        mDeviceAdapter = new DeviceAdapter(this);
//        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
//            @Override
//            public void onConnect(BleDevice bleDevice) {
//                if (!BleManager.getInstance().isConnected(bleDevice)) {
//                    BleManager.getInstance().cancelScan();
////                    connect(bleDevice);
//                }
//            }
//
//            @Override
//            public void onDisConnect(final BleDevice bleDevice) {
//                if (BleManager.getInstance().isConnected(bleDevice)) {
//                    BleManager.getInstance().disconnect(bleDevice);
//                }
//            }
//
//            @Override
//            public void onDetail(BleDevice bleDevice) {
//                if (BleManager.getInstance().isConnected(bleDevice)) {
//                    Intent intent = new Intent(MainActivity.this, OperationActivity.class);
//                    intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
//                    startActivity(intent);
//                }
//            }
//        });
        list_devices = (ListView) findViewById(R.id.list_device);
        list_devices.setAdapter(mDeviceAdapter);

        reset(ble_rssi);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
//                    scan_flag = 1;
//                    while(scan_flag == 1) {
//                        checkPermissions();
//                        SystemClock.sleep(2000);
//                    }
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BleManager.getInstance().cancelScan();
                    btn_scan.setText(getString(R.string.start_scan));
                }
                break;

            case R.id.btn_connect:
//                btn_scan.performClick();
//                SystemClock.sleep(2000);
                break;
        }
    }


    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {
        String[] uuids;
        String str_uuid = "" ;//et_uuid.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        String str_name = ""; //et_name.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

//        String BLE_names = "A207-01,A207-02,A207-03,A207-04,A207-05,A207-06,A207-07,A207-08";
//        names = BLE_names.split(",");

        String mac = ""; //et_mac.getText().toString();


        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, BLE_NAMES)   // 只扫描指定广播名的设备，可选
//                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(false)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(0)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));

                if(success){
                    Log.i("onScanStarted", "onScanStarted: success");
                }
                else {
                    Log.i("onScanStarted", "onScanStarted: failed");
                }
                timer = new Timer();
                timer.schedule(new SaveScanRes(), 500, 1000);
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
                Log.i("onLeScan", "onLeScan: " + bleDevice.getName() + "," + bleDevice.getRssi());

                String name = bleDevice.getName();
                if(Arrays.asList(BLE_NAMES).contains(name)){
                    ble_rssi[Integer.parseInt(name.substring(5)) - 1] = bleDevice.getRssi();
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                Log.i("onScanning", "onScanning: "+ bleDevice.getName() + "," + bleDevice.getRssi());
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));

                timer.cancel();

                int rssi[] = new int[BLE_NAMES.length];
                reset(rssi);
                for (BleDevice bleDevice: scanResultList) {
                    Log.i("onScanFinished", "onScanFinished: "+ bleDevice.getName() + "," + bleDevice.getRssi());
                    if(Arrays.asList(BLE_NAMES).contains(bleDevice.getName())) {
//                        Log.i("BLE_device", "" + bleDevice.getName().substring(6));
//                        Log.i("BLE_device", "" + Integer.parseInt(bleDevice.getName().substring(6)));
                        rssi[Integer.parseInt(bleDevice.getName().substring(5)) - 1] = bleDevice.getRssi();
                    }
                }
                String data = Arrays.toString(rssi);

//                String coordinate = Arrays.toString(et_coordinate.getText().toString().split(",|，"));
                String coordinate = " ";

//                fileUtil.saveSensorData("BLEScanData.csv",  coordinate + "\n\n");
                if(scanResultList.size() == 0){
                    Toast.makeText(MainActivity.this, "Fingerprint scan failed", Toast.LENGTH_SHORT).show();
                }
                else{
                    boolean success = fileUtil.saveSensorData("BLE_Fingerprints.csv", data.substring(1, data.length()-1) + "\n");
//                            coordinate.substring(1, coordinate.length()-1) + ", ," + data.substring(1, data.length()-1) + "\n");
                    Log.d("DATA", "onScanFinished: " + data);
                    if(success){
                        Toast.makeText(MainActivity.this, "Fingerprint save successfully", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Fingerprint save failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    private static void reset(int[] rssi){
        for(int i = 0; i < rssi.length; i++)
            rssi[i] = -100;
    }


    public static String GetSystemTime(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
        //获取当前时间
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    static class SaveScanRes extends TimerTask {
        @Override
        public void run() {
            String data = Arrays.toString(ble_rssi);
            boolean success = fileUtil.saveSensorData("BLEScanData.csv", data.substring(1, data.length()-1)
                    + "," + GetSystemTime() + "\n");
            if(success){
                reset(ble_rssi);
            }
            else{
                Log.i("SaveRes", Arrays.toString(ble_rssi));
            }
        }
    }


    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }


    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }


    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
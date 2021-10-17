package sysu.imsl.ble_scanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.clj.fastble.BleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import sysu.imsl.ble_scanner.adapter.DeviceAdapter;

public class FirstFragment extends Fragment {

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
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        init();

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        view.findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "hello", Toast.LENGTH_SHORT).show();
            }
        });
    }



}
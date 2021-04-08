package com.atiqur.bluetoothjoystick;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.atiqur.bluetoothjoystick.utils.HelperUtils;
import com.atiqur.bluetoothjoystick.utils.Joystick;
import com.atiqur.bluetoothjoystick.utils.ToolbarHelper;
import com.atiqur.bluetoothjoystick.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    public boolean allow = false;
    private boolean menuCreated = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    public Bluetooth mBluetooth = null;
    private String deviceAddress = null;
    private static final int REQUEST_ENABLE_BT = 222;
    private static final int ENABLED = 111;
    public int joystick_range = 1024;
    private boolean isConnected = false;
    Joystick jsHorizontal;
    Joystick jsVertical;
    ConstraintLayout layoutJoystickHorizontal;
    ConstraintLayout layoutJoystickVertical;
    private boolean layoutJoystickHorizontalDown;
    private boolean layoutJoystickVerticalDown;
    private final int joystickRange = 1024;
    private Menu menu;
    int speed = 0, direction = 0;
    private Thread mJoystickThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ToolbarHelper.create(binding.toolbar, null, this, "Bot Controller");
        setJoysticksViews();
        setJoystickListeners();
    }

    public void onResume() {
        super.onResume();
        checkBluetooth();
        if (mBluetooth == null) {
            mBluetooth = new Bluetooth(mHandler);
        }
        if (mBluetooth.getState() == 0) {
            mBluetooth.start();
        }
        this.mJoystickThread = new Thread(carControl);
        this.mJoystickThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.textView1.setText(getString(R.string.speed_string));
        binding.textView2.setText(getString(R.string.direction_string));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mJoystickThread.interrupt();
    }

    public void onDestroy() {
        super.onDestroy();
        mJoystickThread.interrupt();
        if (this.mBluetooth != null) {
            this.mBluetooth.stop();
        }
    }

    private final Runnable carControl = new Runnable() {
        private boolean once = false;

        public void run() {
            while (true) {
                if (allow) {
                    once = true;
                    int yValue = jsVertical.getY();
                    int xValue = jsHorizontal.getX();
//                    Log.d("Value", "Y" + yValue + "");
//                    Log.d("Value", "x" + xValue + "");
                    mBluetooth.write(HelperUtils.toBytes('S', yValue, 4));
                    mBluetooth.write(HelperUtils.toBytes('D', xValue, 4));
                } else if (once) {
                    mBluetooth.write(HelperUtils.toBytes('S', joystick_range / 2, 4));
                    mBluetooth.write(HelperUtils.toBytes('D', joystick_range / 2, 4));
                    once = false;
                }
                synchronized (this) {
                    try {
                        wait(22);
                    } catch (Exception e) {
                        return;
                    }
                }
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void setJoystickListeners() {
        layoutJoystickVertical.setOnTouchListener((arg0, arg1) -> {
            jsVertical.drawStick(arg1);
            if (arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                layoutJoystickVerticalDown = true;
                if (!allow && mBluetooth.getState() == 2) {
                    allow = true;
                }
                speed = jsVertical.getY();
                if (speed >= 512) {
                    binding.textView1.setText(String.format("Speed=%s%%", HelperUtils.map(speed, 512, 1024, 0, 100)));
                } else {
                    binding.textView1.setText(String.format("Speed=%s%%", HelperUtils.map(speed, 511, 0, 0, 100)));
                }
            } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                layoutJoystickVerticalDown = false;
                if (!layoutJoystickHorizontalDown) {
                    allow = false;
                }
                binding.textView1.setText(getString(R.string.speed_string));
            }
            if (arg1.getAction() == MotionEvent.ACTION_DOWN && mBluetooth.getState() != 2 && !layoutJoystickHorizontalDown) {
                Toast.makeText(MainActivity.this, "You are not connected to a device", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        layoutJoystickHorizontal.setOnTouchListener((arg0, arg1) -> {
            jsHorizontal.drawStick(arg1);
            if (arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                layoutJoystickHorizontalDown = true;
                if (!allow && mBluetooth.getState() == 2) {
                    allow = true;
                }
                direction = jsHorizontal.getX();
                if (direction >= 512) {
                    binding.textView2.setText(String.format("Direction=%s%%", HelperUtils.map(direction, 512, 1024, 0, 100)));
                } else {
                    binding.textView2.setText(String.format("Direction=%s%%", HelperUtils.map(direction, 511, 0, 0, 100)));
                }
            } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                layoutJoystickHorizontalDown = false;
                if (!layoutJoystickVerticalDown) {
                    allow = false;
                }
                binding.textView2.setText(getString(R.string.direction_string));
            }
            if (arg1.getAction() == MotionEvent.ACTION_DOWN && mBluetooth.getState() != 2 && !layoutJoystickVerticalDown) {
                Toast.makeText(MainActivity.this, "You are not connected to a device", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void setJoysticksViews() {

        //Horizontal joystick
        layoutJoystickHorizontal = binding.layoutJoystickHorizontal;
        jsHorizontal = new Joystick(this, layoutJoystickHorizontal, R.drawable.joystick_middle);
        jsHorizontal.setOffset(110);
        jsHorizontal.setBoundaries(150);
        jsHorizontal.setDirection(jsHorizontal.X_DIRECTION);
        jsHorizontal.drawZeroPos();

        //Vertcal joystick
        layoutJoystickVertical = binding.layoutJoystickVertical;
        jsVertical = new Joystick(this, layoutJoystickVertical, R.drawable.joystick_middle);
        jsVertical.setOffset(110);
        jsVertical.setBoundaries(150);
        jsVertical.setDirection(jsVertical.Y_DIRECTION);
        jsVertical.drawZeroPos();

        layoutJoystickHorizontalDown = false;
        layoutJoystickVerticalDown = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        this.menu = menu;
        menuCreated = true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.connect_scan) {
            if (!mBluetoothAdapter.isEnabled()) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            } else if (!isConnected) {
                startActivityForResult(new Intent(this, PairedActivity.class), ENABLED);
            } else {
                Toast.makeText(this, "You are already connected!", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.menu_disconnect) {
            mBluetooth.disconnect();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ENABLED && resultCode == RESULT_OK) {
            if (mBluetooth.getState() != 0) {
                if (!deviceAddress.equals(data.getExtras().getString(PairedActivity.EXTRA_DEVICE_ADDRESS))) {
                    mBluetooth.stop();
                    mBluetooth = new Bluetooth(mHandler);
                    try {
                        wait(10);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error" + e, Toast.LENGTH_SHORT).show();
                    }
                    connectDevice(data);
                    return;
                }
                return;
            }
            mBluetooth.stop();
            mBluetooth = new Bluetooth(mHandler);
            connectDevice(data);
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth not enabled. Leaving", Toast.LENGTH_SHORT).show();
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectDevice(Intent data) {
        if (mBluetooth != null) {
            mBluetooth.stop();
        }
        String address = data.getExtras().getString(PairedActivity.EXTRA_DEVICE_ADDRESS);
        deviceAddress = address;
        mBluetooth.connect(mBluetoothAdapter.getRemoteDevice(address));
    }

    private void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (msg.arg1 == 0 && menuCreated) {
                    isConnected = false;
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_bluetooth));
                    menu.getItem(0).setShowAsAction(5);
                    menu.getItem(0).setTitle("Connect");
                } else if (msg.arg1 == 1 && menuCreated) {
                    isConnected = false;
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_bluetooth_connecting));
                    menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                } else if (msg.arg1 == 2 && menuCreated) {
                    isConnected = true;
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_bluetooth));
                    menu.getItem(0).setShowAsAction(5);
                    menu.getItem(0).setTitle("Connected");
                }
            } else if (msg.what == 2) {
                Toast.makeText(MainActivity.this, "Connected to " + msg.getData().getString("device_name"), Toast.LENGTH_SHORT).show();
            } else if (msg.what == 3) {
                Toast.makeText(MainActivity.this, msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
            }
        }
    };
}
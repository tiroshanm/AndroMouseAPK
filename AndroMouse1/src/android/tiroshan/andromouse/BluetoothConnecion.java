package android.tiroshan.andromouse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothConnecion extends Activity {
	
	// Layout view
	private TextView mTitle;
	
	// Name of the connected device
    private String mConnectedDeviceName = null;
	private BluetoothAdapter btadapter=null;
	private CommandHandler controller=null;
	//intent request codes
	private static final int REQUEST_BLUETOOTH_ENABLE=2;
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
 // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    

    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        //setContentView(R.layout.activity_bluetooth_connecion);
        //get bluetooth connection
        btadapter=BluetoothAdapter.getDefaultAdapter();
        if(btadapter==null){
        	//bluetooth is not available
        	//wanna show an alert box
        	Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }


    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case CommandHandler.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    break;
                case CommandHandler.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case CommandHandler.STATE_LISTEN:
                case CommandHandler.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    @Override 
    protected void onStart() {
    	super.onStart();
    	if(!btadapter.isEnabled()){
    		Intent BTenableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
    		startActivityForResult(BTenableIntent, REQUEST_BLUETOOTH_ENABLE);
    	}
    	else{
    		if(controller==null)
    			setupCommand();
    	}
	}
    
    private void setupCommand() {
		// Initialize the BluetoothChatService to perform bluetooth connections
    	controller=new CommandHandler(this,handler);
	}
    
    @Override 
    protected void onResume() {
    	super.onResume();
    	if(controller!=null){
    		if(controller.getState()==CommandHandler.STATE_NONE){
    			controller.start();
    		}
    	}
	}
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (controller != null)
			controller.stop();
	}
    
    private void ensureDiscoverable() {
        if (btadapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = btadapter.getRemoteDevice(address);
                // Attempt to connect to the device
                controller.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupCommand();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
        	Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			controller.write(CommandHandler.VOL_UP);
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			controller.write(CommandHandler.VOL_DOWN);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
    
}

package dk.sixped.automaticvolume;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;


import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class MainActivity  extends BlunoLibrary implements SensorEventListener/*implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener*/{
	private Button buttonScan;
	private TextView serialReceivedText;




    SensorManager mSensorManager;
    Sensor mSensor;
	private Context appContext = this;
    private PendingIntent transitionPendingIntent;
    GoogleApiClient mApiClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        onCreateProcess();//onCreate Process by BlunoLibrary



        serialBegin(115200);													//set the Uart Baudrate on BLE chip to 115200

        serialReceivedText=(TextView) findViewById(R.id.serialReveicedText);			//initial the EditText of the sending data


        buttonScan = (Button) findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
        buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});








        // Additions:


        EditText distanceText = findViewById(R.id.distance);
        final Spinner classifySpinner = findViewById(R.id.classifier);

        distanceText.setHint("Set training distance");


        String[] buildings = new String[]{
                "Choose measure",
                "Simple",
                "Logarithmic",
                "Machine Learning"
        };




        // building spinner
        final List<String> buildingList = new ArrayList<>(Arrays.asList(buildings));
        final ArrayAdapter<String> buildingAdapter = new ArrayAdapter<String>(this,R.layout.spinner_item,buildingList){
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        classifySpinner.setAdapter(buildingAdapter);
        classifySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdap = bluetoothManager.getAdapter();


        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

	}

	protected void onResume(){
		super.onResume();
		//System.out.println("BlUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        onPauseProcess();														//onPause Process by BlunoLibrary
    }
	
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();	
        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {											//Four connection state
		case isConnected:
			buttonScan.setText("Connected");
			break;
		case isConnecting:
			buttonScan.setText("Connecting");
			break;
		case isToScan:
			buttonScan.setText("Scan");
			break;
		case isScanning:
			buttonScan.setText("Scanning");
			break;
		case isDisconnecting:
			buttonScan.setText("isDisconnecting");
			break;
		default:
			break;
		}
	}













//    public void startTraining(){
//
//        LibLinearWrapper lib = new LibLinearWrapper();
//        Instance in = new Instance(2);
//
//        Instances instances = new Instances();
//
//    }



	int window, currentAudioLevel, previousAudioMeasure, audioSum;
	final int WINDOW_SIZE = 100;

	@Override
	public void onSerialReceived(String theString) {
        String[] lines = theString.split(System.getProperty("line.separator"));

        //Log.i("Serial Received",  ""+lines.length);
        for(String str : lines){
            String trimmedStr = str.trim();
            if(!trimmedStr.isEmpty()){
                int currentMeasure = Integer.parseInt(trimmedStr);
                audioSum += currentMeasure;
                if(window >= WINDOW_SIZE){
                    audioSum -= previousAudioMeasure;
                }else{
                    window++;
                }
                currentAudioLevel = audioSum/window;
            }
        }

	}

    BluetoothManager bluetoothManager;
    BluetoothAdapter btAdap;
	final int SCAN_INTERVAL = 5000, FADE_INTERVAL = 500;
	Handler loopHandler, volHandler;
	String SPEAKER_ADDRESS = "F0:13:C3:87:B7:AE", BLUNO_ADDRESS = "D0:39:72:C5:3E:E5";
	int speakerRssi, blunoRssi;
	boolean scanning = false, shouldScan;
	LinkedList<Integer> rssiBuffer = new LinkedList<>();
	int fadeVol = -1, currentVol = -1;

    private Runnable scanLoopRunner = new Runnable() {
        @Override
        public void run() {
            //btAdap.cancelDiscovery();
            setDistance();
            btAdap.startDiscovery();
            scanning = true;
            Log.i("Loop", "Running");
            if(shouldScan)  loopHandler.postDelayed(this, SCAN_INTERVAL);
        }
    };

    public void startScanLoop(){
        shouldScan = true;
        Log.i("Loop", "Loop started");
        loopHandler = new Handler();
        loopHandler.postDelayed(scanLoopRunner, SCAN_INTERVAL);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(rssiReceiver, filter);
    }
    public void stopScanLoop(){
        shouldScan = false;
    }

    private final BroadcastReceiver rssiReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String dAddress = device.getAddress();
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                ((ScrollView)serialReceivedText.getParent()).fullScroll(View.FOCUS_DOWN);
                Log.i("RECIEVEC", "rssi found" + dAddress + device.getName());
                if (dAddress.equals(BLUNO_ADDRESS) || dAddress.equals(SPEAKER_ADDRESS)){
                    Log.i("RECIEVEC", dAddress + " RSSI: " + rssi);
                    rssiUpdate(rssi, dAddress);
                }
            }
        }
    };


    public void rssiUpdate(int rssi, String address){
        Log.i("Distance", "Address: " + address + " RSSI: " + rssi);
        //serialReceivedText.append(address +" RSSI:"+ rssi);
        if(address.equals(SPEAKER_ADDRESS) || address.equals(BLUNO_ADDRESS) || address.equals("80:E6:50:06:D5:A1")){
            rssiBuffer.add(rssi);
        }
    }

    public void estimateVolume(){
        fadeVolume(simpleDistance(blunoRssi));
    }

    private Runnable volRunner = new Runnable() {
        @Override
        public void run() {
            currentVol += (fadeVol-currentVol)/5;
            adjustVolume(currentVol);
            if(currentVol < fadeVol)  volHandler.postDelayed(this, FADE_INTERVAL);
        }
    };

    public void fadeVolume(int vol){
        fadeVol = vol;
        volHandler = new Handler();
        volHandler.postDelayed(volRunner, FADE_INTERVAL);
    }

    public void adjustVolume(int volume){
        //serialReceivedText.append("Percent: " + volume);
        if(volume <=100 && volume >= 0){
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float vol = volume*1.0f;
            float percent = vol/100.0f;
            Log.i("Volume", "percent " + percent);
            int seventyVolume = (int) (maxVolume*percent);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
        }
    }

    public int simpleDistance(int rssi){

        int vol = (rssi + 40)*(-1)*2;
        return vol;
    }

    public int logDistance(int rssi){

        double distance = Math.pow( 10, ((-50 - rssi) / 20));

        return 0;
    }

    float pX = 0.0f;
    float pY = 0.0f;
    float pZ = 0.0f;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1] ;
        float z = sensorEvent.values[2];
        if(Math.abs(x-pX) > 2 || Math.abs(y-pY) > 2 || Math.abs(z-pZ) > 2 && !(pX == 0.0f && pY == 0.0f && pZ == 0.0f)){
            //serialReceivedText.append("X: " + x + " Y: " + y + " Z: " + z );
            serialReceivedText.append("Scanning");
            if(!scanning){
                startScanLoop();
            }
        }else{
            //stopScanLoop();
        }
        pX = x;
        pY = y;
        pZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void setDistance(){
        if(rssiBuffer.size()!=0){
            int rssi = 0;
            for(int i : rssiBuffer){
                rssi += i;
            }
            blunoRssi = rssi/rssiBuffer.size();

            //serialReceivedText.append("averaged::::"+blunoRssi+":::::");
            rssiBuffer.clear();
            estimateVolume();
        }
    }

}
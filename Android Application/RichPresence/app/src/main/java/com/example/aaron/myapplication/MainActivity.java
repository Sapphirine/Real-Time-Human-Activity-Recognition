package com.example.aaron.myapplication;

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.common.api.*;

import java.io.*;

import java.util.ArrayList;
import java.util.List;



public class MainActivity extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemSelectedListener {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float[] gravity;
    private long lastUpdate;
    private GoogleApiClient mGoogleApiClient;

    private int currentActivity = -1;

    private float mX = 0;
    private float mY = 0;
    private float mZ = 0;

    String root;
    File myFile;

    private List<coord> list = new ArrayList<coord>();

    private Handler handler = new Handler();

    private String username;

    PendingIntent mActivityRecognitionPendingIntent;

    private class coord {
        long time;
        float x;
        float y;
        float z;

        public coord(float _x, float _y, float _z, long _time) {
            x = _x;
            y = _y;
            z = _z;
            time = _time;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }

        public long getTime() {
            return time;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        root = Environment.getExternalStorageDirectory().toString();
        myFile = new File(root + "/mytextfile.txt");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = getIntent();
        username = i.getStringExtra("username");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setVisibility(View.INVISIBLE);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.activities, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        final Button buttonStart = (Button) findViewById(R.id.button_start);
        final Button buttonStop = (Button) findViewById(R.id.button_stop);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox training = (CheckBox) findViewById(R.id.checkBoxTraining);
                TextView textX = (TextView) findViewById(R.id.textView5);
                textX.setText("Recording!");

                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
                Intent intent = new Intent(MainActivity.this, RecordService.class);
                intent.putExtra("activity", currentActivity);
                intent.putExtra("username", username);
                if (training.isChecked())
                    intent.putExtra("isTrain", true);
                else {
                    intent.putExtra("isTrain", false);

                    Intent intentRecognition = new Intent(MainActivity.this, ActivityRecognitionIntentService.class);

                    mActivityRecognitionPendingIntent = PendingIntent.getService(MainActivity.this, 0, intentRecognition, PendingIntent.FLAG_UPDATE_CURRENT);
                    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, mActivityRecognitionPendingIntent);

                }
                training.setEnabled(false);
                startService(intent);
            }
        });


        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                TextView textX = (TextView) findViewById(R.id.textView5);
                textX.setText("Not Recording!");
                Intent intent = new Intent(MainActivity.this, RecordService.class);
                stopService(intent);
                CheckBox training = (CheckBox) findViewById(R.id.checkBoxTraining);
                if (!training.isChecked()) {
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityRecognitionPendingIntent);
                }
                training.setEnabled(true);
            }
        });
        buttonStop.setEnabled(false);

        CheckBox training = (CheckBox) findViewById(R.id.checkBoxTraining);
        training.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Spinner spinner = (Spinner) findViewById(R.id.spinner);
                if (isChecked)
                    spinner.setVisibility(View.VISIBLE);
                else
                    spinner.setVisibility(View.INVISIBLE);
            }
        });
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

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        switch (parent.getItemAtPosition(pos).toString()) {
            case "Sitting":
                currentActivity = 1;
                break;
            case "Standing":
                currentActivity = 2;
                break;
            case "Running":
                currentActivity = 3;
                break;
            case "Walking":
                currentActivity = 4;
                break;
            case "Upstairs":
                currentActivity = 5;
                break;
            case "Downstairs":
                currentActivity = 6;
                break;
            default:
                currentActivity = -1;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Connect:", "OnConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connect:", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Connect:", "onConnectionFailed");
    }
}

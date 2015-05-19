package com.example.aaron.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aaron on 4/17/15.
 */
public class RecordService extends Service implements SensorEventListener {

    private static final String TAG = "HelloService";
    private boolean isRunning  = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private int currentActivity = -1;
    private boolean isRecording;

    private float mX = 0;
    private float mY = 0;
    private float mZ = 0;

    String root;
    File myFile;

    final int sampleSize = 50;

    private List<coord> list = new ArrayList<coord>();

    private Handler handler = new Handler();

    WifiManager wifiManager;
    WifiManager.WifiLock lock;

    PowerManager.WakeLock mWakeLock;

    String username;

    HttpClient httpClient = new DefaultHttpClient();

    boolean isTrain;

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

    private void updateData() {
        list.add(new coord(mX, mY, mZ, System.currentTimeMillis()));
        if (list.size() == sampleSize) {
            //!!!!!!!!!!!! WRITE TO DATABASE !!!!!!!!!!!!

            if (isTrain) {
                long currentTime = System.currentTimeMillis();
                FileOutputStream fileout = null;
                try {
                    fileout = new FileOutputStream(myFile, true);
                    OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
                    for (coord c : list) {
                        outputWriter.write(currentTime + " " + c.getTime() + " " + c.getX() + " " + c.getY() + " " + c.getZ() + " " + currentActivity + "\n");
                    }
                    outputWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                new task().execute();
            }
            //!!!!!!!!!!!!!! WRITE TO FILE !!!!!!!!!!!!!!!

            list.clear();
        }
    }

    private final Runnable getData = new Runnable() {
        public void run() {
            updateData();
            handler.postDelayed(this, 20);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (isRecording) {
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mX = event.values[0];
                mY = event.values[1];
                mZ = event.values[2];
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        lock.acquire();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);


        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        mWakeLock.acquire();

        isRunning = true;

        root = Environment.getExternalStorageDirectory().toString();


        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);




    }

    public class addUser extends Thread{
        HttpPost httpPost;
        String activity;
        public addUser(String _activity) {
            String url_select = "http://52.6.200.164/addUser.php";
            httpPost = new HttpPost(url_select);
            activity = _activity;
            Log.d("AddUser", "HI");
        }
        public void run() {
            JSONArray data = new JSONArray();
            data.put(username);
            data.put(activity);

            StringEntity entity = null;
            try {
                entity = new StringEntity(data.toString());
                httpPost.setEntity(entity);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                httpResponse.getEntity().consumeContent();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        isRecording = true;
        currentActivity = intent.getIntExtra("activity", -1);
        username = intent.getStringExtra("username");
        isTrain = intent.getBooleanExtra("isTrain", false);

        if (isTrain)
            myFile = new File(root + "/" + username + ".txt");
        else {
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    String activity = intent.getStringExtra("activity");
                    new addUser(activity).start();
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                    new IntentFilter("GoogleUpdate"));
            new addUser("UNKNOWN").start();
        }

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(getData);
            }
        }).start();

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

        isRunning = false;
        isRecording = false;
        handler.removeCallbacks(getData);
        Log.i(TAG, "Service onDestroy");

        lock.release();
        mWakeLock.release();
    }

    class task extends AsyncTask<String, String, Void> {
        InputStream is = null;
        String result = "";
        JSONArray m_array;

        protected task() {
            m_array = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                JSONArray curr = new JSONArray();
                try {
                    curr.put(list.get(i).getX());
                    curr.put(list.get(i).getY());
                    curr.put(list.get(i).getZ());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                m_array.put(curr);
            }
        }

        protected void onPreExecute() {
        }

        public void insert() {
            String url_select = "http://52.6.200.164/insert2.php";

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url_select);

            try {

                JSONArray data = new JSONArray();
                data.put(username);
                data.put(m_array.toString());
                StringEntity entity = new StringEntity(data.toString());

                httpPost.setEntity(entity);

                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                is = httpEntity.getContent();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("log_tag", "Error in http connection " + e.toString());
            }
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                result = sb.toString();

            } catch (Exception e) {
                // TODO: handle exception
                Log.e("log_tag", "Error converting result " + e.toString());
            }
        }

        @Override
        protected Void doInBackground(String... params) {
            insert();
            return null;

        }

        protected void onPostExecute(Void v) {

        }
    }
}

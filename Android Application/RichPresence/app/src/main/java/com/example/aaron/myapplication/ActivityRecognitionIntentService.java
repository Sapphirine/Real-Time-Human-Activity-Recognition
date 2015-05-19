package com.example.aaron.myapplication;

/**
 * Created by Aaron on 2/15/15.
 */

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ActivityRecognitionIntentService extends IntentService {

    int confidence;
    String activity;
    String username;
    HttpClient httpClient = new DefaultHttpClient();

    public ActivityRecognitionIntentService() {
        // Set the label for the service's background thread
        super("ActivityRecognitionIntentService");
    }

    public int getConfidence() {
        return confidence;
    }

    public String getActivity() {
        return activity;
    }

    public class addUser extends Thread{
        HttpPost httpPost;
        public addUser() {
            String url_select = "http://52.6.200.164/addUser.php";
            httpPost = new HttpPost(url_select);
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
    protected void onHandleIntent(Intent intent) {
        //...
        // If the intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the update
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            // Get the confidence % (probability)
            confidence = mostProbableActivity.getConfidence();

            // Get the type
            int activityType = mostProbableActivity.getType();

            switch(activityType) {
                case DetectedActivity.STILL :
                    activity = "STILL";
                    break;
                case DetectedActivity.IN_VEHICLE:
                    activity = "IN VEHICLE";
                    break;
                case DetectedActivity.ON_FOOT:
                    activity = "ON FOOT";
                    break;
                case DetectedActivity.ON_BICYCLE:
                    activity = "ON BICYCLE";
                    break;
                case DetectedActivity.RUNNING:
                    activity = "RUNNING";
                    break;
                case DetectedActivity.TILTING:
                    activity = "TILTING";
                    break;
                case DetectedActivity.WALKING:
                    activity = "WALKING";
                    break;
                default:
                    activity = "UNKNOWN";
                    break;
            }
            Log.d(activity, String.valueOf(confidence));

            Intent i = new Intent("GoogleUpdate");
            i.putExtra("activity", activity);
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
            manager.sendBroadcast(i);

        }
    }
}

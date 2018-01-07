package com.example.awds.mafiawifi.servises;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import static com.example.awds.mafiawifi.activitys.MainActivity.MY_TAG;

public class ServerService extends Service {
    private int state;
    private SharedPreferences preferences;

    public ServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void changeState(int state) {
        if (preferences == null)
            preferences = getSharedPreferences(MY_TAG, MODE_PRIVATE);
        this.state = state;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("state",state);
        editor.commit();
    }
}

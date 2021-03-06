package com.example.awds.mafiawifi.activitys.server;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.activitys.MainActivity;
import com.example.awds.mafiawifi.activitys.client.ClientGameActivity;
import com.example.awds.mafiawifi.activitys.client.ServerSearchingActivity;
import com.example.awds.mafiawifi.activitys.client.WaitingForGameStartActivity;
import com.example.awds.mafiawifi.interfaces.Bindable;
import com.example.awds.mafiawifi.servises.MyBinder;
import com.example.awds.mafiawifi.servises.ServerService;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.activitys.MainActivity.MY_TAG;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_MAIN_ACTIVITY;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_CLIENT;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_SERVER;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_SEARCHING_FOR_SERVERS;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_WAITING_FOR_GAME_START;

public class WaitingForPlayersActivity extends AppCompatActivity {

    private ServiceConnection connection;
    private Observable<JSONObject> serviceOutput;
    private PublishSubject<JSONObject> serviceInput;
    private Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(MY_TAG, MODE_PRIVATE);
        switch (preferences.getInt("state", STATE_MAIN_ACTIVITY)) {
            case STATE_MAIN_ACTIVITY:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            case STATE_SEARCHING_FOR_SERVERS:
                startActivity(new Intent(this, ServerSearchingActivity.class));
                finish();
                break;
            case STATE_WAITING_FOR_GAME_START:
                startActivity(new Intent(this, WaitingForGameStartActivity.class));
                finish();
                break;
            case STATE_PLAYING_AS_CLIENT:
                startActivity(new Intent(this, ClientGameActivity.class));
                finish();
                break;
            case STATE_PLAYING_AS_SERVER:
                startActivity(new Intent(this, ServerGameActivity.class));
                finish();
                break;
            default:
                setContentView(R.layout.activity_waiting_for_players);
                context = this;
                connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        serviceInput = PublishSubject.create();
                        Bindable service = (Bindable) ((MyBinder) iBinder).getService();
                        serviceOutput = service.bind(serviceInput);
                        serviceOutput.subscribe(j -> Log.d("awdsawds", "message To Activity " + j.toString()), e -> {
                        }, () -> {
                            Log.d("awdsawds", "service disconnect Message");
                            if (!context.isFinishing())
                                context.finish();
                        });
                        unbindService(connection);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {

                    }
                };
                bindService(new Intent(this, ServerService.class), connection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("awdsawds", "destroyServActivity");
        if (serviceInput != null)
            serviceInput.onComplete();
        super.onDestroy();
    }
}

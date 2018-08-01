package com.example.awds.mafiawifi.activitys.client;

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
import com.example.awds.mafiawifi.activitys.server.ServerGameActivity;
import com.example.awds.mafiawifi.activitys.server.WaitingForPlayersActivity;
import com.example.awds.mafiawifi.interfaces.Bindable;
import com.example.awds.mafiawifi.servises.ClientService;
import com.example.awds.mafiawifi.servises.MyBinder;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.activitys.MainActivity.MY_TAG;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_MAIN_ACTIVITY;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_CLIENT;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_SERVER;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_WAITING_FOR_GAME_START;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_WAITING_FOR_PLAYERS;

public class ServerSearchingActivity extends AppCompatActivity {

    private ServiceConnection connection;
    private Observable<JSONObject> serviceOutput;
    private PublishSubject<JSONObject> serviceInput;
    private Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("awdsawds", "createServActivity");
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(MY_TAG, MODE_PRIVATE);
        switch (preferences.getInt("state", STATE_MAIN_ACTIVITY)) {
            case STATE_MAIN_ACTIVITY:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            case STATE_WAITING_FOR_PLAYERS:
                startActivity(new Intent(this, WaitingForPlayersActivity.class));
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
                setContentView(R.layout.activity_server_serch);
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
                bindService(new Intent(this, ClientService.class), connection, BIND_AUTO_CREATE);
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

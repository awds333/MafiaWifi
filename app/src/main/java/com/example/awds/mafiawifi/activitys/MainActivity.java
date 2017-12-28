package com.example.awds.mafiawifi.activitys;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.dialogs.ExitDialog;
import com.example.awds.mafiawifi.dialogs.NamePickDialog;
import com.example.awds.mafiawifi.dialogs.ServerNamePickDialog;


public class MainActivity extends Activity {
    Button openServer, findServer, back;

    public static final String MY_TAG = "AWDS_TAG";

    public static final int STATE_MAIN_ACTIVITY = 0;
    public static final int STATE_SEARCHING_FOR_SERVERS = 1;
    public static final int STATE_WAITING_FOR_PLAYERS = 2;
    public static final int STATE_WAITING_FOR_GAME_START = 3;
    public static final int STATE_PLAYING_AS_CLIENT = 4;
    public static final int STATE_PLAYING_AS_SERVER = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(MY_TAG, MODE_PRIVATE);
        switch (preferences.getInt("state", STATE_MAIN_ACTIVITY)) {
            case STATE_SEARCHING_FOR_SERVERS:
                break;
            case STATE_WAITING_FOR_PLAYERS:
                break;
            case STATE_WAITING_FOR_GAME_START:
                break;
            case STATE_PLAYING_AS_CLIENT:
                break;
            case STATE_PLAYING_AS_SERVER:
                break;
            default:
                setContentView(R.layout.activity_main);
                openServer = (Button) findViewById(R.id.newserver);
                findServer = (Button) findViewById(R.id.findeserv);
                back = (Button) findViewById(R.id.back);
                findServer.setOnClickListener(v -> {
                    openServer.setClickable(false);
                    findServer.setClickable(false);
                    NamePickDialog dialog = new NamePickDialog();
                    dialog.show(getFragmentManager(), MY_TAG);
                    openServer.setClickable(true);
                    findServer.setClickable(true);
                });
                openServer.setOnClickListener(v -> {
                    openServer.setClickable(false);
                    findServer.setClickable(false);
                    ServerNamePickDialog dialog = new ServerNamePickDialog();
                    dialog.show(getFragmentManager(), MY_TAG);
                    openServer.setClickable(true);
                    findServer.setClickable(true);
                });
                back.setOnClickListener(v -> {
                    back.setClickable(false);
                    onBackPressed();
                    back.setClickable(true);
                });
        }
    }

    @Override
    public void onBackPressed() {
        ExitDialog dialog = new ExitDialog();
        dialog.show(getFragmentManager(), MY_TAG);
    }
}

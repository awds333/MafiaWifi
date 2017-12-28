package com.example.awds.mafiawifi.servises;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.activitys.client.ClientGameActivity;
import com.example.awds.mafiawifi.activitys.client.ServerSearchingActivity;
import com.example.awds.mafiawifi.activitys.client.WaitingForGameStartActivity;
import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.engines.client.ServerSearchingEngine;
import com.example.awds.mafiawifi.netclasses.ClientSocketManager;
import com.example.awds.mafiawifi.receivers.MyReceiver;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.example.awds.mafiawifi.activitys.MainActivity.MY_TAG;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_CLIENT;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_SEARCHING_FOR_SERVERS;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_WAITING_FOR_GAME_START;

public class ClientService extends Service {

    private MyBinder binder = new MyBinder();
    private final int MY_ID = 324;
    private Engine engine;
    private ClientSocketManager socketManager;
    private int state;
    private SharedPreferences preferences;
    private Subject<JSONObject> serviceInput, socketManagerInput, engineInput, activityInput, activityOutput, engineOutput;
    private Observable<JSONObject> serviceOutput, socketManagerOutput;
    private Disposable engineOutputDisposable;

    @Override
    public void onCreate() {
        super.onCreate();
        changeState(STATE_SEARCHING_FOR_SERVERS);

        serviceInput = PublishSubject.create();
        socketManagerInput = PublishSubject.create();
        engineInput = PublishSubject.create();
        engineOutput = PublishSubject.create();
        activityInput = PublishSubject.create();
        activityOutput = PublishSubject.create();
        activityOutput.subscribe()
        engine = new ServerSearchingEngine();
        socketManager = ClientSocketManager.getManager();Disposable

        engineOutputDisposable = engine.bind(engineInput).subscribe((JSONObject j) -> engineOutput.onNext(j),e -> engineOutput.onError(e));
        socketManagerOutput = socketManager.bind(socketManagerInput);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class MyBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    private void updateNotification(@Nullable String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setOngoing(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name));
        Intent notificationIntent = null;
        switch (state) {
            case STATE_SEARCHING_FOR_SERVERS:
                notificationIntent = new Intent(this, ServerSearchingActivity.class);
                builder.setContentText(getString(R.string.searching));
                break;
            case STATE_WAITING_FOR_GAME_START:
                notificationIntent = new Intent(this, WaitingForGameStartActivity.class);
                builder.setContentText(getString(R.string.soon_the_beginning));
                break;
            case STATE_PLAYING_AS_CLIENT:
                notificationIntent = new Intent(this, ClientGameActivity.class);
                builder.setContentText(text);
                break;
        }
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toBroad = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(this, MyReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent);
        builder.addAction(android.R.drawable.btn_dialog, getString(R.string.exit), toBroad);

        Notification notification;
        notification = builder.build();

        startForeground(MY_ID, notification);
    }

    private void changeState(int state) {
        if (preferences == null)
            preferences = getSharedPreferences(MY_TAG, MODE_PRIVATE);
        this.state = state;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("state", state);
        editor.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

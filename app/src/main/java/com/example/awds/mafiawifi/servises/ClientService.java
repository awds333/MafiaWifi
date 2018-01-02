package com.example.awds.mafiawifi.servises;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.activitys.client.ClientGameActivity;
import com.example.awds.mafiawifi.activitys.client.ServerSearchingActivity;
import com.example.awds.mafiawifi.activitys.client.WaitingForGameStartActivity;
import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.engines.client.GameClientEngine;
import com.example.awds.mafiawifi.engines.client.ServerSearchingEngine;
import com.example.awds.mafiawifi.engines.client.WaitingClientEngine;
import com.example.awds.mafiawifi.netclasses.ClientSocketManager;
import com.example.awds.mafiawifi.netclasses.WifiStateListener;
import com.example.awds.mafiawifi.receivers.MyReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.example.awds.mafiawifi.EventTypes.ADDRESS_ACTIVITY;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_ENGINE;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_SERVICE;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_SOCKET_MANAGER;
import static com.example.awds.mafiawifi.EventTypes.TYPE_FINISH;
import static com.example.awds.mafiawifi.EventTypes.TYPE_NEXT_ENGINE;
import static com.example.awds.mafiawifi.EventTypes.TYPE_UPDATE_NOTIFICATION;
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
    private Subject<JSONObject> socketManagerInput, engineInput, activityInput, activityOutput, engineOutput, broadcastInput;
    private Observable<JSONObject> serviceInput, socketManagerOutput, engineInputObservable, wifiStateObservable, activityInputObservable;
    private WifiStateListener wifiStateListener;

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
        broadcastInput = PublishSubject.create();
        engine = new ServerSearchingEngine();
        socketManager = ClientSocketManager.getManager();
        wifiStateListener = new WifiStateListener(this);

        engine.bind(engineInput).subscribe((JSONObject j) -> engineOutput.onNext(j), e -> engineOutput.onError(e));
        socketManagerOutput = socketManager.bind(socketManagerInput);
        wifiStateObservable = wifiStateListener.getObservable();

        Observable.merge(activityOutput.filter(j -> j.getInt("address") % ADDRESS_SERVICE == 0), broadcastInput
                , socketManagerOutput.filter(j -> j.getInt("address") % ADDRESS_SERVICE == 0))
                .subscribe(j -> reactMessage(j), e -> Log.d(MY_TAG, e.toString()));
        Observable.merge(wifiStateObservable, engineOutput.filter(j -> j.getInt("address") % ADDRESS_SOCKET_MANAGER == 0))
                .subscribe(socketManagerInput);
        engineInputObservable = Observable.merge(wifiStateObservable, socketManagerOutput.filter(j -> j.getInt("address") % ADDRESS_ENGINE == 0)
                , activityOutput.filter(j -> j.getInt("address") % ADDRESS_ENGINE == 0));
        engineInputObservable.subscribe(engineInput);
        activityInputObservable = Observable.merge(wifiStateObservable, engineOutput.filter(j -> j.getInt("address") % ADDRESS_ACTIVITY == 0));
        activityInputObservable.subscribe(activityInput);

        wifiStateListener.startListen();
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

    private void reactMessage(JSONObject object) {
        try {
            int type = object.getInt("type");
            if(type % TYPE_FINISH==0){

            } else if(type % TYPE_UPDATE_NOTIFICATION==0){
                updateNotification(object.getString("text"));
            } else if(type % TYPE_NEXT_ENGINE==0){
                changeEngine();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setOngoing(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name));
        Intent notificationIntent = null;
        switch (state) {
            case STATE_SEARCHING_FOR_SERVERS:
                notificationIntent = new Intent(this, ServerSearchingActivity.class);
                builder.setContentText(text);
                break;
            case STATE_WAITING_FOR_GAME_START:
                notificationIntent = new Intent(this, WaitingForGameStartActivity.class);
                builder.setContentText(text);
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

    private void changeEngine(){
        switch (state){
            case STATE_SEARCHING_FOR_SERVERS:
                changeState(STATE_WAITING_FOR_GAME_START);
                engine = new WaitingClientEngine();
                break;
            case STATE_WAITING_FOR_GAME_START:
                changeState(STATE_PLAYING_AS_CLIENT);
                engine = new GameClientEngine();
                break;
            default:
                return;
        }
        engineInput.onComplete();
        engineInput = PublishSubject.create();
        engineInputObservable.subscribe(engineInput);
        engine.bind(engineInput).subscribe((JSONObject j) -> engineOutput.onNext(j), e -> engineOutput.onError(e));
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

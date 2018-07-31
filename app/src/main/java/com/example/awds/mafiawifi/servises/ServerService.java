package com.example.awds.mafiawifi.servises;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.activitys.server.ServerGameActivity;
import com.example.awds.mafiawifi.activitys.server.WaitingForPlayersActivity;
import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.engines.client.ServerSearchingEngine;
import com.example.awds.mafiawifi.engines.server.GameServerEngine;
import com.example.awds.mafiawifi.interfaces.Bindable;
import com.example.awds.mafiawifi.netclasses.ServerSocketsManager;
import com.example.awds.mafiawifi.netclasses.WifiStateListener;
import com.example.awds.mafiawifi.receivers.MyReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.example.awds.mafiawifi.EventTypes.ADDRESS_ACTIVITY;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_ENGINE;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_SERVICE;
import static com.example.awds.mafiawifi.EventTypes.ADDRESS_SOCKET_MANAGER;
import static com.example.awds.mafiawifi.EventTypes.EVENT_FINISH;
import static com.example.awds.mafiawifi.EventTypes.EVENT_NEXT_ENGINE;
import static com.example.awds.mafiawifi.EventTypes.EVENT_UPDATE_NOTIFICATION;
import static com.example.awds.mafiawifi.EventTypes.TYPE_CHANGE_STATE;
import static com.example.awds.mafiawifi.activitys.MainActivity.MY_TAG;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_MAIN_ACTIVITY;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_PLAYING_AS_SERVER;
import static com.example.awds.mafiawifi.activitys.MainActivity.STATE_WAITING_FOR_PLAYERS;

public class ServerService extends Service implements Bindable {
    private int state;
    private final MyBinder binder = new MyBinder(this);
    private SharedPreferences preferences;
    private final int MY_ID = 324;
    private Engine engine;
    private ServerSocketsManager socketsManager;
    private Subject<JSONObject> socketsManagerInput, engineInput, activityInput, activityOutput, engineOutput, broadcastInput, wifiStateOutput;
    private Observable<JSONObject> socketsManagerOutput, engineInputObservable, activityInputObservable;
    private WifiStateListener wifiStateListener;
    private Disposable wifiListenerDisposable;
    private String name;

    public ServerService() {
        changeState(STATE_WAITING_FOR_PLAYERS);
        Log.d("awdsawds", "createServerService");
        engineInput = PublishSubject.create();
        engineOutput = PublishSubject.create();
        activityOutput = PublishSubject.create();
        broadcastInput = PublishSubject.create();
        engine = new ServerSearchingEngine(this);
        socketsManager = ServerSocketsManager.getManager();
        wifiStateListener = new WifiStateListener(this);

        engine.bind(engineInput).subscribe((JSONObject j) -> engineOutput.onNext(j), e -> engineOutput.onError(e));
        socketsManagerOutput = socketsManager.bind(socketsManagerInput);
        wifiStateOutput = PublishSubject.create();

        Observable.merge(activityOutput.filter(j -> j.getInt("address") % ADDRESS_SERVICE == 0), broadcastInput
                , socketsManagerOutput.filter(j -> j.getInt("address") % ADDRESS_SERVICE == 0), engineOutput.filter(j -> j.getInt("address") % ADDRESS_SERVICE == 0))
                .subscribe(j -> reactMessage(j), e -> Log.d(MY_TAG, e.toString()));
        Observable.merge(wifiStateOutput, engineOutput.filter(j -> j.getInt("address") % ADDRESS_SOCKET_MANAGER == 0))
                .subscribe(socketsManagerInput);
        engineInputObservable = Observable.merge(wifiStateOutput, socketsManagerOutput.filter(j -> j.getInt("address") % ADDRESS_ENGINE == 0)
                , activityOutput.filter(j -> j.getInt("address") % ADDRESS_ENGINE == 0));
        engineInputObservable.subscribe(engineInput);
        activityInputObservable = Observable.merge(wifiStateOutput, engineOutput.filter(j -> j.getInt("address") % ADDRESS_ACTIVITY == 0));
        wifiListenerDisposable = wifiStateListener.getObservable().subscribe(wifiStateOutput::onNext, wifiStateOutput::onError, wifiStateOutput::onComplete);

        updateNotification(getString(R.string.searching));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String type = intent.getStringExtra("type");
        Log.d("awdsawds", "service command: type " + type);
        if (type.equals("start"))
            name = intent.getStringExtra("name");
        else if (type.equals("finish")) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", TYPE_CHANGE_STATE);
                object.put("event", EVENT_FINISH);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            broadcastInput.onNext(object);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
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
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        activityInput = PublishSubject.create();
        activityInputObservable.subscribe(activityInput);
        observable.subscribe(j -> activityOutput.onNext(j), e -> activityOutput.onError(e), activityInput::onComplete);
        return activityInput;
    }

    private void updateNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setOngoing(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name));
        Intent notificationIntent = null;
        switch (state) {
            case STATE_WAITING_FOR_PLAYERS:
                notificationIntent = new Intent(this, WaitingForPlayersActivity.class);
                builder.setContentText(text);
                break;
            case STATE_PLAYING_AS_SERVER:
                notificationIntent = new Intent(this, ServerGameActivity.class);
                builder.setContentText(text);
                break;
        }
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent broadIntent = new Intent(this, MyReceiver.class);
        broadIntent.putExtra("type", "server");
        PendingIntent toBroad = PendingIntent.getBroadcast(getApplicationContext(), 0, broadIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent);
        builder.addAction(android.R.drawable.btn_dialog, getString(R.string.exit), toBroad);

        Notification notification;
        notification = builder.build();

        startForeground(MY_ID, notification);
    }

    private void reactMessage(JSONObject object) {
        try {
            int type = object.getInt("type");
            int event = object.getInt("event");
            if (type % TYPE_CHANGE_STATE == 0) {
                if (event % EVENT_FINISH == 0) {
                    stopSelf();
                } else if (event % EVENT_UPDATE_NOTIFICATION == 0) {
                    updateNotification(object.getString("text"));
                } else if (type % EVENT_NEXT_ENGINE == 0) {
                    changeEngine();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void changeEngine() {
        switch (state) {
            case STATE_WAITING_FOR_PLAYERS:
                changeState(STATE_PLAYING_AS_SERVER);
                engine = new GameServerEngine();
                break;
            default:
                return;
        }
        engineInput.onComplete();
        engineInput = PublishSubject.create();
        engineInputObservable.subscribe(engineInput);
        engine.bind(engineInput).subscribe((JSONObject j) -> engineOutput.onNext(j), e -> engineOutput.onError(e));
    }

    @Override
    public void onDestroy() {
        wifiListenerDisposable.dispose();
        engineInput.onComplete();
        socketsManagerInput.onComplete();
        activityInput.onComplete();
        broadcastInput.onComplete();
        Log.d("awdsawds", "destroyService");
        changeState(STATE_MAIN_ACTIVITY);
        super.onDestroy();
        stopForeground(true);
    }
}

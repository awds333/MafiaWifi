package com.example.awds.mafiawifi.netclasses;


import com.example.awds.mafiawifi.EventTypes;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ClientSocketManager {
    private static ClientSocketManager ourInstance;

    private PublishSubject<JSONObject> messages, connectionState;
    private Disposable info;

    private boolean wifiConnected = false;

    public static ClientSocketManager getManager() {
        if (ourInstance == null)
            ourInstance = new ClientSocketManager();
        return ourInstance;
    }

    private ClientSocketManager() {
        messages = PublishSubject.create();
        connectionState = PublishSubject.create();
    }

    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        info = observable.filter((JSONObject event) ->
                event.getInt("type") % EventTypes.TYPE_MESSAGE != 0)
                .observeOn(Schedulers.io())
                .subscribe((JSONObject event) -> {
                    if (event.getInt("type") % EventTypes.TYPE_WIFI_CONNECTION == 0)
                        wifiConnected = event.getBoolean("connected");
                    else if (event.getInt("type") % EventTypes.TYPE_CONNECT_TO_SERVER == 0)
                        connect(event.getString("ip"),event.getInt("port"));
                });
        return Observable.merge(messages.subscribeOn(Schedulers.io())
                , connectionState.subscribeOn(Schedulers.io()));
    }

    private void connect(String ip, int port) {

    }
}

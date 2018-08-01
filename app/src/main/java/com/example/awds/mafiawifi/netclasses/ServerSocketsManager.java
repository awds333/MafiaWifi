package com.example.awds.mafiawifi.netclasses;

import android.util.Log;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class ServerSocketsManager {
    private static ServerSocketsManager ourInstance;
    private PublishSubject outSubject;

    public static ServerSocketsManager getManager() {
        if (ourInstance==null)
            ourInstance = new ServerSocketsManager();
        return ourInstance;
    }

    private ServerSocketsManager() {
        outSubject = PublishSubject.create();
    }

    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        observable.subscribe(i-> Log.d("awdsawds",i.toString())
                ,e->Log.d("awdsawds","ERROR IN SOCKETS MANAGER INPUT " +e.toString()),()->outSubject.onComplete());
        return outSubject;
    }
}

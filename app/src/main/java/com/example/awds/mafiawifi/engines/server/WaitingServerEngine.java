package com.example.awds.mafiawifi.engines.server;


import android.util.Log;

import com.example.awds.mafiawifi.engines.Engine;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class WaitingServerEngine extends Engine {
    private PublishSubject outSubject;

    public WaitingServerEngine() {
        outSubject = PublishSubject.create();
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        observable.subscribe(i-> Log.d("awdsawds",i.toString())
                ,e->Log.d("awdsawds","ERROR IN WAITING ENGINE INPUT " +e.toString()),()->outSubject.onComplete());
        return outSubject;
    }
}

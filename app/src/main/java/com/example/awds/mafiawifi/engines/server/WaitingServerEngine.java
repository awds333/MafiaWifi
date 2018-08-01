package com.example.awds.mafiawifi.engines.server;


import com.example.awds.mafiawifi.engines.Engine;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class WaitingServerEngine extends Engine {
    private PublishSubject outSubject;

    public WaitingServerEngine() {
        outSubject = PublishSubject.create();
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        observable.subscribeOn(Schedulers.io()).subscribe(i -> {
        }, e -> {
        }, () -> {
        });

        return outSubject;
    }
}

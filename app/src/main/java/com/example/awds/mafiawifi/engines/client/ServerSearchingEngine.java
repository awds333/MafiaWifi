package com.example.awds.mafiawifi.engines.client;


import com.example.awds.mafiawifi.engines.Engine;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class ServerSearchingEngine extends Engine {
private PublishSubject<JSONObject> subject;
    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        subject = PublishSubject.create();
        observable.subscribe(c->{},e->{},()->subject.onComplete());
        return subject;
    }
}

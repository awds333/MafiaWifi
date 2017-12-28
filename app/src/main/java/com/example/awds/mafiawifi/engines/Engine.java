package com.example.awds.mafiawifi.engines;

import org.json.JSONObject;

import io.reactivex.Observable;

public abstract class Engine {

    public abstract Observable<JSONObject> bind(Observable<JSONObject> observable);

    public abstract void finish();
}

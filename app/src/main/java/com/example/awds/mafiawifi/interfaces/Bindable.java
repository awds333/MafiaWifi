package com.example.awds.mafiawifi.interfaces;

import org.json.JSONObject;

import io.reactivex.Observable;

public interface Bindable {
    public abstract Observable<JSONObject> bind(Observable<JSONObject> observable);
}

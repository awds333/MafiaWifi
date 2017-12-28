package com.example.awds.mafiawifi.engines.client;


import com.example.awds.mafiawifi.engines.Engine;

import org.json.JSONObject;

import io.reactivex.Observable;

public class GameClientEngine extends Engine {

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        return null;
    }

    @Override
    public void finish() {

    }
}

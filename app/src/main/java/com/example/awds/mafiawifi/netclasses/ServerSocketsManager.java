package com.example.awds.mafiawifi.netclasses;

import org.json.JSONObject;

import io.reactivex.Observable;

public class ServerSocketsManager {
    private static ServerSocketsManager ourInstance;

    public static ServerSocketsManager getManager() {
        if (ourInstance==null)
            ourInstance = new ServerSocketsManager();
        return ourInstance;
    }

    private ServerSocketsManager() {
    }

    public Observable<JSONObject> bind(Observable<JSONObject> observable) {

    }
}

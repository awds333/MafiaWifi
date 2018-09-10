package com.example.awds.mafiawifi.netclasses;

import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_GAME_EVENT;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_MESSAGE;

public class ServerSocketsManager {
    private static ServerSocketsManager ourInstance;
    private PublishSubject outSubject;
    private ReadWriteLock rwLock;
    private ScheduledExecutorService executorService;
    private Observable<JSONObject> gameMessages;

    public static ServerSocketsManager getManager() {
        if (ourInstance == null)
            ourInstance = new ServerSocketsManager();
        return ourInstance;
    }

    private ServerSocketsManager() {
        outSubject = PublishSubject.create();
        rwLock = new ReentrantReadWriteLock();
        executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        observable.filter(message -> message.getInt("type") == TYPE_MESSAGE).subscribe(i -> {

                },
                e -> Log.d("awdsawds", "ERROR IN SOCKETS MANAGER INPUT " + e.toString()),
                () -> {
                    outSubject.onComplete();
                });
        gameMessages = observable.filter(message -> message.getInt("type") == TYPE_GAME_EVENT);
        return outSubject;
    }
}

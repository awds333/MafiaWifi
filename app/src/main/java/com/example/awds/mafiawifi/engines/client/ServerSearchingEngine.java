package com.example.awds.mafiawifi.engines.client;


import android.content.Context;
import android.util.Log;

import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.netclasses.IpPinger;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.EventTypes.TYPE_WIFI_CONNECTION;

public class ServerSearchingEngine extends Engine {
    private PublishSubject<JSONObject> subject;
    private IpPinger ipPinger;
    private Disposable disposable;

    public ServerSearchingEngine(Context context) {
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        subject = PublishSubject.create();
        observable.subscribe(c -> {
        }, e -> {
        }, () -> {
            Log.d("awdsawds", "ServerSearchEngine disconnect");
            subject.onComplete();
            disposable.dispose();
        });
        ipPinger = new IpPinger();
        disposable = ipPinger.startPing(observable
                .filter(event -> event.getInt("type") == TYPE_WIFI_CONNECTION))
                .doOnNext(i->Log.d("awdsawds","Reachable:"+i))
                .subscribe();
        return subject;
    }
}

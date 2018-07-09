package com.example.awds.mafiawifi.netclasses;


import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;


public class IpPinger {

    public IpPinger() {
    }

    public Flowable<String> startPing(Observable<JSONObject> wifiStateObservable) {

        Flowable justIps = Flowable.combineLatest(Flowable.range(1, 255).delay(1,TimeUnit.SECONDS).repeat(),
                wifiStateObservable.toFlowable(BackpressureStrategy.LATEST), (ip, tail) -> tail.getString("ipTail") + ip)
                .flatMap(i -> Flowable.just(i))
                .subscribeOn(Schedulers.io())
                .flatMap(ip -> {
                    if (InetAddress.getByName(ip).isReachable(200))
                        return Flowable.just(ip);
                    return Flowable.empty();
                });
        return justIps;
    }
}

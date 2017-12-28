package com.example.awds.mafiawifi.netclasses;


import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class IpPinger {
    public static Observable startPing(Observable<Integer> wifiStateObservable) {
        Observable justIps = Observable.create(e -> {
            for (int i = 1; i < 256; i++) {
                e.onNext(i);
            }
            e.onComplete();
        });
        return BehaviorSubject.combineLatest(justIps, wifiStateObservable, (ip, connected) -> (int)ip*connected)
                .doOnNext(ip -> {
                    if ((int) ip == 0)
                        throw new IOException();
                }).flatMap(ip -> Observable.just(ip))
                .subscribeOn(Schedulers.io());
    }
}

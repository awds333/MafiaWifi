package com.example.awds.mafiawifi.netclasses;



import org.json.JSONObject;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;


public class IpPinger {

    public Flowable<String> startPing(Observable<JSONObject> wifiStateObservable) {

        Flowable justIps = Flowable.combineLatest(Flowable.range(0, 257).delay(1,TimeUnit.SECONDS).repeat(),
                wifiStateObservable.toFlowable(BackpressureStrategy.LATEST), (ip, tail) -> tail.getString("ipTail") + ip)
                .filter(i->i.charAt(0)!='0')
                .flatMap(i -> Flowable.just(i))
                .subscribeOn(Schedulers.io())
                .flatMap(ip -> {
                    if(ip.substring(ip.lastIndexOf('.')+1).equals("256"))
                        return Flowable.just("256");
                    if (InetAddress.getByName(ip).isReachable(200))
                        return Flowable.just(ip);
                    return Flowable.empty();
                }).map(i->i);
        return justIps;
    }
}

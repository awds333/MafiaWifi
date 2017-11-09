package com.example.awds.mafiawifi.netclasses;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.awds.mafiawifi.netclasses.WifiApUtils.WifiApManager;
import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

public class WifiStateObservable {

    public static final int WIFI_STATE_DISCONNECTED = 0;
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_AP = 2;

    private Context context;
    private Observable observable;
    private ConnectableObservable connectableObservable;

    public WifiStateObservable(Context context) {
        this.context = context;
    }

    public Observable<Integer> getObservable(){
        if(observable==null) {
            Observable<Boolean> wifiObservable = ReactiveNetwork.observeNetworkConnectivity(context)
                    .subscribeOn(Schedulers.io())
                    .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                    .map(connectivity -> {
                        if (connectivity.getState().equals(NetworkInfo.State.CONNECTED))
                            return true;
                        return false;
                    });
            Observable wifiApObservable = Observable.create(subscriber -> {
                WifiApManager wifiApManager = new WifiApManager(context);
                Log.d("awdsawds", "create");
                while (!subscriber.isDisposed()) {
                    subscriber.onNext(wifiApManager.isWifiApEnabled());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {

                    }
                }
            }).subscribeOn(Schedulers.io());
            connectableObservable = Observable.combineLatest(wifiApObservable, wifiObservable, (a, w) -> {
                boolean ap = (Boolean) a;
                if (ap)
                    return WIFI_STATE_AP;
                if (w)
                    return WIFI_STATE_CONNECTED;
                return WIFI_STATE_DISCONNECTED;
            }).publish();
            observable = connectableObservable.refCount();
        }
        return observable;
    }

    public void startListen(){
        if(observable!=null){
            connectableObservable.connect();
        }
    }
}

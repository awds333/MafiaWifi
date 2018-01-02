package com.example.awds.mafiawifi.netclasses;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.awds.mafiawifi.netclasses.WifiApUtils.WifiApManager;
import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import static com.example.awds.mafiawifi.EventTypes.TYPE_WIFI_CONNECTION;

public class WifiStateListener {

    public static final int WIFI_STATE_DISCONNECTED = 0;
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_AP = 2;

    private Context context;
    private Observable observable;
    private ConnectableObservable connectableObservable;
    private WifiApManager wifiApManager;

    public WifiStateListener(Context context) {
        this.context = context;
    }

    public Observable<JSONObject> getObservable() {
        if (observable == null) {
            wifiApManager = new WifiApManager(context);
            Observable<Boolean> wifiObservable = ReactiveNetwork.observeNetworkConnectivity(context)
                    .subscribeOn(Schedulers.io())
                    .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                    .map(connectivity -> {
                        if (connectivity.getState().equals(NetworkInfo.State.CONNECTED))
                            return true;
                        return false;
                    }).distinctUntilChanged();
            Observable wifiApObservable = Observable.interval(300, TimeUnit.MILLISECONDS)
                    .map(t -> wifiApManager.isWifiApEnabled())
                    .subscribeOn(Schedulers.io())
                    .distinctUntilChanged();
            connectableObservable = Observable.combineLatest(wifiApObservable, wifiObservable, (a, w) -> {
                boolean ap = (Boolean) a;
                if (ap)
                    return WIFI_STATE_AP;
                if (w)
                    return WIFI_STATE_CONNECTED;
                return WIFI_STATE_DISCONNECTED;
            }).publish();
            observable = connectableObservable.refCount().map(i -> {
                JSONObject object = new JSONObject();
                object.put("type", TYPE_WIFI_CONNECTION);
                object.put("state", i);
                return object;
            });
        }
        return observable;
    }

    public void startListen() {
        if (observable != null) {
            connectableObservable.connect();
        }
    }
}

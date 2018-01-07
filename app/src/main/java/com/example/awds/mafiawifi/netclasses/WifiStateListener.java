package com.example.awds.mafiawifi.netclasses;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static com.example.awds.mafiawifi.EventTypes.TYPE_WIFI_CONNECTION;

public class WifiStateListener {

    public static final int WIFI_STATE_DISCONNECTED = 0;
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_AP = 2;

    private Context context;
    private Observable observable;
    private WifiManager wifiManager;
    private Method getWifiApState;

    public WifiStateListener(Context context) {
        Log.d("awdsawds","createWifiObservable");
        this.context = context;
    }

    public Observable<JSONObject> getObservable() {
        Log.d("awdsawds","getWifiObservable");
        if (observable == null) {
            Log.d("awdsawds","activateWifiObservable");
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
                getWifiApState = wifiManager.getClass().getMethod("getWifiApState");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            Observable<Boolean> wifiObservable = ReactiveNetwork.observeNetworkConnectivity(context)
                    .subscribeOn(Schedulers.io())
                    .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
                    .map(connectivity -> {
                        if (connectivity.getState().equals(NetworkInfo.State.CONNECTED))
                            return true;
                        return false;
                    }).distinctUntilChanged();
            Observable<Boolean> wifiApObservable = Observable.interval(300, TimeUnit.MILLISECONDS)
                    .map(t -> (Integer) getWifiApState.invoke(wifiManager))
                    .map(c -> c == 13)
                    .subscribeOn(Schedulers.io())
                    .distinctUntilChanged();
            observable = Observable.combineLatest(wifiApObservable, wifiObservable, (a, w) -> {
                if (a)
                    return WIFI_STATE_AP;
                if (w)
                    return WIFI_STATE_CONNECTED;
                return WIFI_STATE_DISCONNECTED;
            }).map(i -> {
                JSONObject object = new JSONObject();
                object.put("type", TYPE_WIFI_CONNECTION);
                object.put("state", i);
                return object;
            });
        }
        return observable;
    }
}

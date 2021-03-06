package com.example.awds.mafiawifi.engines.client;


import android.util.Log;

import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.netclasses.IpPinger;
import com.example.awds.mafiawifi.netclasses.ServerScanner;
import com.example.awds.mafiawifi.structures.PlayerInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.structures.EventTypes.ADDRESS_ACTIVITY;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_ACTIVITY_CONNECTED;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_GAME_STATE_INFO;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_MY_INFO;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_NEW_SERVER_FOUND;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_SERVERS_LIST_UPDATE;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_MESSAGE;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_WIFI_CONNECTION;

public class ServerSearchingEngine extends Engine {
    private PublishSubject outSubject, ipsToScan;
    private IpPinger ipPinger;
    private ServerScanner scanner;
    private Disposable disposable;
    private String name;

    private HashMap<String, JSONObject> serversList;
    private HashMap<String, JSONObject> recentServersList;

    private HashMap<String, PlayerInfo> players;

    public ServerSearchingEngine() {
        outSubject = PublishSubject.create();
        ipsToScan = PublishSubject.create();
        serversList = new HashMap<>();
        recentServersList = new HashMap<>();
        ipPinger = new IpPinger();
        scanner = new ServerScanner();
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        observable.subscribeOn(Schedulers.io()).subscribe(message -> {
            Log.d("awdsawds", "ServerSearchEngine " + message.toString());
            int type = message.getInt("type");
            int event = message.getInt("event");
            if (type == TYPE_MESSAGE) {
                if (event == EVENT_MY_INFO) {
                    name = message.getString("name");
                } else if (event == EVENT_ACTIVITY_CONNECTED) {
                    JSONObject gameStateInfo = new JSONObject();
                    gameStateInfo.put("address", ADDRESS_ACTIVITY);
                    gameStateInfo.put("type", TYPE_MESSAGE);
                    gameStateInfo.put("event", EVENT_GAME_STATE_INFO);

                }
            }
        }, e -> {
            Log.d("awdsawds", "ServerSearchEngine error");
        }, () -> {
            Log.d("awdsawds", "ServerSearchEngine disconnect");
            outSubject.onComplete();
            disposable.dispose();
            ipsToScan.onComplete();
        });
        Observable<JSONObject> servers = scanner.bind(ipsToScan);
        servers.subscribe((JSONObject serverInfo) -> {
            synchronized (serversList) {
                serversList.put(serverInfo.getString("ip"), serverInfo);
                recentServersList.put(serverInfo.getString("ip"), serverInfo);
                JSONObject message = new JSONObject();
                message.put("serverInfo", serverInfo);
                message.put("address", ADDRESS_ACTIVITY);
                message.put("type", TYPE_MESSAGE);
                message.put("event", EVENT_NEW_SERVER_FOUND);
                sendOutMessage(message);
            }
        });
        disposable = ipPinger.startPing(observable
                .filter(event -> event.getInt("type") == TYPE_WIFI_CONNECTION))
                .doOnNext(ip -> {
                    if (ip.equals("256")) {
                        updateServersList();
                    }
                })
                .doOnNext(ip -> Log.d("awdsawds", "Reachable:" + ip))
                .subscribe(ip -> ipsToScan.onNext(ip));
        return outSubject;
    }

    private void updateServersList() {
        synchronized (serversList) {
            serversList = recentServersList;
            recentServersList = new HashMap<>();
            JSONObject message = new JSONObject();
            JSONArray serversInfo = new JSONArray();
            for (String ip : serversList.keySet()) {
                serversInfo.put(serversList.get(ip));
            }
            try {
                message.put("serversInfo",serversInfo);
                message.put("address", ADDRESS_ACTIVITY);
                message.put("type", TYPE_MESSAGE);
                message.put("event", EVENT_SERVERS_LIST_UPDATE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendOutMessage(message);
        }
    }

    private synchronized void sendOutMessage(JSONObject message) {
        outSubject.onNext(message);
    }
}

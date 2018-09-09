package com.example.awds.mafiawifi.engines.server;


import android.util.Log;

import com.example.awds.mafiawifi.engines.Engine;
import com.example.awds.mafiawifi.structures.PlayerInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.structures.EventTypes.ADDRESS_ACTIVITY;
import static com.example.awds.mafiawifi.structures.EventTypes.ADDRESS_SOCKET_MANAGER;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_ACTIVITY_CONNECTED;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_GAME_STATE_INFO;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_NEW_PLAYER;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_PLAYER_DISCONNECTED;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_PREPARE_PORT;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_RELEASE_PORT;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_SERVER_INFO;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_GAME_EVENT;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_MESSAGE;

public class WaitingServerEngine extends Engine {
    private PublishSubject outSubject;
    private volatile boolean wait;
    private int infoPortTail;
    private ServerSocket InfoServerSocket;
    private Socket infoSocket;
    private DataOutputStream infoOutputStream;
    private int receptionPortTail;
    private ServerSocket receptionServerSocket;
    private Socket receptionSocket;
    private DataOutputStream receptionOutputStream;
    private byte[] serverInfo;
    private Thread infoThread;
    private Thread receptionThread;
    private int port;

    private HashMap<String, PlayerInfo> players;

    public WaitingServerEngine() {
        outSubject = PublishSubject.create();
        wait = true;
        infoPortTail = 0;
        receptionPortTail = 0;
        port = 1366;
        players = new HashMap<>();
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        Log.d("awdsawds", "bind waiting engine");
        observable.subscribeOn(Schedulers.io()).subscribe(message -> {
            Log.d("awdsawds", "WaitingEngine " + message.toString());
            int type = message.getInt("type");
            int event = message.getInt("event");
            if (type == TYPE_MESSAGE) {
                if (event == EVENT_SERVER_INFO) {
                    message.remove("type");
                    message.remove("event");
                    serverInfo = message.toString().getBytes();

                    PlayerInfo providerInfo = new PlayerInfo(message.getString("name"), "provider");
                    players.put("provider", providerInfo);

                    JSONObject outMessage = new JSONObject();
                    outMessage.put("address", ADDRESS_SOCKET_MANAGER);
                    outMessage.put("type", TYPE_MESSAGE);
                    outMessage.put("event", EVENT_PREPARE_PORT);
                    outMessage.put("port", port);
                    receptionThread.start();
                    infoThread.start();
                    sendOutMessage(outMessage);
                } else if (event == EVENT_ACTIVITY_CONNECTED) {
                    JSONObject messageToActivity = new JSONObject();
                    JSONArray playersList;

                    synchronized (players) {
                        playersList = new JSONArray(players.values());
                    }

                    messageToActivity.put("address", ADDRESS_ACTIVITY);
                    messageToActivity.put("type", TYPE_GAME_EVENT);
                    messageToActivity.put("event", EVENT_GAME_STATE_INFO);
                    messageToActivity.put("playersList", playersList);

                    sendOutMessage(messageToActivity);
                } else if (event == EVENT_PLAYER_DISCONNECTED) {
                    JSONObject messageToPlayers = new JSONObject();
                    JSONObject messageToActivity = new JSONObject();
                    JSONObject messageToSocketsManager = new JSONObject();

                    int port = message.getInt("port");
                    String id = "";
                    for (PlayerInfo info : players.values()) {
                        if (info.getPort() == port)
                            id = info.getId();
                    }
                    synchronized (players) {
                        messageToSocketsManager.put("port", port);
                        players.remove(id);
                    }

                    messageToPlayers.put("address", ADDRESS_SOCKET_MANAGER);
                    messageToPlayers.put("type", TYPE_GAME_EVENT);
                    messageToPlayers.put("event", EVENT_PLAYER_DISCONNECTED);
                    messageToPlayers.put("id", id);

                    sendMessageToAllPlayers(messageToPlayers);

                    messageToSocketsManager.put("address", ADDRESS_SOCKET_MANAGER);
                    messageToSocketsManager.put("type", TYPE_MESSAGE);
                    messageToSocketsManager.put("event", EVENT_RELEASE_PORT);

                    sendOutMessage(messageToSocketsManager);

                    messageToActivity.put("address", ADDRESS_ACTIVITY);
                    messageToActivity.put("type", TYPE_GAME_EVENT);
                    messageToActivity.put("event", EVENT_PLAYER_DISCONNECTED);
                    messageToActivity.put("id", id);

                    sendOutMessage(messageToActivity);
                }
            } else if (type == TYPE_GAME_EVENT) {
                if (event == EVENT_NEW_PLAYER) {
                    JSONObject messageToPlayers = new JSONObject();
                    JSONObject messageToActivity = new JSONObject();
                    JSONObject messageToNewPlayer = new JSONObject();

                    PlayerInfo player = new PlayerInfo(message.getJSONObject("playerInfo"));
                    JSONArray playersList;

                    synchronized (players) {
                        players.put(player.getId(), player);
                        playersList = new JSONArray(players.values());
                    }

                    messageToPlayers.put("address", ADDRESS_SOCKET_MANAGER);
                    messageToPlayers.put("type", TYPE_GAME_EVENT);
                    messageToPlayers.put("event", EVENT_NEW_PLAYER);
                    messageToPlayers.put("playerInfo", message.getJSONObject("playerInfo"));

                    sendMessageToAllPlayers(messageToPlayers);

                    messageToNewPlayer.put("address", ADDRESS_SOCKET_MANAGER);
                    messageToNewPlayer.put("type", TYPE_GAME_EVENT);
                    messageToNewPlayer.put("event", EVENT_GAME_STATE_INFO);
                    messageToNewPlayer.put("playersList", playersList);
                    messageToNewPlayer.put("port", player.getPort());

                    sendOutMessage(messageToNewPlayer);

                    messageToActivity.put("address", ADDRESS_ACTIVITY);
                    messageToActivity.put("type", TYPE_GAME_EVENT);
                    messageToActivity.put("event", EVENT_NEW_PLAYER);
                    messageToActivity.put("playerInfo", message.getJSONObject("playerInfo"));

                    sendOutMessage(messageToActivity);
                }
            }
        }, e -> {
            Log.d("awdsawds", "WaitingEngine error");
        }, () -> {
            Log.d("awdsawds", "WaitingEngine closing");
            if (InfoServerSocket != null)
                InfoServerSocket.close();
            if (receptionServerSocket != null)
                receptionServerSocket.close();
            wait = false;
            outSubject.onComplete();
        });
        infoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InfoServerSocket = new ServerSocket(1360 + (infoPortTail % 3));
                    Log.d("awdsawds", "waiting for client");
                    infoSocket = InfoServerSocket.accept();
                    infoOutputStream = new DataOutputStream(infoSocket.getOutputStream());
                    infoOutputStream.writeInt(serverInfo.length);
                    infoOutputStream.write(serverInfo);
                } catch (IOException e) {
                    Log.d("awdsawds", "socket fail " + (infoPortTail));
                    infoPortTail++;
                } finally {
                    Log.d("awdsawds", "socket close " + (infoPortTail));
                    if (infoOutputStream != null) {
                        try {
                            infoOutputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        infoOutputStream = null;
                    }
                    if (infoSocket != null) {
                        try {
                            infoSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        infoSocket = null;
                    }
                    if (InfoServerSocket != null) {
                        if (!InfoServerSocket.isClosed()) {
                            try {
                                InfoServerSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        InfoServerSocket = null;
                    }
                }
                if (wait)
                    run();
            }
        });
        receptionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receptionServerSocket = new ServerSocket(1363 + (receptionPortTail % 3));
                    Log.d("awdsawds", "waiting for client(reception)");
                    receptionSocket = receptionServerSocket.accept();
                    receptionOutputStream = new DataOutputStream(receptionSocket.getOutputStream());
                    port += 3;
                    JSONObject outMessage = new JSONObject();
                    try {
                        outMessage.put("address", ADDRESS_SOCKET_MANAGER);
                        outMessage.put("type", TYPE_MESSAGE);
                        outMessage.put("event", EVENT_PREPARE_PORT);
                        outMessage.put("port", port);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    receptionOutputStream.writeInt(port - 3);
                    sendOutMessage(outMessage);
                } catch (IOException e) {
                    Log.d("awdsawds", "reception socket fail " + (receptionPortTail));
                    receptionPortTail++;
                } finally {
                    Log.d("awdsawds", "reception socket close " + (receptionPortTail));
                    if (receptionOutputStream != null) {
                        try {
                            receptionOutputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        receptionOutputStream = null;
                    }
                    if (receptionSocket != null) {
                        try {
                            receptionSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        receptionSocket = null;
                    }
                    if (receptionServerSocket != null) {
                        if (!receptionServerSocket.isClosed()) {
                            try {
                                receptionServerSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        receptionServerSocket = null;
                    }
                }
                if (wait)
                    run();
            }
        });
        return outSubject;
    }

    private void sendMessageToAllPlayers(JSONObject message) {
        synchronized (players) {
            for (PlayerInfo info : players.values()) {
                if (port != -1) {
                    JSONObject mes = new JSONObject();
                    Iterator<String> keys = mes.keys();
                    try {
                        while (keys.hasNext()) {
                            String key = keys.next();
                            mes.put(key, message.get(key));
                        }
                        mes.put("port", info.getPort());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendOutMessage(mes);
                }
            }
        }
    }

    private synchronized void sendOutMessage(JSONObject message) {
        outSubject.onNext(message);
    }
}

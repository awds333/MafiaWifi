package com.example.awds.mafiawifi.engines.server;


import android.util.Log;

import com.example.awds.mafiawifi.engines.Engine;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.EventTypes.EVENT_SERVER_INFO;
import static com.example.awds.mafiawifi.EventTypes.TYPE_MESSAGE;

public class WaitingServerEngine extends Engine {
    private PublishSubject outSubject;
    private volatile boolean wait;
    private int portTail;
    private ServerSocket serverSocket;
    private Socket guestSocket;
    private DataOutputStream outputStream;
    private byte[] serverInfo;
    private Thread thread;

    public WaitingServerEngine() {
        outSubject = PublishSubject.create();
        wait = true;
        portTail = 0;
    }

    @Override
    public Observable<JSONObject> bind(Observable<JSONObject> observable) {
        Log.d("awdsawds","bind waiting engine");
        observable.subscribeOn(Schedulers.io()).subscribe(message -> {
            int type = message.getInt("type");
            int event = message.getInt("event");
            if(type==TYPE_MESSAGE){
                if(event==EVENT_SERVER_INFO){
                    message.remove("type");
                    message.remove("event");
                    Log.d("awdsawds",message.toString());
                    serverInfo = message.toString().getBytes();
                    thread.start();
                }
            }
        }, e -> {
        }, () -> {
            if(serverSocket!=null)
                serverSocket.close();
            wait = false;
            outSubject.onComplete();
        });
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(1360 + (portTail % 3));
                    Log.d("awdsawds","waiting for client");
                    guestSocket = serverSocket.accept();
                    outputStream = new DataOutputStream(guestSocket.getOutputStream());
                    outputStream.writeInt(serverInfo.length);
                    outputStream.write(serverInfo);
                } catch (IOException e) {
                    Log.d("awdsawds","socket fail "+ (portTail+1));
                    portTail++;
                } finally {
                    Log.d("awdsawds","socket close "+ (portTail+1));
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        outputStream = null;
                    }
                    if (guestSocket != null) {
                        try {
                            guestSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        guestSocket = null;
                    }
                    if (serverSocket != null) {
                        if (!serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        serverSocket = null;
                    }
                }
                if(wait)
                    run();
            }
        });
        return outSubject;
    }
}

package com.example.awds.mafiawifi.netclasses;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class ServerScanner {
    public Observable<JSONObject> bind(Observable<String> ips) {
         return ips.flatMap(ip -> Observable.just(ip))
                .subscribeOn(Schedulers.io())
                .flatMap(ip -> {
                    DataInputStream inputStream;
                    Socket socket;
                    String message = null;
                    for (int i = 0; i < 9; i++) {
                        socket = null;
                        inputStream = null;
                        try {
                            socket = new Socket(ip, 1360 + (i % 3));
                            inputStream = new DataInputStream(socket.getInputStream());
                            int length = inputStream.readInt();
                            byte[] bytes = new byte[length];
                            inputStream.read(bytes);
                            message = new String(bytes, "UTF-8");
                        } catch (IOException e) {

                        } finally {
                            if (inputStream != null)
                                inputStream.close();
                            if (socket != null)
                                socket.close();
                        }
                        if(message!=null){
                            JSONObject serverInfo = new JSONObject(message);
                            serverInfo.put("ip",ip);
                            return Observable.just(serverInfo);
                        }
                    }
                    return Observable.empty();
                }).map(i->i);
    }
}

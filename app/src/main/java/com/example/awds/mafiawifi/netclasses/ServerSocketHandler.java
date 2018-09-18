package com.example.awds.mafiawifi.netclasses;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

import static com.example.awds.mafiawifi.structures.EventTypes.ADDRESS_ENGINE;
import static com.example.awds.mafiawifi.structures.EventTypes.ADDRESS_SOCKET_MANAGER;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_FAILED_TO_OPEN_SERVER_SOCKET;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_MESSAGE_FROM_CLIENT;
import static com.example.awds.mafiawifi.structures.EventTypes.EVENT_PLAYER_DISCONNECTED;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_GAME_EVENT;
import static com.example.awds.mafiawifi.structures.EventTypes.TYPE_MESSAGE;

public class ServerSocketHandler {
    private ServerSocket serverSocket;
    private Socket socket;

    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private int port;
    private LinkedList<byte[]> messagesQueue;
    private volatile boolean finished = false;
    private volatile boolean reconnecting = false;

    private PublishSubject<JSONObject> outputSubject;
    private Observable<JSONObject> messagesObservable;
    private Disposable messagesObservableDisposable;

    private Runnable reconnect;
    private Runnable sendMessageFromQueue;
    private Runnable checkInputMessages;
    private Runnable checkConnection;
    private Runnable notifyEngine;

    private ScheduledFuture sendMessageFuture;
    private ScheduledFuture checkInputFuture;
    private ScheduledFuture checkConnectionFuture;
    private ScheduledFuture notifyEngineFuture;

    public ServerSocketHandler(int port, Observable<JSONObject> observable, ScheduledExecutorService executorService, PublishSubject output) {
        this.port = port;
        outputSubject = output;
        messagesObservable = observable.filter(message -> message.getInt("port") == this.port);
        messagesObservableDisposable = observable.subscribe(
                message -> {
                    synchronized (messagesQueue) {
                        messagesQueue.add(message.toString().getBytes());
                    }
                }, e -> {
                    Log.d("awdsawds", "Critical exception in ServerSocketHandler Input Observable");
                }, () -> {
                    finish();
                }
        );

        reconnect = new Runnable() {
            @Override
            public void run() {
                if (finished || reconnecting)
                    return;
                reconnecting = true;
                if (sendMessageFuture != null) {
                    sendMessageFuture.cancel(false);
                    sendMessageFuture = null;
                }
                if (checkConnectionFuture != null) {
                    checkConnectionFuture.cancel(false);
                    checkConnectionFuture = null;
                }
                if (checkInputFuture != null) {
                    checkInputFuture.cancel(false);
                    checkInputFuture = null;
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    inputStream = null;
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    outputStream = null;
                }
                if (socket != null) {
                    if (!socket.isClosed())
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    socket = null;
                }
                if (serverSocket != null) {
                    if (!serverSocket.isClosed())
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    serverSocket = null;
                }

                synchronized (executorService) {
                    notifyEngineFuture = executorService.schedule(notifyEngine, 5, TimeUnit.SECONDS);
                }
                while (!finished) {
                    try {
                        openServerSocket();
                        if (finished){
                            if(serverSocket!=null)
                                serverSocket.close();
                            return;
                        }
                        socket = serverSocket.accept();
                    } catch (IOException e) {

                    }
                    if (socket.isConnected())
                        break;
                }
                if (finished)
                    return;
                if (!notifyEngineFuture.isDone())
                    notifyEngineFuture.cancel(false);
                else {
                    synchronized (messagesQueue) {
                        messagesQueue = new LinkedList<>();
                    }
                }
                synchronized (executorService) {
                    sendMessageFuture = executorService.scheduleAtFixedRate(sendMessageFromQueue, 0, 75, TimeUnit.MILLISECONDS);
                    checkInputFuture = executorService.scheduleAtFixedRate(checkInputMessages, 0, 150, TimeUnit.MILLISECONDS);
                    checkConnectionFuture = executorService.scheduleAtFixedRate(checkConnection, 0, 250, TimeUnit.MILLISECONDS);
                }
            }
        };

        notifyEngine = new Runnable() {
            @Override
            public void run() {
                JSONObject message = new JSONObject();
                try {
                    message.put("address", ADDRESS_ENGINE);
                    message.put("type", TYPE_MESSAGE);
                    message.put("event", EVENT_PLAYER_DISCONNECTED);
                    message.put("port", port);
                    sendOutputMessage(message);
                } catch (JSONException e) {
                }
            }
        };

        checkConnection = new Runnable() {
            @Override
            public void run() {
                if (finished || reconnecting)
                    return;
                if (!socket.isConnected()) {
                    Thread reconnectingThread = new Thread(reconnect);
                    reconnectingThread.start();
                }
            }
        };

        sendMessageFromQueue = new Runnable() {
            @Override
            public void run() {
                if (finished || reconnecting)
                    return;
                byte[] message;
                synchronized (messagesQueue) {
                    if (messagesQueue.size() == 0)
                        return;
                    message = messagesQueue.get(0);
                    try {
                        outputStream.writeInt(message.length);
                        outputStream.write(message);
                        messagesQueue.remove(0);
                    } catch (IOException e) {
                    }
                }
            }
        };

        checkInputMessages = new Runnable() {
            @Override
            public void run() {
                if (finished || reconnecting)
                    return;
                byte[] messageBytes = null;
                try {
                    if (inputStream.available() == 0)
                        return;
                    int length = inputStream.readInt();
                    messageBytes = new byte[length];
                    inputStream.readFully(messageBytes);
                } catch (IOException e) {
                    return;
                }
                try {
                    JSONObject message = new JSONObject();
                    message.put("address", ADDRESS_ENGINE);
                    message.put("type", TYPE_GAME_EVENT);
                    message.put("event", EVENT_MESSAGE_FROM_CLIENT);
                    message.put("message", new String(messageBytes));
                    message.put("port", port);
                    sendOutputMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable init = new Runnable() {
            @Override
            public void run() {
                messagesQueue = new LinkedList<>();
                try {
                    openServerSocket();
                    serverSocket.setSoTimeout(5000);
                } catch (IOException e) {
                    //fail to open serverSocket
                    JSONObject message = new JSONObject();
                    try {
                        message.put("address", ADDRESS_SOCKET_MANAGER);
                        message.put("type", TYPE_MESSAGE);
                        message.put("event", EVENT_FAILED_TO_OPEN_SERVER_SOCKET);
                        message.put("port", port);
                        sendOutputMessage(message);
                        return;
                    } catch (JSONException e1) {
                    }
                }
                try {
                    socket = serverSocket.accept();
                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                } catch (SocketTimeoutException e) {
                    JSONObject message = new JSONObject();
                    try {
                        message.put("address", ADDRESS_SOCKET_MANAGER);
                        message.put("type", TYPE_MESSAGE);
                        message.put("event", EVENT_FAILED_TO_OPEN_SERVER_SOCKET);
                        message.put("port", port);
                        sendOutputMessage(message);
                        return;
                    } catch (JSONException e1) {
                    }
                } catch (IOException e) {
                    Thread reconnectingThread = new Thread(reconnect);
                    reconnectingThread.start();
                    return;
                }

                synchronized (executorService) {
                    sendMessageFuture = executorService.scheduleAtFixedRate(sendMessageFromQueue, 0, 75, TimeUnit.MILLISECONDS);
                    checkInputFuture = executorService.scheduleAtFixedRate(checkInputMessages, 0, 150, TimeUnit.MILLISECONDS);
                    checkConnectionFuture = executorService.scheduleAtFixedRate(checkConnection, 0, 250, TimeUnit.MILLISECONDS);
                }
            }
        };
        Thread initThread = new Thread(init);
        initThread.start();
    }

    private synchronized void sendOutputMessage(JSONObject message) {
        outputSubject.onNext(message);
    }

    public void finish() {
        if (finished)
            return;
        finished = true;
        if (sendMessageFuture != null) {
            if (!sendMessageFuture.isCancelled())
                sendMessageFuture.cancel(false);
        }
        if (checkConnectionFuture != null) {
            if (!checkConnectionFuture.isCancelled())
                checkConnectionFuture.cancel(false);
        }
        if (checkInputFuture != null) {
            if (!checkInputFuture.isCancelled())
                checkInputFuture.cancel(false);
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            if (!socket.isClosed())
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        if (serverSocket != null) {
            if (!serverSocket.isClosed())
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        if (!messagesObservableDisposable.isDisposed())
            messagesObservableDisposable.dispose();
        outputSubject.onComplete();
    }

    private void openServerSocket() throws IOException {
        for (int i = 0; i < 9; i++) {
            try {
                serverSocket = new ServerSocket(port + (i % 3));
            } catch (IOException e) {
                continue;
            }
            return;
        }
        throw new IOException();
    }
}

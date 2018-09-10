package com.example.awds.mafiawifi.structures;

public class EventTypes {
    public static final int ADDRESS_SOCKET_MANAGER = 0;
    public static final int ADDRESS_ACTIVITY = 1;
    public static final int ADDRESS_ENGINE = 2;
    public static final int ADDRESS_SERVICE = 3;

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_WIFI_CONNECTION = 1;
    public static final int TYPE_CHANGE_STATE = 2;
    public static final int TYPE_GAME_EVENT = 3;

    //Change state events:
    public static final int EVENT_CONNECT_TO_SERVER = 0;
    public static final int EVENT_FINISH = 1;
    public static final int EVENT_NEXT_ENGINE = 2;
    public static final int EVENT_UPDATE_NOTIFICATION = 3;

    //Message events:
    public static final int EVENT_GAME_STATE_INFO = -1; //уникальный номер
    public static final int EVENT_NEW_SERVER_FOUND = 0;
    public static final int EVENT_SERVERS_LIST_UPDATE = 1;
    public static final int EVENT_MY_INFO = 2;
    public static final int EVENT_PREPARE_PORT = 3;
    public static final int EVENT_RELEASE_PORT = 4;
    public static final int EVENT_PLAYER_DISCONNECTED = 5;
    public static final int EVENT_ACTIVITY_CONNECTED = 6;
    public static final int EVENT_FAILED_TO_OPEN_SERVER_SOCKET = 7;


    //Game events:
    public static final int EVENT_NEW_PLAYER = 0;
}

package com.example.awds.mafiawifi.structures;

import org.json.JSONException;
import org.json.JSONObject;

public class PlayerInfo {

    private String name;
    private String id;
    private int port;

    private boolean alive;
    private int role;

    public PlayerInfo(String name, String id) {
        this.name = name;
        this.id = id;
        role = -1;
        alive = true;
        port = -1;
    }

    public PlayerInfo(String name, String id, int port) {
        this.port = port;
        this.name = name;
        this.id = id;
        role = -1;
        alive = true;
    }

    public PlayerInfo(JSONObject playerInfo) throws JSONException {
        name = playerInfo.getString("name");
        id = playerInfo.getString("id");
        role = -1;
        alive = true;
    }

    public void setGameState(int role) {
        this.role = role;
    }

    public JSONObject toJsonObject() {
        JSONObject myInfo = new JSONObject();
        try {
            myInfo.put("name", name);
            myInfo.put("id", id);
            if (role != -1) {
                myInfo.put("role", role);
                myInfo.put("alive", alive);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return myInfo;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getRole() {
        return role;
    }

    public int getPort() {
        return port;
    }
}

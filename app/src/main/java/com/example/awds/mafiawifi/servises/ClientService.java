package com.example.awds.mafiawifi.servises;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ClientService extends Service {



    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class MyBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }


}

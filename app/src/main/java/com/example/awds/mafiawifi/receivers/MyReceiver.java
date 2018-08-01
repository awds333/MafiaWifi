package com.example.awds.mafiawifi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.awds.mafiawifi.servises.ClientService;
import com.example.awds.mafiawifi.servises.ServerService;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent toService = null;
        if (intent.getStringExtra("type").equals("client")) {
            toService = new Intent(context, ClientService.class);
        } else if (intent.getStringExtra("type").equals("server")) {
            toService = new Intent(context, ServerService.class);
        } else return;
        toService.putExtra("type", "finish");
        context.startService(toService);
    }
}

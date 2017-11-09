package com.example.awds.mafiawifi.servises;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ServerService extends Service {
    public ServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        PublishSubject s = PublishSubject.create();
        Observable o = Observable.range(1,3);
        Observable p = o.mergeWith(s);
    }
}

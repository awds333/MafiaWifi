package com.example.awds.mafiawifi.servises;

import android.os.Binder;

import com.example.awds.mafiawifi.interfaces.Bindable;

public class MyBinder extends Binder{
    private Bindable bindable;

    public MyBinder(Bindable bindable) {
        this.bindable = bindable;
    }

    public Bindable getService() {
        return bindable;
    }
}

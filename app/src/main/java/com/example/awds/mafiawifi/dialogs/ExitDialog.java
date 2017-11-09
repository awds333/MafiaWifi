package com.example.awds.mafiawifi.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.awds.mafiawifi.R;


public class ExitDialog extends DialogFragment implements View.OnClickListener{
    View dialog;
    Activity context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dialog = inflater.inflate(R.layout.exit_dialog, null);
        dialog.findViewById(R.id.exitbt).setOnClickListener(this);
        dialog.findViewById(R.id.canselbt).setOnClickListener(this);
        context = getActivity();
        return dialog;
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.exitbt){
            context.finish();
            dismiss();
        } else dismiss();
    }
}

package com.example.awds.mafiawifi.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.awds.mafiawifi.R;
import com.example.awds.mafiawifi.activitys.MainActivity;
import com.example.awds.mafiawifi.activitys.server.WaitingForPlayersActivity;
import com.example.awds.mafiawifi.servises.ServerService;


public class ServerNamePickDialog extends DialogFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private View dialog;
    private Activity context;
    private LinearLayout pasLayout;
    private EditText password;
    private SharedPreferences sPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dialog = inflater.inflate(R.layout.newserv_dialog, null);
        dialog.findViewById(R.id.yesbt).setOnClickListener(this);
        dialog.findViewById(R.id.canselbt).setOnClickListener(this);
        ((CheckBox) dialog.findViewById(R.id.passwordchb)).setOnCheckedChangeListener(this);
        pasLayout = ((LinearLayout) dialog.findViewById(R.id.container));
        context = getActivity();
        sPreferences = context.getSharedPreferences(MainActivity.MY_TAG, Context.MODE_PRIVATE);
        if (sPreferences.getBoolean("remember", false)) {
            ((EditText) dialog.findViewById(R.id.text)).setText(sPreferences.getString("name", ""));
            ((EditText) dialog.findViewById(R.id.sname)).setText(sPreferences.getString("servername", ""));
        }
        return dialog;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.yesbt) {
            String name = ((EditText) dialog.findViewById(R.id.text)).getText().toString();
            String servname = ((EditText) dialog.findViewById(R.id.sname)).getText().toString();
            if (!name.replaceAll("\\s+", "").equals("")) {
                if (!servname.replaceAll("\\s+", "").equals("")) {
                    Intent intent = new Intent(context, WaitingForPlayersActivity.class);
                    Intent startService = new Intent(context, ServerService.class);
                    startService.putExtra("name", name);
                    startService.putExtra("servername", servname);
                    intent.putExtra("name", name);
                    intent.putExtra("servername", servname);
                    if (password != null)
                        if (!password.getText().toString().equals("")) {
                            intent.putExtra("lock", password.getText().toString());
                            startService.putExtra("lock", password.getText().toString());
                        }
                    if (sPreferences.getBoolean("remember", false)) {
                        SharedPreferences.Editor editor = sPreferences.edit();
                        editor.putString("name", name);
                        editor.putString("servername", servname);
                        editor.commit();
                    }
                    context.startService(startService);
                    context.startActivity(intent);
                    dismiss();
                } else
                    Toast.makeText(context, context.getText(R.string.serventername), Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(context, context.getText(R.string.entername), Toast.LENGTH_LONG).show();
        } else {
            dismiss();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        pasLayout.removeAllViews();
        password = new EditText(context);
        password.setHint(R.string.password);
        password.setHintTextColor(Color.GRAY);
        password.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(8);
        password.setFilters(fArray);
        pasLayout.addView(password, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}

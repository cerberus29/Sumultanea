package com.example.cj.sumultanea;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;

public class SETTINGS extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    public boolean multiplayer = false;
    public String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.multiplayerwitch:
                if (b) {
                    // TODO: start multiplayer mode
                } else {
                    // TODO: stop multiplayer mode
                }
                multiplayer = b;
                break;
        }
    }
}

package com.example.cj.sumultanea;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

/**
 * Save and restore settings using the example from:
 * https://developer.android.com/guide/topics/data/data-storage.html#pref
 */
public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher {
    private static final String PREFS_NAME = "SettingsActivity";
    private static final String PREF_MULTI_PLAYER_MODE = "multiPlayerMode";
    private static final String PREF_MULTI_PLAYER_NAME = "multiPlayerName";

    private static boolean mMultiPlayerMode;
    private static String mMultiPlayerName;
    private EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Restore preferences

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mMultiPlayerMode = settings.getBoolean(PREF_MULTI_PLAYER_MODE, false);
        mMultiPlayerName = settings.getString(PREF_MULTI_PLAYER_NAME, "player");

        Switch multiPlayerModeSwitch = findViewById(R.id.multiPlayerModeSwitch);
        multiPlayerModeSwitch.setChecked(mMultiPlayerMode);
        name = findViewById(R.id.uname);
        name.addTextChangedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_MULTI_PLAYER_MODE, mMultiPlayerMode);
        editor.putString(PREF_MULTI_PLAYER_NAME, mMultiPlayerName);

        // Commit the edits!
        editor.commit();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.multiPlayerModeSwitch:
                if (b) {
                    // TODO: start multiplayer mode
                } else {
                    // TODO: stop multiplayer mode
                }
                mMultiPlayerMode = b;
                break;
        }
    }

    public static boolean isMultiPlayerMode() {
        return mMultiPlayerMode;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
    mMultiPlayerName = name.getText().toString();
    }
}

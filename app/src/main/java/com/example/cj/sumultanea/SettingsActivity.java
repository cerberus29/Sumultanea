package com.example.cj.sumultanea;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Save and restore settings using the example from:
 * https://developer.android.com/guide/topics/data/data-storage.html#pref
 */
public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String PREFS_NAME = "SettingsActivity";
    private static final String PREF_MULTI_PLAYER_MODE = "multiPlayerMode";
    private static boolean mMultiPlayerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mMultiPlayerMode = settings.getBoolean(PREF_MULTI_PLAYER_MODE, false);

        Switch multiPlayerModeSwitch = findViewById(R.id.multiPlayerModeSwitch);
        multiPlayerModeSwitch.setChecked(mMultiPlayerMode);
        // Restrict to single-player mode for now
        multiPlayerModeSwitch.setEnabled(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_MULTI_PLAYER_MODE, mMultiPlayerMode);

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
}

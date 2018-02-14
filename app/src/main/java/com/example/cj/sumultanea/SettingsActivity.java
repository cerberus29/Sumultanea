package com.example.cj.sumultanea;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Save and restore settings using the example from:
 * https://developer.android.com/guide/topics/data/data-storage.html#pref
 */
public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String PREFS_NAME = "SettingsActivity";
    // Settings keys
    private static final String PREF_MULTI_PLAYER_MODE = "multiPlayerMode";
    private static final String PREF_MULTI_PLAYER_MASTER = "multiPlayerMaster";
    private static final String PREF_MULTI_PLAYER_ALIAS = "multiPlayerAlias";
    // Default values
    private static final boolean DEFAULT_MULTI_PLAYER_MODE = false;
    private static final boolean DEFAULT_MULTI_PLAYER_MASTER = false;
    private static final String DEFAULT_MULTI_PLAYER_ALIAS = "Player";

    private Switch mMultiPlayerMasterView;
    private EditText mMultiPlayerAliasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Restore preferences
        boolean multiPlayerMode = isMultiPlayerMode(this);
        boolean multiPlayerMaster = isMultiPlayerMaster(this);
        String multiPlayerAlias = getMultiPlayerAlias(this);

        // Modify UI to show the current values
        Switch multiPlayerModeSwitch = findViewById(R.id.multiPlayerModeSwitch);
        multiPlayerModeSwitch.setChecked(multiPlayerMode);
        multiPlayerModeSwitch.setOnCheckedChangeListener(this);

        mMultiPlayerMasterView = findViewById(R.id.multiPlayerMasterSwitch);
        mMultiPlayerMasterView.setChecked(multiPlayerMaster);
        mMultiPlayerMasterView.setEnabled(multiPlayerMode);
        mMultiPlayerMasterView.setOnCheckedChangeListener(this);

        mMultiPlayerAliasView = findViewById(R.id.multiPlayerAlias);
        mMultiPlayerAliasView.setText(multiPlayerAlias);
        mMultiPlayerAliasView.setEnabled(multiPlayerMode);
        mMultiPlayerAliasView.setOnEditorActionListener(mMultiPlayerAliasListener);
    }

    // --------------- Get notified when UI changes -----------------------------

    // This gets called when a Switch button was toggled
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.multiPlayerModeSwitch:
                setMultiplayerMode(b);
                mMultiPlayerAliasView.setEnabled(b);
                mMultiPlayerMasterView.setEnabled(b);
                break;
            case R.id.multiPlayerMasterSwitch:
                setMultiplayerMaster(b);
                break;
        }
    }

    // This gets called when we interact with the TextView
    public TextView.OnEditorActionListener mMultiPlayerAliasListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                            event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    setMultiplayerAlias(mMultiPlayerAliasView.getText().toString());
                    return true; // consume.
                }
            }
            return false;
        }
    };

    // --------------- Public getters (static because can be called from other activity) -------

    public static boolean isMultiPlayerMode(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(PREF_MULTI_PLAYER_MODE, DEFAULT_MULTI_PLAYER_MODE);
    }
    public static boolean isMultiPlayerMaster(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(PREF_MULTI_PLAYER_MASTER, DEFAULT_MULTI_PLAYER_MASTER);
    }
    public static String getMultiPlayerAlias(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(PREF_MULTI_PLAYER_ALIAS, DEFAULT_MULTI_PLAYER_ALIAS);
    }

    // --------------- Internal setters -----------------------------

    private void setMultiplayerMode(boolean enabled) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_MULTI_PLAYER_MODE, enabled);
        editor.apply();
    }

    private void setMultiplayerMaster(boolean isMaster) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_MULTI_PLAYER_MASTER, isMaster);
        editor.apply();
    }

    private void setMultiplayerAlias(String alias) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_MULTI_PLAYER_ALIAS, alias);
        editor.apply();
    }

}

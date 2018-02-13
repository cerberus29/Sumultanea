package com.example.cj.sumultanea;

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
    private static final String PREF_MULTI_PLAYER_MODE = "multiPlayerMode";
    private static final String PREF_MULTI_PLAYER_MASTER = "multiPlayerMaster";
    private static final String PREF_MULTI_PLAYER_ALIAS = "multiPlayerAlias";

    private static boolean mMultiPlayerMode;
    private static boolean mMultiPlayerMaster;
    private static String mMultiPlayerAlias;

    private Switch mMultiPlayerMasterView;
    private EditText mMultiPlayerAliasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mMultiPlayerMode = settings.getBoolean(PREF_MULTI_PLAYER_MODE, false);
        mMultiPlayerMaster = settings.getBoolean(PREF_MULTI_PLAYER_MASTER, false);
        mMultiPlayerAlias = settings.getString(PREF_MULTI_PLAYER_ALIAS, "Player");

        // Modify UI to show the current values
        Switch multiPlayerModeSwitch = findViewById(R.id.multiPlayerModeSwitch);
        multiPlayerModeSwitch.setChecked(mMultiPlayerMode);
        multiPlayerModeSwitch.setOnCheckedChangeListener(this);
        mMultiPlayerMasterView = findViewById(R.id.multiPlayerMasterSwitch);
        mMultiPlayerMasterView.setChecked(mMultiPlayerMaster);
        mMultiPlayerMasterView.setEnabled(mMultiPlayerMode);
        mMultiPlayerMasterView.setOnCheckedChangeListener(this);
        mMultiPlayerAliasView = findViewById(R.id.multiPlayerAlias);
        mMultiPlayerAliasView.setText(mMultiPlayerAlias);
        mMultiPlayerAliasView.setEnabled(mMultiPlayerMode);
        mMultiPlayerAliasView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event != null &&
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event == null || !event.isShiftPressed()) {
                        mMultiPlayerAlias = mMultiPlayerAliasView.getText().toString();
                        return true; // consume.
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onStop() {
        // Save values
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_MULTI_PLAYER_MODE, mMultiPlayerMode);
        editor.putBoolean(PREF_MULTI_PLAYER_MASTER, mMultiPlayerMaster);
        editor.putString(PREF_MULTI_PLAYER_ALIAS, mMultiPlayerAlias);
        editor.commit();
        super.onStop();
    }

    // --------------- Get notified when UI changes -----------------------------

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
                mMultiPlayerAliasView.setEnabled(mMultiPlayerMode);
                mMultiPlayerMasterView.setEnabled(mMultiPlayerMode);
                break;
            case R.id.multiPlayerMasterSwitch:
                mMultiPlayerMaster = b;
                break;
        }
    }

    // --------------- make values available -----------------------------

    public static boolean isMultiPlayerMode() {
        return mMultiPlayerMode;
    }
    public static boolean isMultiPlayerMaster() {
        return mMultiPlayerMaster;
    }
    public static String getMultiPlayerAlias() {
        return mMultiPlayerAlias;
    }
}

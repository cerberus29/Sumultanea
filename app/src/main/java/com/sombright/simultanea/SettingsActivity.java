package com.sombright.simultanea;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextView.OnEditorActionListener {
    private Switch mMultiPlayerMasterView;
    private EditText mMultiPlayerAliasView;
    private PreferencesProxy mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mPrefs = new PreferencesProxy(this);

        // Restore preferences
        boolean multiPlayerMode = mPrefs.isMultiPlayerMode();
        boolean multiPlayerMaster = mPrefs.isMultiPlayerMaster();
        String multiPlayerAlias = mPrefs.getMultiPlayerAlias();

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
        mMultiPlayerAliasView.setOnEditorActionListener(this);
    }

    // --------------- Get notified when UI changes -----------------------------

    // This gets called when a Switch button was toggled
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.multiPlayerModeSwitch:
                mPrefs.setMultiplayerMode(b);
                mMultiPlayerAliasView.setEnabled(b);
                mMultiPlayerMasterView.setEnabled(b);
                break;
            case R.id.multiPlayerMasterSwitch:
                mPrefs.setMultiplayerMaster(b);
                break;
        }
    }

    // This gets called when we interact with the TextView
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v != mMultiPlayerAliasView) {
            return false;
        }
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event != null &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (event == null || !event.isShiftPressed()) {
                mPrefs.setMultiplayerAlias(mMultiPlayerAliasView.getText().toString());
                return true;
            }
        }
        return false;
    }

}

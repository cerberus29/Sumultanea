package com.sombright.simultanea;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private MediaPlayer mediaPlayer;
    private boolean playIntro = true;
    private PreferencesProxy mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mPrefs = new PreferencesProxy(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (playIntro) {
            playIntro = false;
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.home);
            mediaPlayer.setLooping(true);
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onPause();
    }

    public void onClickCharacters(View view) {
        Intent intent;
        Log.d(TAG, "Launching character selection activity");
        intent = new Intent(this, CharacterSelectionActivity.class);
        startActivity(intent);
    }

    public void onClickPlay(View view) {
        Intent intent;
        if (mPrefs.isMultiPlayerMaster())
            intent = new Intent(this, TaskMasterActivity.class);
        else
            intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    public void onClickSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickProfile(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }
}

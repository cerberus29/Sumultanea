package com.example.cj.sumultanea;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class simultanea extends AppCompatActivity {
    public static final String TAG = "Simultanea";
    private MediaPlayer mediaPlayer;

    static final int PICK_CHARACTER_REQUEST = 0;
    public static final String CHARACTER_KEY = "character";
    public static final int DEFAULT_CHARACTER = 0;
    private int currentCharacter = DEFAULT_CHARACTER;
    private boolean playIntro = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simultanea);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playIntro) {
            playIntro = false;
            Intent intent = new Intent(this, intro.class);
            startActivity(intent);
        } else if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.home);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void onClickCharacters(View view) {
        Intent intent;
        Log.d(TAG, "Launching character selection activity");
        intent = new Intent(this, CharacterSelectionActivity.class);
        startActivityForResult(intent, PICK_CHARACTER_REQUEST);
    }

    public void onClickPlay(View view) {
        Intent intent = new Intent(this, PLAY.class);
        intent.putExtra(CHARACTER_KEY, currentCharacter);
        startActivity(intent);
    }

    public void onClickSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickProfile(View view) {
        Intent intent = new Intent(this, PROFILE.class);
        startActivity(intent);
    }

    // This function is called when a child activity returns from startActivityForResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_CHARACTER_REQUEST:
                if (resultCode == RESULT_OK) {
                    currentCharacter = data.getIntExtra(CHARACTER_KEY, currentCharacter);
                }
                break;
        }
    }
}

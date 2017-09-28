package com.example.cj.sumultanea;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class simultanea extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "Simultanea";
    private Button playbutton;
    private Button characterselection;
    private Button settingss;
    private Button profileview;

    static final int PICK_CHARACTER_REQUEST = 0;
    public static final String CHARACTER_KEY = "character";
    public static final int CHARACTER1 = 1;
    public static final int CHARACTER2 = 2;
    public static final int CHARACTER3 = 3;
    public static final int CHARACTER4 = 4;
    private int currentCharacter = CHARACTER1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaPlayer ring = MediaPlayer.create(this, R.raw.home);
        ring.setLooping(true);
        ring.start();

        setContentView(R.layout.activity_simultanea);

        playbutton = (Button) findViewById(R.id.PLAY);
        playbutton.setOnClickListener(this);

        settingss = (Button) findViewById(R.id.SETTINGS);
        settingss.setOnClickListener(this);

        characterselection = (Button) findViewById(R.id.CHARACTERS);
        characterselection.setOnClickListener(this);

        profileview = (Button) findViewById(R.id.PROFILE);
        profileview.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.CHARACTERS:
                Log.d(TAG, "Launching character selection activity");
                intent = new Intent(this, characters.class);
                startActivityForResult(intent, PICK_CHARACTER_REQUEST);
                break;
            case R.id.PLAY:
                intent = new Intent(this, PLAY.class);
                intent.putExtra(CHARACTER_KEY, currentCharacter);
                startActivity(intent);
                break;
            case R.id.SETTINGS:
                intent = new Intent(this, SETTINGS.class);
                startActivity(intent);
                break;
            case R.id.PROFILE:
                intent = new Intent(this, PROFILE.class);
                startActivity(intent);
                break;
        }

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

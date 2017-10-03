package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class characters extends AppCompatActivity implements View.OnClickListener {
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characters);

        // Start all button animations
        ImageButton btn;
        AnimationDrawable anim;
        btn = (ImageButton) findViewById(R.id.buttonCharacter1);
        anim = (AnimationDrawable) btn.getDrawable();
        anim.start();
        btn = (ImageButton) findViewById(R.id.buttonCharacter2);
        anim = (AnimationDrawable) btn.getDrawable();
        anim.start();
        btn = (ImageButton) findViewById(R.id.buttonCharacter3);
        anim = (AnimationDrawable) btn.getDrawable();
        anim.start();
        btn = (ImageButton) findViewById(R.id.buttonCharacter4);
        anim = (AnimationDrawable) btn.getDrawable();
        anim.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer = MediaPlayer.create(characters.this, R.raw.fight1);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    public void onClick(View view) {
        // Return the character selection back to the main activity
        Intent resultIntent = new Intent();
        switch (view.getId()) {
            case R.id.buttonCharacter1:
                resultIntent.putExtra(simultanea.CHARACTER_KEY, simultanea.CHARACTER1);
                break;
            case R.id.buttonCharacter2:
                resultIntent.putExtra(simultanea.CHARACTER_KEY, simultanea.CHARACTER2);
                break;
            case R.id.buttonCharacter3:
                resultIntent.putExtra(simultanea.CHARACTER_KEY, simultanea.CHARACTER3);
                break;
            case R.id.buttonCharacter4:
                resultIntent.putExtra(simultanea.CHARACTER_KEY, simultanea.CHARACTER4);
                break;
            default:
                setResult(RESULT_CANCELED);
                finish();
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
package com.example.cj.sumultanea;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.VideoView;

public class intro2 extends AppCompatActivity implements MediaPlayer.OnCompletionListener {
    private VideoView videov;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro2);
        videov = findViewById(R.id.videoView);
        String videopath = "android.resource://" + getPackageName() + "/" + R.raw.pyre;
        Uri uri = Uri.parse(videopath);
        videov.setVideoURI(uri);
        videov.setOnCompletionListener(this);
        videov.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        finish();
    }
}

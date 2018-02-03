package com.example.cj.sumultanea;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.VideoView;

public class intro extends AppCompatActivity implements MediaPlayer.OnCompletionListener {
    private static final int videoSequence[] = {
            R.raw.load,
            R.raw.pyre,
    };
    private int videoCounter;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);
        videoView = findViewById(R.id.videoView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoCounter = 0;
        startNextVideo();
    }

    private void startNextVideo() {
        int currentVideo = videoSequence[videoCounter];
        String videoPath = "android.resource://" + getPackageName() + "/" + currentVideo;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);
        videoView.setOnCompletionListener(this);
        videoView.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        videoCounter++;
        if (videoCounter < videoSequence.length) {
            startNextVideo();
        } else {
            finish();
        }
    }
}

package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

public class characters extends AppCompatActivity implements View.OnClickListener {


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characters);

        MediaPlayer ring = MediaPlayer.create(characters.this, R.raw.fight1);
        ring.start();
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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    public void clkplay(View v)

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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("characters Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
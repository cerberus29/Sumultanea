package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;

public class CharacterSelectionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MediaPlayer mediaPlayer;
    CharacterSelectionAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);
        // Automatically fill-up the GridView with our adapter
        GridView gridview = findViewById(R.id.characterSelectionGridView);
        mAdapter = new CharacterSelectionAdapter(this, R.layout.character_selection_example_item, CharacterPool.charactersList);
        gridview.setAdapter(mAdapter);
        gridview.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer = MediaPlayer.create(CharacterSelectionActivity.this, R.raw.fight1);
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
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        // Return the character selection back to the main activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(simultanea.CHARACTER_KEY, position);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
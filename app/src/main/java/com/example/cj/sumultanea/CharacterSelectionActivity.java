package com.example.cj.sumultanea;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class CharacterSelectionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static final String TAG = "CharacterSel";
    private MediaPlayer mediaPlayer;
    CharacterSelectionAdapter mAdapter;
    private GridView gridview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);
        // Automatically fill-up the GridView with our adapter
        gridview = findViewById(R.id.characterSelectionGridView);
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
        Intent intent;
        Log.d(TAG, "Launching character details activity");
        intent = new Intent(this, CharacterDetailsActivity.class);
        intent.putExtra(simultanea.CHARACTER_KEY, position);
        startActivityForResult(intent, simultanea.PICK_CHARACTER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case simultanea.PICK_CHARACTER_REQUEST:
                if (resultCode == RESULT_OK) {
                    int character = data.getIntExtra(simultanea.CHARACTER_KEY, gridview.getSelectedItemPosition());
                    // Return the character selection back to the main activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(simultanea.CHARACTER_KEY, character);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
                break;
        }
    }
}
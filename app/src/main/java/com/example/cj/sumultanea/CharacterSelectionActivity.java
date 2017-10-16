package com.example.cj.sumultanea;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class CharacterSelectionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MediaPlayer mediaPlayer;
    CharacterSelectionAdapter mAdapter;
    private GridView gridview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);

        // Instead of manually filling the grid with character icons, we connect
        // the GridView to an "adapter" whose job is to prepare each character
        // icon in a uniform way.
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

    // Called when the player clicks an item in the grid view (position)
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        // Launch CharacterDetailsActivity to show the details of the character at "position"
        Intent intent;
        intent = new Intent(this, CharacterDetailsActivity.class);
        intent.putExtra(simultanea.CHARACTER_KEY, position);
        startActivityForResult(intent, simultanea.PICK_CHARACTER_REQUEST);
    }

    // Called when the CharacterDetailsActivity returns:
    // If the user clicked "pick" in the details activity, we go back to the main menu.
    // If the user clicked the back button, we stay here to select another character.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case simultanea.PICK_CHARACTER_REQUEST:
                if (resultCode == RESULT_OK) {
                    // Send the character number back to the main activity
                    int character = data.getIntExtra(simultanea.CHARACTER_KEY, gridview.getSelectedItemPosition());
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(simultanea.CHARACTER_KEY, character);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
                break;
        }
    }
}
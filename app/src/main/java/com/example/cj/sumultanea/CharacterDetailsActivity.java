package com.example.cj.sumultanea;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import static com.example.cj.sumultanea.simultanea.DEFAULT_CHARACTER;

public class CharacterDetailsActivity extends AppCompatActivity {
    private int mCharacterNum = DEFAULT_CHARACTER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_details);
        TabHost tabHost = findViewById(R.id.tabHostStatsLore);
        tabHost.setup();


        // Retrieve the character selection sent by the main activity as part of the "intent"
        Intent intent = getIntent();
        mCharacterNum = intent.getIntExtra(simultanea.CHARACTER_KEY, mCharacterNum);
        Character character = CharacterPool.charactersList[mCharacterNum];

        ImageView imageViewCharacter = findViewById(R.id.imageViewCharacter);
        imageViewCharacter.setImageResource(character.getImageResource());
        TextView textViewCharacterName = findViewById(R.id.textViewCharacterName);
        textViewCharacterName.setText(character.getName());
        TextView textViewStats = findViewById(R.id.textViewStats);
        textViewStats.setText("TODO: stats about character " + character.getName() + ".");
        TextView textViewLore = findViewById(R.id.textViewLore);
        textViewLore.setText("TODO: describe character " + character.getName() + "'s lore.");
    }

    public void onClickPick(View v) {
        // Return the character selection back to the main activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(simultanea.CHARACTER_KEY, mCharacterNum);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

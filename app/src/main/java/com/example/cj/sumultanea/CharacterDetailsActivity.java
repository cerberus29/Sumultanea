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
    private static final String TAG_STATS = "stats";
    private static final String TAG_LORE = "lore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_details);
        // Setup the tabs
        TabHost tabHost = findViewById(R.id.tabHostStatsLore);
        tabHost.setup();
        TabHost.TabSpec spec = tabHost.newTabSpec(TAG_STATS);
        spec.setContent(R.id.tabStats);
        spec.setIndicator("Stats");
        tabHost.addTab(spec);
        spec = tabHost.newTabSpec(TAG_LORE);
        spec.setContent(R.id.tabLore);
        spec.setIndicator("Lore");
        tabHost.addTab(spec);

        // Retrieve the character selection sent by the main activity as part of the "intent"
        Intent intent = getIntent();
        mCharacterNum = intent.getIntExtra(simultanea.CHARACTER_KEY, mCharacterNum);
        Character character = CharacterPool.charactersList[mCharacterNum];

        ImageView imageViewCharacter = findViewById(R.id.imageViewCharacter);
        imageViewCharacter.setImageResource(character.getImageResource());
        TextView textViewCharacterName = findViewById(R.id.textViewCharacterName);
        textViewCharacterName.setText(character.getStringResourceName());
        TextView textViewStats = findViewById(R.id.textViewStats);
        String msg_stats = String.format(getString(R.string.msg_stats),
                character.getHealth(), character.getRecovery(),
                character.getAttack(), character.getDefense());
        textViewStats.setText(msg_stats);
        TextView textViewLore = findViewById(R.id.textViewLore);
        textViewLore.setText(character.getStringResourceLore());
    }

    public void onClickPick(View v) {
        // Return the character selection back to the main activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(simultanea.CHARACTER_KEY, mCharacterNum);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

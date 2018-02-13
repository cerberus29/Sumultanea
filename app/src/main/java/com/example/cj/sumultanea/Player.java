package com.example.cj.sumultanea;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.content.ContextCompat;

public class Player {
    public AnimationDrawable animation;
    public int lives;
    public String name;
    public Character mCharacter;
    public String endpointId;
    public Player(Context context, int character, String name) {
        this.name = name;
        lives = 3;
        mCharacter = CharacterPool.charactersList[character];
        animation = (AnimationDrawable) ContextCompat.getDrawable(context,mCharacter.getImageResource());
    }
}
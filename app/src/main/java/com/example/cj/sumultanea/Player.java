package com.example.cj.sumultanea;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;

public class Player {
    public AnimationDrawable animation;
    public int lives;
    public String name;
    public Player(Context context, int character, String name) {
        this.name = name;
        lives = 3;
        int resId = CharacterPool.charactersList[character].getImageResource();
        animation = (AnimationDrawable) context.getDrawable(resId);
    }
}
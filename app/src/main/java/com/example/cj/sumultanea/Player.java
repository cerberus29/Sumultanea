package com.example.cj.sumultanea;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;

import static com.example.cj.sumultanea.simultanea.CHARACTER1;
import static com.example.cj.sumultanea.simultanea.CHARACTER2;
import static com.example.cj.sumultanea.simultanea.CHARACTER3;
import static com.example.cj.sumultanea.simultanea.CHARACTER4;

public class Player {
    public AnimationDrawable animation;
    public int lives;
    public Player(Context context, int character) {
        Resources res = context.getResources();
        lives = 3;
        switch (character) {
            case CHARACTER1:
                animation = (AnimationDrawable)res.getDrawable(R.drawable.bluepanda);
                break;
            case CHARACTER2:
                animation = (AnimationDrawable)res.getDrawable(R.drawable.duel);
                break;
            case CHARACTER3:
                animation = (AnimationDrawable)res.getDrawable(R.drawable.pump);
                break;
            case CHARACTER4:
                animation = (AnimationDrawable)res.getDrawable(R.drawable.war);
                break;
        }
    }
}
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

    private AnimationDrawable animation;
    private Drawable frame;
    private int currentFrame = 0;
    private int updatesSinceLastFrame = 0;
    private int x = 0, y = 0;
    private int dx = 1, dy = 0;

    public Player(Context context, int character) {
        Resources res = context.getResources();
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

    public void update() {
        x += dx;
        y += dy;
        if (x < 0 || x > 500) {
            x = 0;
        }
        if (y < 0 || y > 500) {
            y = 0;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (++updatesSinceLastFrame > 10) {
            if (++currentFrame >= animation.getNumberOfFrames()) {
                currentFrame = 0;
            }
            updatesSinceLastFrame = 0;
        }
        frame = animation.getFrame(currentFrame);
        frame.setBounds(x, y, x+200, y+200);
        frame.draw(canvas);
    }
}
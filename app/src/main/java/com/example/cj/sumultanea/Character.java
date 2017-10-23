package com.example.cj.sumultanea;

public class Character {
    private final int mImageResource;
    private final int mImageResourceHurt;
    private final int mImageResourceDeath;
    private final int mImageResourceAttack;
    private final String mName;

    Character(String name, int imgRes, int imgResHurt, int imgResDeath, int imgResAttack) {
        mName = name;
        mImageResource = imgRes;
        mImageResourceHurt = imgResHurt;
        mImageResourceDeath = imgResDeath;
        mImageResourceAttack = imgResAttack;
    }

    int getImageResource() {
        return mImageResource;
    }
    int getImageResourceHurt() {
        return mImageResourceHurt;
    }
    int getImageResourceDeath() {
        return mImageResourceDeath;
    }
    int getImageResourceAttack() {
        return mImageResourceAttack;
    }

    String getName() {
        return mName;
    }
}

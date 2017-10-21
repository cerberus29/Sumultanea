package com.example.cj.sumultanea;

public class Character {
    private final int mImageResource;
    private final int mImageResourceHurt;
    private final int mImageResourceDeath;
    private final String mName;

    Character(String name, int imgRes, int imgResHurt, int imgResDeath) {
        mName = name;
        mImageResource = imgRes;
        mImageResourceHurt = imgResHurt;
        mImageResourceDeath = imgResDeath;
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

    String getName() {
        return mName;
    }
}

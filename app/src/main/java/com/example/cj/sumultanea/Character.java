package com.example.cj.sumultanea;

public class Character {
    private final int mImageResource;
    private final String mName;

    Character(String name, int imageResource) {
        mName = name;
        mImageResource = imageResource;
    }

    int getImageResource() {
        return mImageResource;
    }

    String getName() {
        return mName;
    }
}

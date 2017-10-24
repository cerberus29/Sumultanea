package com.example.cj.sumultanea;

public class Character {
    private final int mStringResourceName;
    private final int mImageResource;
    private final int mImageResourceHurt;
    private final int mImageResourceDeath;
    private final int mImageResourceAttack;
    // TODO: add one more int for the lore string resource (like I did for the name)

    Character(int strResName,
              int imgRes,
              int imgResHurt,
              int imgResDeath,
              int imgResAttack /*, <-- don't forget the comma
               todo: add one more parameter to receive the lore string resource */) {
        mStringResourceName = strResName;
        mImageResource = imgRes;
        mImageResourceHurt = imgResHurt;
        mImageResourceDeath = imgResDeath;
        mImageResourceAttack = imgResAttack;
        // todo: save the lore resource parameter into the variable
    }

    int getStringResourceName() {
        return mStringResourceName;
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
    // todo: add getStringResourceLore ...
}

package com.example.cj.sumultanea;

public class Character {
    private final int mStringResourceName;
    private final int mImageResource;
    private final int mImageResourceHurt;
    private final int mImageResourceDeath;
    private final int mImageResourceAttack;
    private final int mStringResourceLore;
    private final int mHealth, mRecovery, mAttack, mDefense;

    Character(int strResName,
              int imgRes,
              int imgResHurt,
              int imgResDeath,
              int imgResAttack,
              int strResLore,
              int health, int recovery, int attack, int defense) {
        mStringResourceName = strResName;
        mImageResource = imgRes;
        mImageResourceHurt = imgResHurt;
        mImageResourceDeath = imgResDeath;
        mImageResourceAttack = imgResAttack;
        mStringResourceLore = strResLore;
        mHealth = health;
        mRecovery = recovery;
        mAttack = attack;
        mDefense = defense;
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
    int getStringResourceLore() {
        return mStringResourceLore;
    }
    int getHealth() {
        return mHealth;
    }
    int getRecovery() {
        return mRecovery;
    }
    int getAttack() {
        return mAttack;
    }
    int getDefense() {
        return mDefense;
    }
}

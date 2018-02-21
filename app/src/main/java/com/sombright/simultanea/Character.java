package com.sombright.simultanea;

import android.content.Context;

class Character {
    private final int mStringResourceName;
    private final int mImageResource;
    private final int mImageResourceHurt;
    private final int mDeathAnimationId;
    private final int mDeadImageId;
    private final int mImageResourceAttack;
    private final int mStringResourceLore;
    private final int mHeal, mRecovery, mAttack, mDefense;

    Character(int strResName,
              int imgRes,
              int imgResHurt,
              int deathAnimationId, int deadImageId,
              int imgResAttack,
              int strResLore,
              int heal, int recovery, int attack, int defense) {
        mStringResourceName = strResName;
        mImageResource = imgRes;
        mImageResourceHurt = imgResHurt;
        mDeathAnimationId = deathAnimationId;
        mDeadImageId = deadImageId;
        mImageResourceAttack = imgResAttack;
        mStringResourceLore = strResLore;
        mHeal = heal;
        mRecovery = recovery;
        mAttack = attack;
        mDefense = defense;
    }

    int getStringResourceName() {
        return mStringResourceName;
    }

    private static String getCharacterName(Context context, Character character) {
        int resId = character.getStringResourceName();
        return context.getString(resId);
    }

    public String getName(Context context) {
        return getCharacterName(context, this);
    }

    int getImageResource() {
        return mImageResource;
    }
    int getImageResourceHurt() {
        return mImageResourceHurt;
    }
    int getDeathAnimationId() {
        return mDeathAnimationId;
    }
    int getDeadImageId() {
        return mDeadImageId;
    }
    int getImageResourceAttack() {
        return mImageResourceAttack;
    }
    int getStringResourceLore() {
        return mStringResourceLore;
    }
    int getHeal() {
        return mHeal;
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

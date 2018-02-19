package com.sombright.simultanea;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class Player {
    private static final String TAG = "Player";
    private final Context mContext;
    private String mEndpointId;
    private String mName;
    private Character mCharacter;
    private AnimationDrawable mAnimation;
    private int mHealth;
    private boolean mAnswered;

    Player(Context context) {
        mContext = context;
        mHealth = 3;
        mAnswered = false;
    }

    public String getEndpointId() {
        return mEndpointId;
    }

    public void setEndpointId(String endpointId) {
        mEndpointId = endpointId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public Character getCharacter() {
        return mCharacter;
    }

    public void setCharacter(String name) {
        for (Character character : CharacterPool.charactersList) {
            if (character.getName(mContext).equals(name)) {
                mCharacter = character;
                return;
            }
        }
        if (mCharacter == null) {
            Log.e(TAG, "Unknown character " + name + ". Using default instead.");
            mCharacter = CharacterPool.getDefaultCharacter();
        }
    }

    public String getCharacterName() {
        return mCharacter.getName(mContext);
    }

    int getHealth() {
        return mHealth;
    }

    void setHealth(int health) {
        mHealth = health;
    }

    public boolean hasAnswered() {
        return mAnswered;
    }

    public void setAnswered(boolean answered) {
        mAnswered = answered;
    }

    public AnimationDrawable getAnimation() {
        if (mAnimation == null) {
            mAnimation = (AnimationDrawable) ContextCompat.getDrawable(mContext, mCharacter.getImageResource());
        }
        return mAnimation;
    }
}
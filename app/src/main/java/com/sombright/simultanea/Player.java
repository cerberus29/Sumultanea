package com.sombright.simultanea;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Player {
    private static final String TAG = "Player";
    static final int COMBAT_MODE_NONE = 0;
    static final int COMBAT_MODE_ATTACK = 1;
    static final int COMBAT_MODE_DEFEND = 2;
    static final int COMBAT_MODE_HEAL = 3;
    private final Context mContext;
    private String mEndpointId;
    private String mName;
    private Character mCharacter;
    private AnimationDrawable mAnimation;
    private int mHealth;
    private boolean mAnswered;
    private int mCombatMode;

    Player(Context context) {
        mContext = context;
        mHealth = 100;
        mAnswered = false;
        mCombatMode = COMBAT_MODE_NONE;
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

    int getCombatMode() {
        return mCombatMode;
    }

    void setCombatMode(int mode) {
        switch (mode) {
            case COMBAT_MODE_ATTACK:
                mCombatMode = mode;
                break;
            case COMBAT_MODE_DEFEND:
                mCombatMode = mode;
                break;
            case COMBAT_MODE_HEAL:
                mCombatMode = mode;
                mHealth = min(mHealth + mCharacter.getHeal(), 100);
                break;
            default:
                mCombatMode = COMBAT_MODE_NONE;
        }
    }

    boolean isFake() {
        return mEndpointId == null || mEndpointId.isEmpty();
    }

    void attack(Player opponent) {
        int damage = mCharacter.getAttack() - opponent.getCharacter().getDefense();
        if (opponent.getCombatMode() == COMBAT_MODE_DEFEND) {
            damage = damage / 2;
        }
        Log.d(TAG, "Player " + mName + " attacking " + opponent.mCharacter.getName(mContext) + " with damage=" + damage + " on health=" + opponent.getHealth());
        opponent.setHealth(max(0, opponent.getHealth() - damage));
    }
}
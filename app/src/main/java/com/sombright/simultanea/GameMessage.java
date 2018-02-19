package com.sombright.simultanea;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

class GameMessage {
    private static final String TAG = "GameMessage";

    /**
     * This is the version of the messaging API.
     * It is used to make sure both devices talk the same language.
     * We must increment it every time we change the structure of the messages.
     */
    private static final int GAME_MESSAGE_API_VERSION = 0;

    static final int GAME_MESSAGE_TYPE_PLAYER_INFO = 1;
    static final int GAME_MESSAGE_TYPE_QUESTION = 2;
    static final int GAME_MESSAGE_TYPE_ATTACK = 3;
    private static final String KEY_VERSION = "version";
    private static final String KEY_TYPE = "type";

    private int mType;
    private QuizPool.Entry mQuestion;

    static class MessageInfo {
        int version = GAME_MESSAGE_API_VERSION;
        int type;

        MessageInfo(int type) {
            this.type = type;
        }
    }

    static class PlayerInfo extends MessageInfo {
        String name;
        String character;
        int health;
        int battle;
        PlayerInfo() {
            super(GAME_MESSAGE_TYPE_PLAYER_INFO);
        }
    }
    PlayerInfo playerInfo;

    static class AttackInfo extends MessageInfo {
        String victim;
        AttackInfo() {
            super(GAME_MESSAGE_TYPE_ATTACK);
        }
    }
    AttackInfo attackInfo;

    /**
     * Parse a message into a GameMessage object, according to the API version.
     *
     * @param bytes the raw byte array received.
     * @return a valid GameMessage object, or null if the message couldn't be parsed.
     */
    static GameMessage fromBytes(byte[] bytes) {
        int version;
        int type;

        String jsonString = new String(bytes, UTF_8);
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            version = jsonObject.getInt(KEY_VERSION);
            type = jsonObject.getInt(KEY_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        if (version > GAME_MESSAGE_API_VERSION) {
            Log.e(TAG, "Sorry, we can't parse this message");
            return null;
        }

        GameMessage msg = new GameMessage();
        msg.setType(type);
        Gson gson = new Gson();
        switch (type) {
            case GAME_MESSAGE_TYPE_PLAYER_INFO:
                msg.playerInfo = gson.fromJson(jsonString, PlayerInfo.class);
                return msg;
            case GAME_MESSAGE_TYPE_ATTACK:
                msg.attackInfo = gson.fromJson(jsonString, AttackInfo.class);
                return msg;
        }
        return null;
    }

    byte[] toBytes() {
        String jsonString;
        Gson gson = new Gson();
        switch (mType) {
            case GAME_MESSAGE_TYPE_PLAYER_INFO:
                jsonString = gson.toJson(playerInfo);
                break;
            case GAME_MESSAGE_TYPE_ATTACK:
                jsonString = gson.toJson(attackInfo);
                break;
            default:
                return null;
        }
        return jsonString.getBytes(UTF_8);
    }


    int getType() {
        return mType;
    }

    void setType(int type) {
        mType = type;
        switch (type) {
            case GAME_MESSAGE_TYPE_PLAYER_INFO:
                playerInfo = new PlayerInfo();
                break;
            case GAME_MESSAGE_TYPE_ATTACK:
                attackInfo = new AttackInfo();
                break;
        }
    }

    QuizPool.Entry getQuestion() {
        return mQuestion;
    }

    void setQuestion(QuizPool.Entry question) {
        mQuestion = question;
    }
}

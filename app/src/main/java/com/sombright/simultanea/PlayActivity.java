package com.sombright.simultanea;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.sombright.simultanea.MainActivity.TAG;

public class PlayActivity extends ConnectionsActivity implements View.OnClickListener, PlayersViewAdapter.OnClickPlayerListener {

    private final static int STATE_WAITING_FOR_PLAYERS = 1;
    private final static int STATE_WAITING_FOR_QUESTION = 2;
    private final static int STATE_WAITING_FOR_ANSWER = 3;

    private final static int MESSAGE_DURATION_MS = 1500;
    private final static int LONG_MESSAGE_DURATION_MS = 3000;
    private final static int MAX_PLAYERS = 10;
    /*
     * Multiplayer connection code based on the rockpaperscissors example from:
     * https://github.com/googlesamples/android-nearby
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String SERVICE_ID = PlayActivity.class.getName();
    TextView questionText;
    private Random random = new Random();
    private LinearLayout answersLayout;
    private LinearLayout battleOptionsLayout;
    private Player me;
    private PlayersViewAdapter mPlayersViewAdapter;
    private GridView otherPlayersLayout;
    private TextView textViewLocalPlayer;
    private ProgressBar localPlayerHealth;
    private ImageView localPlayerThumb;
    private ImageView localPlayerAnimation, otherPlayerAnimation;
    private Button buttonStartGame, buttonQuestion, buttonAnswers, buttonBattle;
    private Button buttonHeal, buttonAttack, buttonDefend;
    private Handler handler = new Handler();
    private MediaPlayer mMusic;
    private Animation fadeOutAnimation;
    // Some variables we need to keep for the zoom-out animation
    private ImageView mOtherPlayerThumb;
    private Player mOtherPlayer;
    private Runnable mDoThisAfterAnimation;
    private boolean mWinBattle;
    // Translation and scale factors
    private PointF localPlayerStartScale = new PointF();
    private PointF otherPlayerStartScale = new PointF();
    private PointF localPlayerStartTranslation = new PointF();
    private PointF otherPlayerStartTranslation = new PointF();
    private int mShortAnimationDuration = 500;
    private Player victim = null;
    private LinearLayout victim_container = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Code for hiding the status bar from
         * https://developer.android.com/training/system-ui/status.html
         */
        // If the Android version is lower than Jellybean, use this call to hide
        // the status bar.
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if (actionBar != null)
                actionBar.hide();
        }

        PreferencesProxy prefs = new PreferencesProxy(this);

        // Retrieve a copy of the settings (cannot change during the game)
        String name = prefs.getMultiPlayerAlias();

        me = new Player(this);
        me.setName(name);
        me.setCharacter(prefs.getCharacter());
        Log.d(TAG, "Playing as character " + me.getCharacter());

        // Apply the initial layout (we will modify below)
        setContentView(R.layout.activity_play);

        // These are the portions of the layout that we will modify during the game
        questionText = findViewById(R.id.questionTextView);
        answersLayout = findViewById(R.id.answersLayout);
        battleOptionsLayout = findViewById(R.id.battleOptionsLayout);
        otherPlayersLayout = findViewById(R.id.otherPlayersLayout);
        textViewLocalPlayer = findViewById(R.id.textViewLocalPlayer);
        localPlayerAnimation = findViewById(R.id.localPlayerAnimation);
        otherPlayerAnimation = findViewById(R.id.otherPlayerAnimation);

        buttonStartGame = findViewById(R.id.buttonStartGame);
        buttonQuestion = findViewById(R.id.buttonQuestion);
        buttonAnswers = findViewById(R.id.buttonAnswers);
        buttonBattle = findViewById(R.id.buttonBattle);

        buttonHeal = findViewById(R.id.buttonHeal);
        buttonAttack = findViewById(R.id.buttonAttack);
        buttonDefend = findViewById(R.id.buttonDefend);

        localPlayerThumb = findViewById(R.id.imageViewLocalPlayer);
        localPlayerHealth = findViewById(R.id.localPlayerHealth);
        localPlayerHealth.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

        buttonQuestion.setEnabled(false);
        buttonAnswers.setEnabled(true);
        buttonBattle.setEnabled(true);

        updateLocalPlayerUi();

        // Prepare an animation object for when we loose a life
        fadeOutAnimation = new AlphaAnimation(1, 0);
        fadeOutAnimation.setDuration(MESSAGE_DURATION_MS / 6);
        fadeOutAnimation.setInterpolator(new LinearInterpolator());
        fadeOutAnimation.setRepeatCount(Animation.INFINITE);
        fadeOutAnimation.setRepeatMode(Animation.REVERSE);
        // Remove the fake content we put in the initial layout (for designing)
        mPlayersViewAdapter = new PlayersViewAdapter(this, R.layout.players_example_item, this);

        questionText.setText(R.string.waiting_for_master);
        answersLayout.removeAllViews();
        startDiscovering();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
        // Background music
        mMusic = MediaPlayer.create(this, R.raw.fight2);
        mMusic.setLooping(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mMusic.start();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        mMusic.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mMusic.stop();
        mMusic.release();
        stopAllEndpoints();
        super.onStop();
    }
/*
    public void addFakePlayer(View v) {
        Log.d(TAG, "addFakePlayer");
        // Find a unique name
        String characterName = null, playerName = null;
        int num = 0; // Add a number when the name already exists
        Player player = null;
        do {
            num++;
            for (Character character: CharacterPool.charactersList) {
                characterName = character.getName(PlayActivity.this);
                playerName = characterName;
                if (num > 1) {
                    playerName += " " + num;
                }
                player = getPlayerByName(playerName);
                if (player == null) {
                    break;
                }
            }
        } while (player != null);
        addPlayer("", characterName, playerName);
    }
*/
    private void addPlayer(Endpoint endpoint, String character, String name) {
        Log.d(TAG, "addPlayer");
        Player player = new Player(this);
        player.setEndpoint(endpoint);
        player.setCharacter(character);
        player.setName(name);
        mPlayersViewAdapter.add(player);
    }

    private void updateLocalPlayerUi() {
        Log.d(TAG, "updateLocalPlayerUi");
        // Set our character image
        AnimationDrawable animationDrawable = me.getAnimation();
        if (localPlayerThumb.getDrawable() != animationDrawable) {
            localPlayerThumb.setImageDrawable(animationDrawable);
            animationDrawable.start();
        }
        if (!textViewLocalPlayer.getText().equals(me.getName())) {
            textViewLocalPlayer.setText(me.getName());
        }
        if (localPlayerHealth.getProgress() != me.getHealth()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                localPlayerHealth.setProgress(me.getHealth(), true);
            } else {
                localPlayerHealth.setProgress(me.getHealth());
            }
        }
    }

    // Go through a sub-tree of views and call setEnabled on every leaf (views)
    private void recursiveSetEnabled(boolean enable, ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                recursiveSetEnabled(enable, (ViewGroup) child);
            }
        }
    }

    public void onClickStartGame(View view) {
        Log.d(TAG, "onClickStartGame");
        stopDiscovering();
    }

    /**
     * Functions to change the content of the bottom-right box
     */
    public void showQuestion(View view) {
        Log.d(TAG, "showQuestion");
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        questionText.setVisibility(View.VISIBLE);
        questionText.bringToFront();
        buttonQuestion.setEnabled(false);
        buttonAnswers.setEnabled(true);
        buttonBattle.setEnabled(true);
        abortCombatMode();
    }

    public void abortCombatMode() {
        if (me.getCombatMode() != Player.COMBAT_MODE_ATTACK) {
            mPlayersViewAdapter.setClickable(false);
            buttonAttack.setEnabled(true);
            buttonDefend.setEnabled(true);
            buttonHeal.setEnabled(true);
            me.setCombatMode(Player.COMBAT_MODE_NONE);
        }
    }
    public void showAnswers(View view) {
        Log.d(TAG, "showAnswers");
        questionText.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.VISIBLE);
        answersLayout.bringToFront();
        buttonQuestion.setEnabled(true);
        buttonAnswers.setEnabled(false);
        buttonBattle.setEnabled(true);
        abortCombatMode();
    }

    public void showBattleOptions(View view) {
        Log.d(TAG, "showBattleOptions");
        questionText.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.VISIBLE);
        battleOptionsLayout.bringToFront();
        buttonQuestion.setEnabled(true);
        buttonAnswers.setEnabled(true);
        buttonBattle.setEnabled(false);
    }


    public void buttonHealClicked(View view) {
        buttonHeal.setEnabled(false);
        buttonAttack.setEnabled(false);
        buttonDefend.setEnabled(false);
        me.setCombatMode(Player.COMBAT_MODE_HEAL);
        updateLocalPlayerUi();
        sendPlayerDetails();
        int cooldown = me.getCharacter().getRecovery() * 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (me.getHealth() == 0) {
                    return;
                }
                me.setCombatMode(Player.COMBAT_MODE_NONE);
                updateLocalPlayerUi();
                sendPlayerDetails();
                buttonHeal.setEnabled(true);
                buttonAttack.setEnabled(true);
                buttonDefend.setEnabled(true);
            }
        }, cooldown);
    }

    public void buttonDefendClicked(View view) {
        buttonHeal.setEnabled(false);
        buttonAttack.setEnabled(false);
        buttonDefend.setEnabled(false);
        me.setCombatMode(Player.COMBAT_MODE_DEFEND);
        updateLocalPlayerUi();
        sendPlayerDetails();
        int cooldown = me.getCharacter().getRecovery() * 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (me.getHealth() == 0) {
                    return;
                }
                me.setCombatMode(Player.COMBAT_MODE_NONE);
                updateLocalPlayerUi();
                sendPlayerDetails();
                buttonHeal.setEnabled(true);
                buttonAttack.setEnabled(true);
                buttonDefend.setEnabled(true);
            }
        }, cooldown);
    }

    public void buttonAttackClicked(View view) {
        buttonHeal.setEnabled(false);
        buttonAttack.setEnabled(false);
        buttonDefend.setEnabled(false);
        me.setCombatMode(Player.COMBAT_MODE_ATTACK);
        mPlayersViewAdapter.setClickable(true);
        updateLocalPlayerUi();
        sendPlayerDetails();
    }

    /**
     * Zoom-in animation:
     * - hide the small thumbnail
     * - calculate the offset and scaling required to shrink the big animation image exactly over the thumbnail
     * - animate from the thumbnail position to the full-size animation position.
     *
     * @param otherPlayerThumb:     small image of the other player.
     * @param otherPlayer:          other player data.
     * @param doThisAfterAnimation: what to run when the animation is finished.
     * @param winBattle:            true if I (local player) am the victor of the battle.
     */
    private void zoomImageFromThumb(ImageView otherPlayerThumb, Player otherPlayer, Runnable doThisAfterAnimation, boolean winBattle) {
        // Save the values, because we will need them in the zoom-out animation
        mOtherPlayerThumb = otherPlayerThumb;
        mOtherPlayer = otherPlayer;
        mDoThisAfterAnimation = doThisAfterAnimation;
        mWinBattle = winBattle;

        // Load the big zoom-in images (same as the small thumbnail images for now).
        otherPlayerAnimation.setImageDrawable(otherPlayerThumb.getDrawable());
        localPlayerAnimation.setImageDrawable(localPlayerThumb.getDrawable());

        // Calculate the starting and ending bounds for the zoomed-in image.
        Rect startBounds = new Rect();
        Rect finalBounds = new Rect();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the zoomed-in image.
        mOtherPlayerThumb.getGlobalVisibleRect(startBounds);
        otherPlayerAnimation.getGlobalVisibleRect(finalBounds);
        // Calculate the transformations needed to make the big animation fit exactly on top of the thumbnail:
        otherPlayerStartTranslation.x = startBounds.left - finalBounds.left;
        otherPlayerStartTranslation.y = startBounds.top - finalBounds.top;
        otherPlayerStartScale.x = (float) startBounds.width() / finalBounds.width();
        otherPlayerStartScale.y = (float) startBounds.height() / finalBounds.height();

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnails.
        mOtherPlayerThumb.setAlpha(0f);

        // Apply translation and scale to cover the thumbnail
        otherPlayerAnimation.setTranslationX(otherPlayerStartTranslation.x);
        otherPlayerAnimation.setTranslationY(otherPlayerStartTranslation.y);
        otherPlayerAnimation.setScaleX(otherPlayerStartScale.x);
        otherPlayerAnimation.setScaleY(otherPlayerStartScale.y);
        otherPlayerAnimation.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        otherPlayerAnimation.setPivotX(0f);
        otherPlayerAnimation.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties.
        ViewPropertyAnimator anim = otherPlayerAnimation.animate();
        anim.translationX(0f);
        anim.translationY(0f);
        anim.scaleX(1f);
        anim.scaleY(1f);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();

        // Do the same thing for the local player animation
        localPlayerThumb.getGlobalVisibleRect(startBounds);
        localPlayerAnimation.getGlobalVisibleRect(finalBounds);
        localPlayerStartTranslation.x = startBounds.left - finalBounds.left;
        localPlayerStartTranslation.y = startBounds.top - finalBounds.top;
        localPlayerStartScale.x = (float) startBounds.width() / finalBounds.width();
        localPlayerStartScale.y = (float) startBounds.height() / finalBounds.height();
        localPlayerThumb.setAlpha(0f);
        localPlayerAnimation.setTranslationX(localPlayerStartTranslation.x);
        localPlayerAnimation.setTranslationY(localPlayerStartTranslation.y);
        localPlayerAnimation.setScaleX(localPlayerStartScale.x);
        localPlayerAnimation.setScaleY(localPlayerStartScale.y);
        localPlayerAnimation.setVisibility(View.VISIBLE);
        localPlayerAnimation.setPivotX(0f);
        localPlayerAnimation.setPivotY(0f);
        anim = localPlayerAnimation.animate();
        anim.translationX(0f);
        anim.translationY(0f);
        anim.scaleX(1f);
        anim.scaleY(1f);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        // Schedule the next step at the end of the zoom-in animation
        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                doCharacterAnimation();
            }
        });
        anim.start();
    }

    /**
     * Battle animations
     * The attacker and the victim images are now fully zoomed-in.
     * Time to play the attack and the hurt/death animations.
     */
    private void doCharacterAnimation() {
        // Start the battle animations now
        Player victim, victor;
        ImageView victimView, victorView;
        if (mWinBattle) {
            victim = mOtherPlayer;
            victimView = otherPlayerAnimation;
            victor = me;
            victorView = localPlayerAnimation;
        } else {
            victim = me;
            victor = mOtherPlayer;
            victimView = localPlayerAnimation;
            victorView = otherPlayerAnimation;
        }
        // Victim: select between hurt and death animations
        int resId;
        if (victim.getHealth() > 0) {
            resId = victim.getCharacter().getImageResourceHurt();
        } else {
            resId = victim.getCharacter().getDeathAnimationId();
        }
        victimView.setImageResource(resId);
        AnimationDrawable anim = (AnimationDrawable) victimView.getDrawable();
        anim.start();
        // Victor: select attack animation
        victorView.setImageResource(victor.getCharacter().getImageResourceAttack());
        anim = (AnimationDrawable) victorView.getDrawable();
        anim.start();
        // Let the hurt/death animation run for a certain time before zooming out
        if (victim.getHealth() > 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    zoomImageBackToThumb();
                }
            }, 1000);
        } else {
            // Death animation takes a bit longer
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    zoomImageBackToThumb();
                }
            }, 2000);
        }
    }

    /**
     * Zoom-out animation:
     * - Shrink the animation images back to the thumbnail position
     */
    private void zoomImageBackToThumb() {
        // Stop the hurt or death animation
        AnimationDrawable animationDrawable = (AnimationDrawable) otherPlayerAnimation.getDrawable();
        animationDrawable.stop();
        // Zoom-out with the dead image or normal image
        if (mOtherPlayer.getHealth() == 0)
            otherPlayerAnimation.setImageResource(mOtherPlayer.getCharacter().getDeadImageId());
        else
            otherPlayerAnimation.setImageDrawable(mOtherPlayerThumb.getDrawable());

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        ViewPropertyAnimator anim = otherPlayerAnimation.animate();
        anim.translationX(otherPlayerStartTranslation.x);
        anim.translationY(otherPlayerStartTranslation.y);
        anim.scaleX(otherPlayerStartScale.x);
        anim.scaleY(otherPlayerStartScale.y);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();

        // Same for local player
        animationDrawable = (AnimationDrawable) localPlayerAnimation.getDrawable();
        animationDrawable.stop();
        if (me.getHealth() == 0)
            localPlayerAnimation.setImageResource(me.getCharacter().getDeadImageId());
        else
            localPlayerAnimation.setImageDrawable(localPlayerThumb.getDrawable());
        anim = localPlayerAnimation.animate();
        anim.translationX(localPlayerStartTranslation.x);
        anim.translationY(localPlayerStartTranslation.y);
        anim.scaleX(localPlayerStartScale.x);
        anim.scaleY(localPlayerStartScale.y);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        // Next step...
        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                finishAnimation();
            }
        });
        anim.start();
    }

    /**
     * After the zoom-out animation, we clean-up and re-enable the thumbnail.
     */
    private void finishAnimation() {
        // Re-enable thumb view with the same image as the zoom-out animation
        mOtherPlayerThumb.setImageDrawable(otherPlayerAnimation.getDrawable());
        mOtherPlayerThumb.setAlpha(1f);
        otherPlayerAnimation.setVisibility(View.INVISIBLE);
        otherPlayerAnimation.setTranslationX(0f);
        otherPlayerAnimation.setTranslationY(0f);
        otherPlayerAnimation.setScaleX(1f);
        otherPlayerAnimation.setScaleY(1f);
        localPlayerThumb.setImageDrawable(localPlayerAnimation.getDrawable());
        localPlayerThumb.setAlpha(1f);
        localPlayerAnimation.setVisibility(View.INVISIBLE);
        localPlayerAnimation.setTranslationX(0f);
        localPlayerAnimation.setTranslationY(0f);
        localPlayerAnimation.setScaleX(1f);
        localPlayerAnimation.setScaleY(1f);
        // Next step...
        handler.post(mDoThisAfterAnimation);
    }

    private void announceDamage() {
        if (me.getHealth() == 0) {
            questionText.setText(R.string.game_over);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishGame();
                }
            }, MESSAGE_DURATION_MS);
        }
    }

    private void finishGame() {
        mMusic.stop();
        finish();
    }

    @Override
    public void onClick(View v) {
        // Check if it was a player button
        Player player = (Player) v.getTag(R.id.id_player);
        if (player != null) {
            mPlayersViewAdapter.setClickable(false);
            GameMessage msg = new GameMessage();
            msg.setType(GameMessage.GAME_MESSAGE_TYPE_ATTACK);
            msg.attackInfo.victim = player.getName();
            broadcastMessage(msg);
            // TODO startAttackAnimation(player);
            buttonHeal.setEnabled(true);
            buttonAttack.setEnabled(true);
            buttonDefend.setEnabled(true);
            return;
        }

        // Check if it was an answer button
        QuizPool.Answer answer = (QuizPool.Answer) v.getTag(R.id.id_answer);
        if (answer != null) {
            // First thing: prevent user from clicking other answers while we handle this one.
            recursiveSetEnabled(false, answersLayout);

            if (answer.correct) {
                Log.d(TAG, "Correct!");
                questionText.setText(R.string.answer_correct);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.GREEN, 150));
            } else {
                Log.d(TAG, "Incorrect!");
                questionText.setText(R.string.answer_incorrect);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.RED, 150));
            }
            GameMessage msg = new GameMessage();
            msg.setType(GameMessage.GAME_MESSAGE_TYPE_ANSWER);
            msg.answerInfo.correct = answer.correct;
            send(Payload.fromBytes(msg.toBytes()));
            return;
        }

        Log.e(TAG, "Unhandled click on " + v);
    }

    private void attackOtherPlayer() {
        if (victim.getHealth() == 0) {
            String msg_without_victim = getString(R.string.attack_killed);
            String msg_with_victim = String.format(msg_without_victim, victim.getName());
            questionText.setText(msg_with_victim);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removePlayer(victim.getName());
                }
            }, MESSAGE_DURATION_MS);
        }
    }

    private void removePlayer(String name) {
        // Disable the victim
        recursiveSetEnabled(false, victim_container);
        // Check if we won
        boolean everybodyDead = true;
        for (int i = 0; i < mPlayersViewAdapter.getCount(); i++) {
            final Player player = mPlayersViewAdapter.getItem(i);
            if (player == null)
                continue;
            if (player.getHealth() != 0) {
                everybodyDead = false;
                MediaPlayer mediaPlayer = MediaPlayer.create(PlayActivity.this, R.raw.fail);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.release();
                        mMusic.setVolume(1.0f, 1.0f);
                    }
                });
                mMusic.setVolume(1.0f, 0.5f);
                mediaPlayer.start();
                break; // no need to continue, we have at least 1 opponent alive.

            }
        }
        if (everybodyDead) {
            questionText.setText(R.string.game_won);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishGame();
                }
            }, LONG_MESSAGE_DURATION_MS);
        }
    }

    /**
     * Process a message received from another device.
     *
     * @param endpoint the sender
     * @param payload  the message
     */
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() != Payload.Type.BYTES) {
            Log.e(TAG, "Cannot handle messages of type " + payload.getType());
            return;
        }
        GameMessage msg = GameMessage.fromBytes(payload.asBytes());
        if (msg == null) {
            Log.e(TAG, "Cannot parse messages from endpoint " + endpoint.getName());
            cleanupAndDisconnectEndpoint(endpoint);
            return;
        }
        switch (msg.getType()) {
            case GameMessage.GAME_MESSAGE_TYPE_PLAYER_INFO:
                onReceivePlayerInfo(endpoint, msg);
                break;
            case GameMessage.GAME_MESSAGE_TYPE_ATTACK:
                //onReceiveAttackInfo(endpoint, msg);
                break;
            case GameMessage.GAME_MESSAGE_TYPE_QUESTION:
                onReceiveQuestion(msg);
                break;
            default:
                Log.wtf(TAG, "Unhandled game message type: " + msg.getType());
        }
    }

    private void onReceivePlayerInfo(Endpoint endpoint, GameMessage msg) {
        Log.d(TAG, "onReceivePlayerInfo");
        Player player;

        if (msg.playerInfo.uniqueId.equals(me.getUniqueID())) {
            player = me;
            player.setPlayerDetails(msg);
            updateLocalPlayerUi();
            if (me.getPoints() == 0) {
                //TODO disable attack?
            }
        } else {
            player = mPlayersViewAdapter.getPlayer(msg.playerInfo.endpointId);
            // The first time we receive a player's info, we add it to the UI
            if (player == null) {
                player = new Player(this);
                player.setPlayerDetails(msg);
                mPlayersViewAdapter.add(player);
            } else {
                player.setPlayerDetails(msg);
                mPlayersViewAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onReceiveQuestion(GameMessage msg) {
        Log.d(TAG, "onReceiveQuestion");
        QuizPool.Entry entry = msg.questionInfo.entry;
        questionText.setText(entry.question);

        // We clear-out the old buttons, and create new ones for the current question
        answersLayout.removeAllViews();
        for (QuizPool.Answer answer : entry.answers) {
            Button button = new Button(this);
            button.setText(answer.text);
            button.setTag(R.id.id_answer, answer);
            button.setOnClickListener(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            button.setLayoutParams(lp);
            answersLayout.addView(button);
        }
        // In case we just started the game...
        buttonQuestion.setVisibility(View.VISIBLE);
        buttonAnswers.setVisibility(View.VISIBLE);
        buttonBattle.setVisibility(View.VISIBLE);
    }

    private void broadcastMessage(GameMessage msg) {
        Log.d(TAG, "broadcastMessage");
        send(Payload.fromBytes(msg.toBytes()));
    }

    private void sendPlayerDetails() {
        Log.d(TAG, "sendPlayerDetails");
        GameMessage msg = me.getPlayerDetails();
        send(Payload.fromBytes(msg.toBytes()));
    }

    private void cleanupAndDisconnectEndpoint(Endpoint endpoint) {
        disconnect(endpoint);
        removePlayer(endpoint.getName());
    }

    // --- A bunch of functions used by the connection callbacks ----

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        Log.i(TAG, "Found taskmaster");
        // Immediately request connection and stop looking
        connectToEndpoint(endpoint);
        stopDiscovering();
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        Log.i(TAG, "onConnectionInitiated: accepting connection");
        acceptConnection(endpoint);
        questionText.setText("Connecting to taskmaster...");
        buttonStartGame.setVisibility(View.GONE);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Log.i(TAG, "onEndpointConnected: connection successful");
        // Once the connection is established, we send our player info to the taskmaster
        questionText.setText("Connected, waiting for game to begin...");
        sendPlayerDetails();
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        questionText.setText("Connection failed, but let's keep looking for taskmaster...");
        buttonStartGame.setVisibility(View.VISIBLE);
        startDiscovering();
    }

    private void onDisconnectedInternal(String endpointId) {
        finish();
    }

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            Log.wtf(TAG, "We received permission results for something we didn't request!");
            return;
        }
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        Log.wtf(TAG, "All the permissions were accepted, let's retry now.");
        recreate();
    }

    @Override
    protected String getName() {
        return me.getName();
    }

    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    protected Strategy getStrategy() {
        return null;
    }

    @Override
    public void onClickPlayer(Player player) {
        Log.v(TAG, "onClickPlayer: " + player.getName());
    }
}
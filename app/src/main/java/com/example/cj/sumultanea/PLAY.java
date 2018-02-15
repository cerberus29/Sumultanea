package com.example.cj.sumultanea;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.example.cj.sumultanea.simultanea.DEFAULT_CHARACTER;
import static com.example.cj.sumultanea.simultanea.TAG;

public class PLAY extends AppCompatActivity {

    private final static int STATE_WAITING_FOR_PLAYERS = 1;
    private final static int STATE_WAITING_FOR_QUESTION = 2;
    private final static int STATE_WAITING_FOR_ANSWER = 3;
    private int state = STATE_WAITING_FOR_PLAYERS;

    private QuizPool quizPool;
    private Random random = new Random();
    TextView questionText;
    private LinearLayout answersLayout;
    private LinearLayout battleOptionsLayout;
    private Player me;
    private List<Player> otherPlayers = new ArrayList<>();
    private LinearLayout otherPlayersLayout;
    private TextView textViewLocalPlayer;
    private LinearLayout localPlayerLivesLayout;
    private ImageView localPlayerThumb;
    private ImageView localPlayerAnimation, otherPlayerAnimation;
    private LinearLayout controlsLayout;
    private Button buttonQuestion, buttonAnswers, buttonBattle;
    private Button buttonHeal, buttonAttack, buttonDefend;
    private Handler handler = new Handler();
    private MediaPlayer ring;
    private final static int MESSAGE_DURATION_MS = 1500;
    private final static int LONG_MESSAGE_DURATION_MS = 3000;
    // Animation for when a player looses a life
    private Animation fadeOutAnimation;
    // This is to keep track of which heart is currently animated (only one at a time), so we can stop the animation later
    private ImageView fadingLifeImg = null;

    private boolean isMultiPlayerMode;
    private boolean isMultiPlayerMaster;
    private String mMultiPlayerAlias;

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

        // Retrieve the character selection sent by the main activity as part of the "intent"
        Intent intent = getIntent();
        int character = intent.getIntExtra(simultanea.CHARACTER_KEY, DEFAULT_CHARACTER);

        // Retrieve a copy of the settings (cannot change during the game)
        isMultiPlayerMode = SettingsActivity.isMultiPlayerMode(this);
        isMultiPlayerMaster = SettingsActivity.isMultiPlayerMaster(this);
        mMultiPlayerAlias = SettingsActivity.getMultiPlayerAlias(this);

        me = new Player(this, character, "Me");
        Log.d(TAG, "Playing as character " + character);

        // Apply the initial layout (we will modify below)
        setContentView(R.layout.activity_play);

        // These are the portions of the layout that we will modify during the game
        questionText = findViewById(R.id.questionTextView);
        answersLayout = findViewById(R.id.answersLayout);
        battleOptionsLayout = findViewById(R.id.battleOptionsLayout);
        otherPlayersLayout = findViewById(R.id.otherPlayersLayout);
        textViewLocalPlayer = findViewById(R.id.textViewLocalPlayer);
        localPlayerLivesLayout = findViewById(R.id.localPlayerLivesLayout);
        localPlayerAnimation = findViewById(R.id.localPlayerAnimation);
        otherPlayerAnimation = findViewById(R.id.otherPlayerAnimation);

        controlsLayout = findViewById(R.id.controlsLayout);
        buttonQuestion = findViewById(R.id.buttonQuestion);
        buttonAnswers = findViewById(R.id.buttonAnswers);
        buttonBattle = findViewById(R.id.buttonBattle);

        buttonHeal = findViewById(R.id.buttonHeal);
        buttonAttack = findViewById(R.id.buttonAttack);
        buttonDefend = findViewById(R.id.buttonDefend);

        localPlayerThumb = findViewById(R.id.imageViewLocalPlayer);

        updateControlButtons();

        // Set our character image
        localPlayerThumb.setImageDrawable(me.animation);
        me.animation.start();
        textViewLocalPlayer.setText(mMultiPlayerAlias);

        // Display our lives
        // Remove the fake content we put in the initial layout (for designing)
        localPlayerLivesLayout.removeAllViews();
        for (int i = 0; i < me.lives; i++) {
            ImageView lifeImg = new ImageView(this);
            lifeImg.setImageResource(R.drawable.heart);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(30, 30);
            lp.gravity = Gravity.CENTER;
            lifeImg.setLayoutParams(lp);
            //lifeImg.setAdjustViewBounds(true);
            localPlayerLivesLayout.addView(lifeImg);
        }
        // Prepare an animation object for when we loose a life
        fadeOutAnimation = new AlphaAnimation(1, 0);
        fadeOutAnimation.setDuration(MESSAGE_DURATION_MS / 6);
        fadeOutAnimation.setInterpolator(new LinearInterpolator());
        fadeOutAnimation.setRepeatCount(Animation.INFINITE);
        fadeOutAnimation.setRepeatMode(Animation.REVERSE);
        // Remove the fake content we put in the initial layout (for designing)
        otherPlayersLayout.removeAllViews();

        if (isMultiPlayerMode) {
            connectionsClient = Nearby.getConnectionsClient(this);
            questionText.setText("Waiting for other players...");
            answersLayout.removeAllViews();
            me.name = mMultiPlayerAlias;
            if (isMultiPlayerMaster)
                startAdvertising();
            else
                startDiscovery();
        } else {
            // Single-player mode: play against all characters (except ours)
            for (int i = 0; i < CharacterPool.charactersList.length; i++) {
                if (i != character) {
                    String name = getString(CharacterPool.charactersList[i].getStringResourceName());
                    otherPlayers.add(new Player(this, i, name));
                }
            }
            rebuildOtherPlayersLayout();
            // Let's get started!
            quizPool = new QuizPool(this);
            newQuestion();
        }
    }

    private void rebuildOtherPlayersLayout() {
        // Remove the fake content we put in the initial layout (for designing)
        otherPlayersLayout.removeAllViews();
        // Fill other players row
        for (Player player : otherPlayers) {
            // Container for player + 3 lives
            LinearLayout playerAndLivesContainer = new LinearLayout(this);
            // Vertical, because we want the player on top of the lives
            playerAndLivesContainer.setOrientation(LinearLayout.VERTICAL);

            // Player image
            ImageButton btn = new ImageButton(this);
            // Attach the player data to the button
            btn.setTag(R.id.id_player, player);
            // Prepare the button style
            btn.setScaleType(ImageView.ScaleType.FIT_START);
            btn.setImageDrawable(player.animation);
            player.animation.start();
            btn.setAdjustViewBounds(true);
            btn.setEnabled(false);
            btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
            btn.setOnClickListener(onClickOtherPlayer);
            // Add the player button to the vertical container (on top of the name and lives)
            playerAndLivesContainer.addView(btn);

            // Name
            TextView nameTextView = new TextView(this);
            nameTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            nameTextView.setText(player.name);
            nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            playerAndLivesContainer.addView(nameTextView);

            // Sub-Container for 3 lives (row)
            LinearLayout livesContainer = new LinearLayout(this);
            // Horizontal, because we want the hearts lined-up side-by-side
            livesContainer.setOrientation(LinearLayout.HORIZONTAL);
            // Center horizontally based on parent container width
            livesContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
            livesContainer.setGravity(Gravity.CENTER);
            for (int i = 0; i < player.lives; i++) {
                ImageView lifeImg = new ImageView(this);
                lifeImg.setImageResource(R.drawable.heart);
                lifeImg.setLayoutParams(new LinearLayout.LayoutParams(20, 20));
                lifeImg.setAdjustViewBounds(true);
                livesContainer.addView(lifeImg);
            }
            // Add the lives row to the vertical container (under the player icon)
            playerAndLivesContainer.addView(livesContainer);

            // Finally, add the player and its lives to the game layout.
            otherPlayersLayout.addView(playerAndLivesContainer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Background music
        ring = MediaPlayer.create(this, R.raw.fight2);
        ring.setLooping(true);
        ring.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Background music
        ring.stop();
        ring.release();
    }

    private void broadcastEvent(GameEvent event) {
        Parcel parcel = Parcel.obtain();
        event.writeToParcel(parcel, 0);
        byte [] bytes = parcel.marshall();
        parcel.recycle();
        for (Player player: otherPlayers) {
            connectionsClient.sendPayload(player.endpointId, Payload.fromBytes(bytes));
        }
    }

    private void newQuestion() {
        if (isMultiPlayerMode) {
            if (isMultiPlayerMaster) {
                QuizPool.Entry entry = quizPool.getQuestion();
                GameEvent event = new GameEvent(GameEvent.TYPE_QUESTION);
                event.entry = entry;
                broadcastEvent(event);
                for (Player player: otherPlayers) {
                    player.answered = false;
                }
                setQuestion(entry);
            } else {
                // Do nothing
            }
        } else {
            QuizPool.Entry entry = quizPool.getQuestion();
            setQuestion(entry);
        }
    }

    private void setQuestion(QuizPool.Entry entry) {
        questionText.setText(entry.question);

        // We clear-out the old buttons, and create new ones for the current question
        answersLayout.removeAllViews();
        int count = 0;
        for (QuizPool.Answer answer : entry.answers) {
            Button button = new Button(this);
            button.setText(answer.text);
            button.setTag(R.id.id_answer, answer);
            button.setOnClickListener(onClickAnswer);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            button.setLayoutParams(lp);
            if (count == 0) {
                answersLayout.addView(button);
            } else {
                // insert at random position
                int index = random.nextInt(count);
                answersLayout.addView(button, index);
            }
            count++;
        }
        state = STATE_WAITING_FOR_ANSWER;
        updateControlButtons();
    }

    private View.OnClickListener onClickAnswer = new View.OnClickListener() {
        public void onClick(View v) {
            if (state != STATE_WAITING_FOR_ANSWER) {
                Log.e(TAG, "Unexpected answer click while in state " + state);
                return;
            }
            state = STATE_WAITING_FOR_QUESTION;

            // First thing: prevent user from clicking other answers while we handle this one.
            recursiveSetEnabled(false, answersLayout);

            // Retrieve the answer associated with this button
            QuizPool.Answer answer = (QuizPool.Answer) v.getTag(R.id.id_answer);
            if (answer.correct) {
                Log.d(TAG, "Correct!");
                questionText.setText(R.string.answer_correct);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.GREEN, 150));
            } else {
                Log.d(TAG, "Incorrect!");
                questionText.setText(R.string.answer_incorrect);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.RED, 150));
            }

            if (isMultiPlayerMode && !isMultiPlayerMaster) {
                // Do nothing, just wait for task master to send a new question
            } else {
                // Wait for all players to answer, then send a new question
                handler.post(checkAllPlayersAnswered);
            }
        }
    };

    private Runnable checkAllPlayersAnswered = new Runnable() {
        @Override
        public void run() {
            for (Player player: otherPlayers) {
                if (!player.answered) {
                    // Check back in 1 second
                    handler.postDelayed(this, 1000);
                    return;
                }
            }
            // Everyone has answered, time for a new question
            newQuestion();
        }
    };

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

    /**
     * Function to enable or disable the Question/Answers/Battle buttons
     */
    private void updateControlButtons() {
        switch (state) {
            case STATE_WAITING_FOR_PLAYERS:
                controlsLayout.setVisibility(View.GONE);
                buttonQuestion.setEnabled(false);
                buttonAnswers.setEnabled(false);
                buttonBattle.setEnabled(false);
                break;
            case STATE_WAITING_FOR_QUESTION:
                controlsLayout.setVisibility(View.VISIBLE);
                buttonQuestion.setEnabled(true);
                buttonAnswers.setEnabled(false);
                buttonBattle.setEnabled(true);
                break;
            case STATE_WAITING_FOR_ANSWER:
                controlsLayout.setVisibility(View.GONE);
                buttonQuestion.setEnabled(false);
                buttonAnswers.setEnabled(false);
                buttonBattle.setEnabled(false);
                break;
        }
    }

    /**
     * Functions to change the content of the bottom-right box
     *
     * These are called when the user clicks on the
     */
    public void showQuestion(View view) {
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        questionText.setVisibility(View.VISIBLE);
        questionText.bringToFront();
    }
    public void showAnswers(View view) {
        questionText.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.VISIBLE);
        answersLayout.bringToFront();
    }
    public void showBattleOptions(View view) {
        questionText.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.VISIBLE);
        battleOptionsLayout.bringToFront();
    }

    /**
     * Change the status of the other players buttons (for choosing who to attack)
     *
     * We never enable buttons for dead players, that's just wrong.
     *
     * @param enabled: if true, the user can press the button to attack
     */
    private void enablePlayersButtons(boolean enabled) {
        for (int i = 0; i < otherPlayers.size(); i++) {
            LinearLayout container = (LinearLayout) otherPlayersLayout.getChildAt(i);
            ImageButton button = (ImageButton) container.getChildAt(0);
            // Make sure dead players are not re-enabled
            if (otherPlayers.get(i).lives == 0) {
                button.setEnabled(false);
            } else {
                button.setEnabled(enabled);
            }
        }
    }

    private Runnable waitForYourFate = new Runnable() {
        @Override
        public void run() {
            /* Todo: make the game more intelligent by calculating:
               the likelihood for each player to answer correctly (player skills)
               the likelihood for each player to attack me
               For now: just get attacked 50% of the time by a random player.
             */
            if (random.nextInt(2) == 0) {
                int attacker_index;
                Player attacker;
                // Prevent a zombie attack! Pick random attacker until we find one that is alive
                do {
                    attacker_index = random.nextInt(otherPlayers.size());
                    attacker = otherPlayers.get(attacker_index);
                } while (attacker.lives == 0);
                String msg_without_name = getString(R.string.fate_attacked);
                String msg_with_name = String.format(msg_without_name, attacker.name);
                questionText.setText(msg_with_name);

                me.lives--;

                // Animate the heart
                fadingLifeImg = (ImageView) localPlayerLivesLayout.getChildAt(me.lives);
                fadingLifeImg.startAnimation(fadeOutAnimation);

                // Animate the characters (hurt or death)
                LinearLayout attackerContainer = (LinearLayout) otherPlayersLayout.getChildAt(attacker_index);
                ImageButton attackerView = (ImageButton) attackerContainer.getChildAt(0);
                zoomImageFromThumb(attackerView, attacker, doAnnounceDamage, false);
            } else {
                questionText.setText(R.string.fate_spared);
                handler.postDelayed(doNextQuestion, MESSAGE_DURATION_MS);
            }
        }
    };

    /**
     * Animation code from https://developer.android.com/training/animation/zoom.html
     * Simplified and modified to play battle animations (attack, hurt or die).
     * Sequence:
     * - Both characters zoom-in from their small thumbnail to the animation area.
     * - Animate attack and hurt/die.
     * - Both characters zoom-out back to their original position.
     */
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

    /**
     * Zoom-in animation:
     * - hide the small thumbnail
     * - calculate the offset and scaling required to shrink the big animation image exactly over the thumbnail
     * - animate from the thumbnail position to the full-size animation position.
     *
     * @param otherPlayerThumb: small image of the other player.
     * @param otherPlayer: other player data.
     * @param doThisAfterAnimation: what to run when the animation is finished.
     * @param winBattle: true if I (local player) am the victor of the battle.
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
        anim.withEndAction(doCharacterAnimation);
        anim.start();
    }

    /**
     * Battle animations
     * The attacker and the victim images are now fully zoomed-in.
     * Time to play the attack and the hurt/death animations.
     */
    private Runnable doCharacterAnimation = new Runnable() {
        @Override
        public void run() {
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
            if (victim.lives > 0) {
                resId = victim.mCharacter.getImageResourceHurt();
            } else {
                resId = victim.mCharacter.getDeathAnimationId();
            }
            victimView.setImageResource(resId);
            AnimationDrawable anim = (AnimationDrawable) victimView.getDrawable();
            anim.start();
            // Victor: select attack animation
            victorView.setImageResource(victor.mCharacter.getImageResourceAttack());
            anim = (AnimationDrawable) victorView.getDrawable();
            anim.start();
            // Let the hurt/death animation run for a certain time before zooming out
            if (victim.lives > 0) {
                handler.postDelayed(zoomImageBackToThumb, 1000);
            } else {
                // Death animation takes a bit longer
                handler.postDelayed(zoomImageBackToThumb, 2000);
            }
        }
    };

    /**
     * Zoom-out animation:
     * - Shrink the animation images back to the thumbnail position
     */
    private Runnable zoomImageBackToThumb = new Runnable() {
        @Override
        public void run() {
            // Stop the hurt or death animation
            AnimationDrawable animationDrawable = (AnimationDrawable) otherPlayerAnimation.getDrawable();
            animationDrawable.stop();
            // Zoom-out with the dead image or normal image
            if (mOtherPlayer.lives == 0)
                otherPlayerAnimation.setImageResource(mOtherPlayer.mCharacter.getDeadImageId());
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
            if (me.lives == 0)
                localPlayerAnimation.setImageResource(me.mCharacter.getDeadImageId());
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
            anim.withEndAction(doFinishAnimation);
            anim.start();
        }
    };

    /**
     * After the zoom-out animation, we clean-up and re-enable the thumbnail.
     */
    private Runnable doFinishAnimation = new Runnable() {
        @Override
        public void run() {
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
    };

    private Runnable doAnnounceDamage = new Runnable() {
        @Override
        public void run() {
            fadingLifeImg.setVisibility(View.GONE);
            fadingLifeImg.clearAnimation();
            localPlayerLivesLayout.removeView(fadingLifeImg);
            if (me.lives == 0) {
                questionText.setText(R.string.game_over);
                handler.postDelayed(doFinishGame, MESSAGE_DURATION_MS);
            } else {
                String msg_without_lives = getString(R.string.game_not_over);
                String msg_with_lives = String.format(msg_without_lives, me.lives);
                questionText.setText(msg_with_lives);
                handler.postDelayed(doNextQuestion, MESSAGE_DURATION_MS);
            }
        }
    };

    private Runnable doNextQuestion = new Runnable() {
        @Override
        public void run() {
            newQuestion();
        }
    };

    private Runnable doFinishGame = new Runnable() {
        @Override
        public void run() {
            ring.stop();
            finish();
        }
    };

    private Player victim = null;
    private LinearLayout victim_container = null;
    private View.OnClickListener onClickOtherPlayer = new View.OnClickListener() {
        public void onClick(View v) {
            enablePlayersButtons(false);
            // Check which answer correspond that button.
            victim = (Player) v.getTag(R.id.id_player);
            victim.lives--;
            // Find the heart to animate
            victim_container = (LinearLayout) v.getParent();
            LinearLayout livesContainer = (LinearLayout) victim_container.getChildAt(1);
            // The image we want is at the end of the row
            fadingLifeImg = (ImageView) livesContainer.getChildAt(livesContainer.getChildCount() - 1);
            fadingLifeImg.startAnimation(fadeOutAnimation);
            String msg_without_player = getString(R.string.do_attack);
            String msg_with_player = String.format(msg_without_player, victim.name);
            questionText.setText(msg_with_player);
            ImageView victim_image = (ImageView) victim_container.getChildAt(0);
            zoomImageFromThumb(victim_image, victim, doAttackOtherPlayer, true);
        }
    };

    private Runnable doAttackOtherPlayer = new Runnable() {
        @Override
        public void run() {
            // Stop the animation and remove the fading life icon from it's container
            fadingLifeImg.setVisibility(View.GONE);
            fadingLifeImg.clearAnimation();
            LinearLayout livesContainer = (LinearLayout) fadingLifeImg.getParent();
            livesContainer.removeView(fadingLifeImg);
            if (victim.lives == 0) {
                String msg_without_victim = getString(R.string.attack_killed);
                String msg_with_victim = String.format(msg_without_victim, victim.name);
                questionText.setText(msg_with_victim);
                handler.postDelayed(doRemovePlayer, MESSAGE_DURATION_MS);
            } else {
                newQuestion();
            }
        }
    };

    private Runnable doRemovePlayer = new Runnable() {
        @Override
        public void run() {
            // Disable the victim
            recursiveSetEnabled(false, victim_container);
            // Check if we won
            boolean everybodyDead = true;
            for (Player player : otherPlayers) {
                if (player.lives != 0) {
                    everybodyDead = false;
                    MediaPlayer mediaPlayer = MediaPlayer.create(PLAY.this, R.raw.fail);
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mediaPlayer.release();
                            ring.setVolume(1.0f, 1.0f);
                        }
                    });
                    ring.setVolume(1.0f, 0.5f);
                    mediaPlayer.start();
                    break; // no need to continue, we have at least 1 opponent alive.

                }
            }
            if (everybodyDead) {
                questionText.setText(R.string.game_won);
                handler.postDelayed(doFinishGame, LONG_MESSAGE_DURATION_MS);
            } else {
                newQuestion();
            }
        }
    };



    /*
     * Multiplayer connection code based on the rockpaperscissors example from:
     * https://github.com/googlesamples/android-nearby
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private static class GameEvent implements Parcelable {
        public static final int TYPE_QUESTION = 0;
        public static final int TYPE_PLAYER_ANSWERED = 1;
        public int type;

        QuizPool.Entry entry;

        public GameEvent(int type) {
            this.type = type;
        }

        protected GameEvent(Parcel in) {
            type = in.readInt();
            switch (type) {
                case TYPE_QUESTION:
                    entry = new QuizPool.Entry(in);
                    break;
                case TYPE_PLAYER_ANSWERED:
                    break;
            }
        }

        public static final Creator<GameEvent> CREATOR = new Creator<GameEvent>() {
            @Override
            public GameEvent createFromParcel(Parcel in) {
                return new GameEvent(in);
            }

            @Override
            public GameEvent[] newArray(int size) {
                return new GameEvent[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(type);
            switch (type) {
                case TYPE_QUESTION:
                    entry.writeToParcel(dest, flags);
                    break;
            }
        }
    }
    // Our handle to Nearby Connections
    private ConnectionsClient connectionsClient;

    private final Map<String, String> mDiscoveredPlayers = new HashMap<>();
    private final Map<String, String> mPendingConnections = new HashMap<>();
    private final Map<String, String> mEstablishedConnections = new HashMap<>();

    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Parcel parcel = Parcel.obtain();
                    byte bytes[] = payload.asBytes();
                    parcel.unmarshall(bytes, 0, bytes.length);
                    parcel.setDataPosition(0);
                    GameEvent event = GameEvent.CREATOR.createFromParcel(parcel);
                    parcel.recycle();
                    switch (event.type) {
                        case GameEvent.TYPE_QUESTION:
                            if (isMultiPlayerMaster) {
                                Log.e(TAG, "Task master wasn't expecting a question");
                            } else {
                                setQuestion(event.entry);
                            }
                            break;
                        case GameEvent.TYPE_PLAYER_ANSWERED:
                            for (Player player: otherPlayers) {
                                if (player.endpointId == endpointId) {
                                    player.answered = true;
                                    break;
                                }
                            }
                            break;
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == Status.SUCCESS /*&& myChoice != null && opponentChoice != null*/) {
                        //finishRound();
                    }
                }
            };

    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(me.name, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    if (me.name == connectionInfo.getEndpointName()) {
                        Log.e(TAG, "This game is too small for both of us " + connectionInfo.getEndpointName());
                        questionText.setText("Somebody is trying to connect with the same name as me " + connectionInfo.getEndpointName());
                        connectionsClient.rejectConnection(endpointId);
                        return;
                    }
                    for (Player player : otherPlayers) {
                        if (player.name == connectionInfo.getEndpointName()) {
                            Log.e(TAG, "Sorry, we already have a player named " + connectionInfo.getEndpointName());
                            questionText.setText("Somebody is trying to connect with the same name as an existing player " + connectionInfo.getEndpointName());
                            connectionsClient.rejectConnection(endpointId);
                            return;
                        }
                    }
                    Log.i(TAG, "onConnectionInitiated: accepting connection");
                    questionText.setText("Accepting connection from " + connectionInfo.getEndpointName().toString());
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    mPendingConnections.put(endpointId, connectionInfo.getEndpointName());
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful");

                        final Context context = otherPlayersLayout.getContext();
                        otherPlayers.add(new Player(context, 0, mPendingConnections.get(endpointId).toString()));
                        if (otherPlayers.size()==1) {
                            Button button = new Button(context);
                            button.setText("Start playing now...");
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connectionsClient.stopDiscovery();
                                    connectionsClient.stopAdvertising();
                                    // Let's get started!
                                    quizPool = new QuizPool(context);
                                    newQuestion();
                                }
                            });
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.gravity = Gravity.CENTER;
                            button.setLayoutParams(lp);
                            answersLayout.addView(button);
                        }
                        mEstablishedConnections.put(endpointId, mPendingConnections.remove(endpointId));
                        rebuildOtherPlayersLayout();
                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed");
                        mPendingConnections.remove(endpointId);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    questionText.setText("Disconnected from " + mEstablishedConnections.get(endpointId));
                    Player player;
                    for (int i = 0; i < otherPlayers.size(); i++) {
                        player = otherPlayers.get(i);
                        if (player.name == mEstablishedConnections.get(endpointId)) {
                            player.lives = 0;
                            victim_container = (LinearLayout) otherPlayersLayout.getChildAt(i);
                            ImageButton imageButton = (ImageButton) victim_container.getChildAt(0);
                            imageButton.setImageResource(player.mCharacter.getDeadImageId());
                            LinearLayout livesContainer = (LinearLayout) victim_container.getChildAt(1);
                            livesContainer.removeAllViews();
                            handler.postDelayed(doRemovePlayer, LONG_MESSAGE_DURATION_MS);
                            break;
                        }
                    }
                }
            };

    @Override
    protected void onStart() {
        super.onStart();
        if (isMultiPlayerMode) {
            if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onStop() {
        if (connectionsClient != null) {
            connectionsClient.stopAllEndpoints();
        }
        super.onStop();
    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    /** Starts looking for other players using Nearby Connections. */
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY));
    }

    /** Broadcasts our presence using Nearby Connections so other players can find us. */
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                me.name, getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY));
    }
}
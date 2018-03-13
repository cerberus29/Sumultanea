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

import static com.sombright.simultanea.Constants.SERVICE_ID;
import static com.sombright.simultanea.MainActivity.TAG;

public class PlayActivity extends ConnectionsActivity implements View.OnClickListener, PlayersViewAdapter.OnClickPlayerListener, OpenTriviaDatabase.Listener {

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
    private ImageView leftAnimation, rightAnimation;
    private Button buttonStartGame, buttonQuestion, buttonAnswers, buttonBattle;
    private boolean singlePlayerMode = false;
    private Button buttonHeal, buttonAttack, buttonDefend;
    private Handler handler = new Handler();
    private MediaPlayer mMusic;
    private Animation fadeOutAnimation;
    // Some variables we need to keep for the zoom-out animation
    private ImageView mOtherPlayerThumb;
    private Player mOtherPlayer;
    private boolean mWinBattle;
    // Translation and scale factors
    class ZoomAnimationData {
        ImageView thumb;
        ImageView big;
        PointF startScale = new PointF();
        PointF startTranslation = new PointF();
    }
    class AttackAnimationData {
        Player attacker, victim;
        boolean kill;
        ZoomAnimationData left = new ZoomAnimationData();
        ZoomAnimationData right = new ZoomAnimationData();
    }
    private AttackAnimationData mAttackAnimationData = null;
    private int mShortAnimationDuration = 500;
    private PreferencesProxy mPrefs;

    private QuizPool quizPool = null;
    private OpenTriviaDatabase opentdb = null;

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

        mPrefs = new PreferencesProxy(this);

        // Retrieve a copy of the settings (cannot change during the game)
        String name = mPrefs.getMultiPlayerAlias();

        me = new Player(this);
        me.setName(name);
        me.setCharacter(mPrefs.getCharacter());
        Log.d(TAG, "Playing as character " + me.getCharacter());

        // Apply the initial layout (we will modify below)
        setContentView(R.layout.activity_play);

        // These are the portions of the layout that we will modify during the game
        questionText = findViewById(R.id.questionTextView);
        answersLayout = findViewById(R.id.answersLayout);
        battleOptionsLayout = findViewById(R.id.battleOptionsLayout);
        otherPlayersLayout = findViewById(R.id.otherPlayersLayout);
        textViewLocalPlayer = findViewById(R.id.textViewLocalPlayer);
        leftAnimation = findViewById(R.id.leftAnimation);
        rightAnimation = findViewById(R.id.rightAnimation);

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
        otherPlayersLayout.setAdapter(mPlayersViewAdapter);

        questionText.setText(R.string.waiting_for_master);
        answersLayout.removeAllViews();
        startDiscovering();
    }

    @Override
    protected void onDiscoveryStarted() {
        Log.d(TAG, "onDiscoveryStarted");
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
        if (me.getPoints() == 0) {
            //TODO disable attack?
            buttonAttack.setEnabled(false);
        } else {
            buttonAttack.setEnabled(true);
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
        buttonStartGame.setVisibility(View.GONE);
        singlePlayerMode = true;
        for (int i = 0; i < 5; i++) {
            // Find a unique name
            String characterName = null, playerName = null;
            int num = 0; // Add a number when the name already exists
            Player player = null;
            do {
                num++;
                for (Character character : CharacterPool.charactersList) {
                    characterName = character.getName(PlayActivity.this);
                    playerName = characterName;
                    if (num > 1) {
                        playerName += " " + num;
                    }
                    player = mPlayersViewAdapter.getPlayerByName(playerName);
                    if (player == null) {
                        break;
                    }
                }
            } while (player != null);
            player = new Player(this);
            player.setName(playerName);
            player.setCharacter(characterName);
            GameMessage msg = player.getPlayerDetails();
            onReceive(null, Payload.fromBytes(msg.toBytes()));
        }

        if (mPrefs.shouldUseOpenTriviaDatabase()) {
            opentdb = new OpenTriviaDatabase(this);
            opentdb.setQuestionAttributes(OpenTriviaDatabase.CATEGORY_ANY,
                    OpenTriviaDatabase.DIFFICULTY_ANY,
                    OpenTriviaDatabase.TYPE_ANY);
            opentdb.setListener(this);
        } else {
            quizPool = new QuizPool(this);
        }
        pickQuestion();
    }

    private void pickQuestion() {
        me.setAnswered(false);
        mPlayersViewAdapter.setAnswered(false);
        QuizPool.Entry entry;
        if (opentdb != null) {
            OpenTriviaDatabase.Question question = opentdb.getQuestion();
            if (question == null) {
                // We'll come back later in onQuestionsAvailable callback
                waitingForQuestion = true;
                return;
            }
            // Convert to QuizPool.Question
            int type;
            switch (question.type) {
                case OpenTriviaDatabase.TYPE_MULTIPLE_CHOICE:
                    type = QuizPool.TYPE_MULTIPLE_CHOICE;
                    break;
                case OpenTriviaDatabase.TYPE_TRUE_OR_FALSE:
                    type = QuizPool.TYPE_TRUE_FALSE;
                    break;
                default:
                    Log.e(TAG, "Unknown question type " + question.type);
                    type = QuizPool.TYPE_MULTIPLE_CHOICE;
                    break;
            }
            List<QuizPool.Answer> answers = new ArrayList<>();
            for (String answer: question.incorrect_answers)
                answers.add(new QuizPool.Answer(answer, false));
            answers.add(random.nextInt(answers.size()),
                    new QuizPool.Answer(question.correct_answer, true));
            entry = new QuizPool.Entry(question.question, type, answers);
        } else {
            entry = quizPool.getQuestion();
        }
        GameMessage msg = new GameMessage();
        msg.setType(GameMessage.GAME_MESSAGE_TYPE_QUESTION);
        msg.questionInfo.entry = entry;
        onReceive(null, Payload.fromBytes(msg.toBytes()));
        // Fake receiving updates from taskmaster as other players answered
        for (int i = 0; i < mPlayersViewAdapter.getCount(); i++) {
            final Player player = mPlayersViewAdapter.getItem(i);
            if (player == null) {
                continue;
            }
            // Simulate other players answering within 0-10 seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO: vary the likeliness that the player answered correctly based on the character
                    boolean correct = random.nextBoolean();
                    // Player sends their answer to tasks master...
                    player.setAnswered(true);
                    // Taskmaster adjusts player info
                    if (correct) {
                        player.setPoints(player.getPoints()+1);
                        // Taskmaster sends player info
                        GameMessage msg = player.getPlayerDetails();
                        onReceive(null, Payload.fromBytes(msg.toBytes()));
                    }
                    // Taskmaster picks another question
                    if (me.hasAnswered() && mPlayersViewAdapter.hasEveryoneAnswered()) {
                        pickQuestion();
                    }
                }
            }, random.nextInt(10)*1000);
        }
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
            if (me.getPoints() != 0) {
                buttonAttack.setEnabled(true);
            }
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
                if (me.getPoints() != 0) {
                    buttonAttack.setEnabled(true);
                }
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
                if (me.getPoints() != 0) {
                    buttonAttack.setEnabled(true);
                }
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

    private void animateAttack(Player attacker, Player victim, boolean defending, boolean killed) {
        final AttackAnimationData data = new AttackAnimationData();

        data.attacker = attacker;
        data.victim = victim;
        data.kill = killed;

        if (attacker == me) {
            data.left.thumb = localPlayerThumb;
        } else {
            int position = mPlayersViewAdapter.getPosition(attacker);
            LinearLayout item = (LinearLayout) otherPlayersLayout.getChildAt(position);
            data.left.thumb = (ImageView) item.getChildAt(0);
        }
        data.left.big = leftAnimation;

        if (victim == me) {
            data.right.thumb = localPlayerThumb;
        } else {
            int position = mPlayersViewAdapter.getPosition(victim);
            LinearLayout item = (LinearLayout) otherPlayersLayout.getChildAt(position);
            data.right.thumb = (ImageView) item.getChildAt(0);
        }
        data.right.big = rightAnimation;

        zoomImageFromThumb(data.left, null);
        zoomImageFromThumb(data.right, new Runnable() {
            @Override
            public void run() {
                doCharacterAnimation(data);
            }
        });
    }

    /**
     * Zoom-in animation:
     * - hide the small thumbnail
     * - calculate the offset and scaling required to shrink the big animation image exactly over the thumbnail
     * - animate from the thumbnail position to the full-size animation position.
     */
    private void zoomImageFromThumb(ZoomAnimationData data, Runnable endAction) {
        // Load the big zoom-in images (same as the small thumbnail images for now).
        data.big.setImageDrawable(data.thumb.getDrawable());

        // Calculate the starting and ending bounds for the zoomed-in image.
        Rect startBounds = new Rect();
        Rect finalBounds = new Rect();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the zoomed-in image.
        data.thumb.getGlobalVisibleRect(startBounds);
        data.big.getGlobalVisibleRect(finalBounds);
        // Calculate the transformations needed to make the big animation fit exactly on top of the thumbnail:
        data.startTranslation.x = startBounds.left - finalBounds.left;
        data.startTranslation.y = startBounds.top - finalBounds.top;
        data.startScale.x = (float) startBounds.width() / finalBounds.width();
        data.startScale.y = (float) startBounds.height() / finalBounds.height();

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnails.
        data.thumb.setAlpha(0f);

        // Apply translation and scale to cover the thumbnail
        data.big.setTranslationX(data.startTranslation.x);
        data.big.setTranslationY(data.startTranslation.y);
        data.big.setScaleX(data.startScale.x);
        data.big.setScaleY(data.startScale.y);
        data.big.setVisibility(View.VISIBLE);
        data.big.bringToFront();

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        data.big.setPivotX(0f);
        data.big.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties.
        ViewPropertyAnimator anim = data.big.animate();
        anim.translationX(0f);
        anim.translationY(0f);
        anim.scaleX(1f);
        anim.scaleY(1f);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        if (endAction != null) {
            // Schedule the next step at the end of the zoom-in animation
            anim.withEndAction(endAction);
        }
        anim.start();
    }

    /**
     * Battle animations
     * The attacker and the victim images are now fully zoomed-in.
     * Time to play the attack and the hurt/death animations.
     */
    private void doCharacterAnimation(final AttackAnimationData data) {
        // Victim: select between hurt and death animations
        int resId;
        if (data.kill) {
            resId = data.victim.getCharacter().getDeathAnimationId();
        } else {
            resId = data.victim.getCharacter().getImageResourceHurt();
        }
        data.right.big.setImageResource(resId);
        AnimationDrawable anim = (AnimationDrawable) data.right.big.getDrawable();
        anim.start();
        // Select attack animation
        data.left.big.setImageResource(data.attacker.getCharacter().getImageResourceAttack());
        anim = (AnimationDrawable) data.left.big.getDrawable();
        anim.start();
        // Let the hurt/death animation run for a certain time before zooming out
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                zoomImageBackToThumb(data.left, data.attacker.getCharacter(), false);
                zoomImageBackToThumb(data.right, data.victim.getCharacter(), data.kill);
            }
            // Death animation takes a bit longer
        }, data.kill ? 2000: 1000);
    }

    /**
     * Zoom-out animation:
     * - Shrink the animation images back to the thumbnail position
     */
    private void zoomImageBackToThumb(final ZoomAnimationData data, Character character, boolean dead) {
        // Stop the hurt or death animation
        AnimationDrawable animationDrawable = (AnimationDrawable) data.big.getDrawable();
        animationDrawable.stop();
        // Zoom-out with the dead image or normal image
        if (dead)
            data.big.setImageResource(character.getDeadImageId());
        else
            data.big.setImageDrawable(data.thumb.getDrawable());

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        ViewPropertyAnimator anim = data.big.animate();
        anim.translationX(data.startTranslation.x);
        anim.translationY(data.startTranslation.y);
        anim.scaleX(data.startScale.x);
        anim.scaleY(data.startScale.y);
        anim.setDuration(mShortAnimationDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                // Re-enable thumb view with the same image as the zoom-out animation
                data.thumb.setImageDrawable(data.big.getDrawable());
                data.thumb.setAlpha(1f);
                data.big.setVisibility(View.INVISIBLE);
                data.big.setTranslationX(0f);
                data.big.setTranslationY(0f);
                data.big.setScaleX(1f);
                data.big.setScaleY(1f);
            }
        });
        anim.start();
    }

    private void finishGame() {
        mMusic.stop();
        finish();
    }

    @Override
    public void onClick(View v) {
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
            if (singlePlayerMode) {
                me.setAnswered(true);
                if (answer.correct) {
                    // Simulate task master sending us updated points
                    me.setPoints(me.getPoints()+1);
                    // Taskmaster sends player info
                    GameMessage msg = me.getPlayerDetails();
                    onReceive(null, Payload.fromBytes(msg.toBytes()));
                }
                if (mPlayersViewAdapter.hasEveryoneAnswered()) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pickQuestion();
                        }
                    }, 500);
                }
            } else {
                GameMessage msg = new GameMessage();
                msg.setType(GameMessage.GAME_MESSAGE_TYPE_ANSWER);
                msg.answerInfo.correct = answer.correct;
                send(Payload.fromBytes(msg.toBytes()));
            }
            return;
        }

        Log.e(TAG, "Unhandled click on " + v);
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
        assert msg != null;
        switch (msg.getType()) {
            case GameMessage.GAME_MESSAGE_TYPE_PLAYER_INFO:
                onReceivePlayerInfo(endpoint, msg);
                break;
            case GameMessage.GAME_MESSAGE_TYPE_ATTACK:
                onReceiveAttackInfo(endpoint, msg);
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
            if (me.getPoints() != 0) {
                buttonAttack.setEnabled(true);
            }
        } else {
            player = mPlayersViewAdapter.getPlayer(msg.playerInfo.uniqueId);
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

    private void onReceiveAttackInfo(Endpoint endpoint, GameMessage msg) {
        Log.d(TAG, "onReceiveAttackInfo");
        Player attacker;
        Player victim;
        if (me.getUniqueID().equals(msg.attackInfo.attackerId)) {
            attacker = me;
            victim = mPlayersViewAdapter.getPlayer(msg.attackInfo.victimId);
        } else if (me.getUniqueID().equals(msg.attackInfo.victimId)) {
            victim = me;
            attacker = mPlayersViewAdapter.getPlayer(msg.attackInfo.attackerId);
        } else {
            // We don't care about other people's battles
            return;
        }
        animateAttack(attacker, victim, msg.attackInfo.defending, msg.attackInfo.killed);
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
        if (singlePlayerMode) {
            // Simulate the task master sending the player info to everyone, including ourselves
            onReceive(null, Payload.fromBytes(msg.toBytes()));
        } else {
            send(Payload.fromBytes(msg.toBytes()));
        }
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
    public void onClickPlayer(Player victim) {
        Log.v(TAG, "onClickPlayer: " + victim.getName());
        // Check if it was a player button
        mPlayersViewAdapter.setClickable(false);
        if (singlePlayerMode) {
            // Simulate task master calculating results and informing us back
            GameMessage msg = new GameMessage();
            msg.setType(GameMessage.GAME_MESSAGE_TYPE_ATTACK);
            msg.attackInfo.attackerId = me.getUniqueID();
            msg.attackInfo.victimId = victim.getUniqueID();
            msg.attackInfo.defending = victim.getCombatMode() == Player.COMBAT_MODE_DEFEND;
            int damage = me.getCharacter().getAttack();
            if (msg.attackInfo.defending) {
                damage -= victim.getCharacter().getDefense();
            }
            if (damage < 0) {
                damage = 0;
            }
            int victimHealth = victim.getHealth() - damage;
            if (victimHealth <= 0) {
                victim.setHealth(0);
                msg.attackInfo.killed = true;
            } else {
                victim.setHealth(victimHealth);
            }
            // Attacking is not free...
            me.setPoints(me.getPoints()-1);
            // Send attack details
            onReceive(null, Payload.fromBytes(msg.toBytes()));
            // Send updated player info
            msg = victim.getPlayerDetails();
            onReceive(null, Payload.fromBytes(msg.toBytes()));
            msg = me.getPlayerDetails();
            onReceive(null, Payload.fromBytes(msg.toBytes()));
        } else {
            GameMessage msg = new GameMessage();
            msg.setType(GameMessage.GAME_MESSAGE_TYPE_ATTACK);
            msg.attackInfo.victimId = victim.getUniqueID();
            broadcastMessage(msg);
        }
        buttonHeal.setEnabled(true);
        if (me.getPoints() != 0) {
            buttonAttack.setEnabled(true);
        }
        buttonDefend.setEnabled(true);
    }


    // OpenTriviaDatabase callbacks (for single player mode

    @Override
    public void onCategoriesChanged(List<String> categories) {
        // Do nothing
    }
    private boolean waitingForQuestion = false;
    @Override
    public void onQuestionsAvailable(boolean available) {
        if (available && waitingForQuestion) {
            waitingForQuestion = false;
            pickQuestion();
        }
    }
}
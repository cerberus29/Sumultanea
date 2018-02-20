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

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

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
    private final Map<String, String> mDiscoveredPlayers = new HashMap<>();
    private final Map<String, String> mPendingConnections = new HashMap<>();
    private final Map<String, String> mEstablishedConnections = new HashMap<>();
    TextView questionText;
    private int state = STATE_WAITING_FOR_PLAYERS;
    private QuizPool quizPool;
    private Random random = new Random();
    private LinearLayout answersLayout;
    private LinearLayout battleOptionsLayout;
    private Player me;
    private List<Player> otherPlayers = new ArrayList<>();
    private FlexboxLayout otherPlayersLayout;
    private TextView textViewLocalPlayer;
    private ProgressBar localPlayerHealth;
    private ImageView localPlayerThumb;
    private ImageView localPlayerAnimation, otherPlayerAnimation;
    private Button buttonQuestion, buttonAnswers, buttonBattle;
    private Button buttonStartGame, buttonAddFakePlayer;
    private Button buttonHeal, buttonAttack, buttonDefend;
    private Handler handler = new Handler();
    private MediaPlayer mMusic;
    private Animation fadeOutAnimation;
    private boolean isTaskMaster;
    private Runnable doCheckAllPlayersAnswered = new Runnable() {
        @Override
        public void run() {
            checkAllPlayersAnswered();
        }
    };
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
    // Our handle to Nearby Connections
    private ConnectionsClient connectionsClient;
    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    // Handle this message in a regular function to reduce indentation.
                    onPayloadReceivedInternal(endpointId, payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    // Do nothing.
                }
            };
    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    onConnectionInitiatedInternal(endpointId, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    onConnectionResultInternal(endpointId, result);
                }

                @Override
                public void onDisconnected(String endpointId) {
                    onDisconnectedInternal(endpointId);
                }
            };
    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    onEndpointFoundInternal(endpointId, info);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    onEndpointLostInternal(endpointId);
                }
            };

    /**
     * Returns true if the app was granted all the permissions. Otherwise, returns false.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

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
        isTaskMaster = prefs.isMultiPlayerMaster();
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

        buttonQuestion = findViewById(R.id.buttonQuestion);
        buttonAnswers = findViewById(R.id.buttonAnswers);
        buttonBattle = findViewById(R.id.buttonBattle);
        buttonStartGame = findViewById(R.id.buttonStartGame);
        buttonAddFakePlayer = findViewById(R.id.buttonAddFakePlayer);

        buttonHeal = findViewById(R.id.buttonHeal);
        buttonAttack = findViewById(R.id.buttonAttack);
        buttonDefend = findViewById(R.id.buttonDefend);

        localPlayerThumb = findViewById(R.id.imageViewLocalPlayer);
        localPlayerHealth = findViewById(R.id.localPlayerHealth);
        localPlayerHealth.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

        buttonQuestion.setEnabled(false);
        buttonAnswers.setEnabled(false);
        buttonBattle.setEnabled(false);

        updateLocalPlayerUi();

        // Prepare an animation object for when we loose a life
        fadeOutAnimation = new AlphaAnimation(1, 0);
        fadeOutAnimation.setDuration(MESSAGE_DURATION_MS / 6);
        fadeOutAnimation.setInterpolator(new LinearInterpolator());
        fadeOutAnimation.setRepeatCount(Animation.INFINITE);
        fadeOutAnimation.setRepeatMode(Animation.REVERSE);
        // Remove the fake content we put in the initial layout (for designing)
        otherPlayersLayout.removeAllViews();

        quizPool = new QuizPool(this);

        connectionsClient = Nearby.getConnectionsClient(this);
        questionText.setText(R.string.waiting_for_players);
        answersLayout.removeAllViews();
        if (isTaskMaster) {
            // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
            connectionsClient.startAdvertising(me.getName(), getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY));
        } else {
            // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
            connectionsClient.startDiscovery(getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY));
        }
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
        if (connectionsClient != null) {
            connectionsClient.stopAllEndpoints();
        }
        super.onStop();
    }

    public void addFakePlayer(View v) {
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
        addPlayer(characterName, playerName);
    }

    private void addPlayer(String character, String name) {
        Player player = new Player(this);
        player.setCharacter(character);
        player.setName(name);
        otherPlayers.add(player);
        updateOtherPlayersUi();
    }

    private void updateOtherPlayersUi() {
        int index = 0;
        for (Player player : otherPlayers) {
            // Container for player icon + name + health
            LinearLayout playerAndLivesContainer = (LinearLayout) otherPlayersLayout.getChildAt(index++);
            if (playerAndLivesContainer == null) {
                playerAndLivesContainer = new LinearLayout(this);
                // Vertical, because we want the player on top of the name and health
                playerAndLivesContainer.setOrientation(LinearLayout.VERTICAL);
                playerAndLivesContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
                // Player image
                ImageButton btn = new ImageButton(this);
                // Attach the player data to the button
                btn.setTag(R.id.id_player, player);
                // Prepare the button style
                btn.setScaleType(ImageView.ScaleType.FIT_START);
                AnimationDrawable animationDrawable = player.getAnimation();
                btn.setImageDrawable(animationDrawable);
                animationDrawable.start();
                btn.setAdjustViewBounds(true);
                btn.setEnabled(false);
                // Convert 75dp into pixels
                int size_in_px = dp2px(75);
                btn.setLayoutParams(new LinearLayout.LayoutParams(size_in_px, size_in_px));
                btn.setOnClickListener(this);
                // Add the player button to the vertical container (on top of the name and health)
                playerAndLivesContainer.addView(btn);

                // Name
                TextView nameTextView = new TextView(this);
                nameTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                nameTextView.setText(player.getName());
                nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                playerAndLivesContainer.addView(nameTextView);

                ProgressBar healthBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                healthBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                // Center horizontally based on parent container width
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                size_in_px = dp2px(8);
                lp.setMargins(size_in_px,0,size_in_px,0);
                healthBar.setLayoutParams(lp);
                healthBar.setProgress(player.getHealth());
                // Add the lives row to the vertical container (under the player icon)
                playerAndLivesContainer.addView(healthBar);

                // Finally, add to the game layout.
                otherPlayersLayout.addView(playerAndLivesContainer);
                if (otherPlayers.size() == 1) {
                    buttonStartGame.setEnabled(true);
                }
                if (otherPlayers.size() == MAX_PLAYERS) {
                    buttonAddFakePlayer.setEnabled(false);
                }
            } else {
                ImageButton btn = (ImageButton) playerAndLivesContainer.getChildAt(0);
                if (btn.getTag(R.id.id_player) != player) {
                    btn.setTag(R.id.id_player, player);
                }
                AnimationDrawable animationDrawable = player.getAnimation();
                if (btn.getDrawable() != animationDrawable) {
                    btn.setImageDrawable(animationDrawable);
                    animationDrawable.start();
                }
                // Name
                TextView nameTextView = (TextView) playerAndLivesContainer.getChildAt(1);
                if (nameTextView.getText() != player.getName()) {
                    nameTextView.setText(player.getName());
                }
                // Health
                ProgressBar healthBar = (ProgressBar) playerAndLivesContainer.getChildAt(2);
                if (healthBar.getProgress() != player.getHealth()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        healthBar.setProgress(player.getHealth(), true);
                    } else {
                        healthBar.setProgress(player.getHealth());
                    }
                }
            }
        }
        while (otherPlayersLayout.getChildCount() > otherPlayers.size()) {
            otherPlayersLayout.removeViewAt(otherPlayersLayout.getChildCount() - 1);
        }
    }

    private void updateLocalPlayerUi() {
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

    // Handy function to convert sizes in dp (used in the layout editor) into pixels (used in the code)
    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void startGame() {
        if (isTaskMaster)
            connectionsClient.stopAdvertising();
        else
            connectionsClient.stopDiscovery();
        newQuestion();
        showQuestion(buttonQuestion);
    }

    private void newQuestion() {
        if (isTaskMaster) {
            QuizPool.Entry entry = quizPool.getQuestion();
            GameMessage msg = new GameMessage();
            msg.setType(GameMessage.GAME_MESSAGE_TYPE_QUESTION);
            msg.setQuestion(entry);
            broadcastMessage(msg);
            for (Player player : otherPlayers) {
                player.setAnswered(false);
            }
            setQuestion(entry);
        } else {
            Log.wtf(TAG, "Other players should just wait for a new question!");
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
            button.setOnClickListener(this);
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
    }

    private void fakePlayerAnswer() {
        for (Player player : otherPlayers) {
            if (!player.hasAnswered()) {
                player.setAnswered(true);
            }
        }
    }

    private void checkAllPlayersAnswered() {
        for (Player player : otherPlayers) {
            if (!player.hasAnswered()) {
                // Check back in 1 second
                handler.postDelayed(doCheckAllPlayersAnswered, 500);
                return;
            }
        }
        // Everyone has answered, time for a new question
        newQuestion();
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

    /**
     * Functions to change the content of the bottom-right box
     */
    public void showQuestion(View view) {
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        questionText.setVisibility(View.VISIBLE);
        questionText.bringToFront();
        buttonQuestion.setEnabled(false);
        buttonAnswers.setEnabled(true);
        buttonBattle.setEnabled(true);
    }

    public void showAnswers(View view) {
        questionText.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.VISIBLE);
        answersLayout.bringToFront();
        buttonQuestion.setEnabled(true);
        buttonAnswers.setEnabled(false);
        buttonBattle.setEnabled(true);
    }

    public void showBattleOptions(View view) {
        questionText.setVisibility(View.INVISIBLE);
        answersLayout.setVisibility(View.INVISIBLE);
        battleOptionsLayout.setVisibility(View.VISIBLE);
        battleOptionsLayout.bringToFront();
        buttonQuestion.setEnabled(true);
        buttonAnswers.setEnabled(true);
        buttonBattle.setEnabled(false);
    }


    public void buttonStartGameClicked(View view) {
        if (state == STATE_WAITING_FOR_PLAYERS) {
            buttonQuestion.setVisibility(View.VISIBLE);
            buttonAnswers.setVisibility(View.VISIBLE);
            buttonBattle.setVisibility(View.VISIBLE);
            buttonStartGame.setVisibility(View.GONE);
            buttonAddFakePlayer.setVisibility(View.GONE);
            startGame();
        } else {
            Log.d(TAG, "Unhandled task master button click in state " + state);
        }
    }

    public void buttonHealClicked(View view) {
        buttonHeal.setEnabled(false);
        buttonAttack.setEnabled(false);
        buttonDefend.setEnabled(false);
        me.setCombatMode(Player.COMBAT_MODE_HEAL);
        updateLocalPlayerUi();
        sendPlayerDetails(me);
        int cooldown = me.getCharacter().getRecovery() * 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (me.getHealth() == 0) {
                    return;
                }
                me.setCombatMode(Player.COMBAT_MODE_NONE);
                updateLocalPlayerUi();
                sendPlayerDetails(me);
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
        sendPlayerDetails(me);
        int cooldown = me.getCharacter().getRecovery() * 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (me.getHealth() == 0) {
                    return;
                }
                me.setCombatMode(Player.COMBAT_MODE_NONE);
                updateLocalPlayerUi();
                sendPlayerDetails(me);
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
        enablePlayersButtons(true);
        updateLocalPlayerUi();
        sendPlayerDetails(me);
    }

    /**
     * Change the status of the other players buttons (for choosing who to attack)
     * <p>
     * We never enable buttons for dead players, that's just wrong.
     *
     * @param enabled: if true, the user can press the button to attack
     */
    private void enablePlayersButtons(boolean enabled) {
        for (int i = 0; i < otherPlayers.size(); i++) {
            LinearLayout container = (LinearLayout) otherPlayersLayout.getChildAt(i);
            ImageButton button = (ImageButton) container.getChildAt(0);
            // Make sure dead players are not re-enabled
            if (otherPlayers.get(i).getHealth() == 0) {
                button.setEnabled(false);
            } else {
                button.setEnabled(enabled);
            }
        }
    }

    private void waitForYourFate() {
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
            } while (attacker.getHealth() == 0);
            String msg_without_name = getString(R.string.fate_attacked);
            String msg_with_name = String.format(msg_without_name, attacker.getName());
            questionText.setText(msg_with_name);

            me.setHealth(me.getHealth() - 1);

            // Animate the heart
            localPlayerHealth.startAnimation(fadeOutAnimation);

            // Animate the characters (hurt or death)
            LinearLayout attackerContainer = (LinearLayout) otherPlayersLayout.getChildAt(attacker_index);
            ImageButton attackerView = (ImageButton) attackerContainer.getChildAt(0);
            zoomImageFromThumb(attackerView, attacker, new Runnable() {
                @Override
                public void run() {
                    announceDamage();
                }
            }, false);
        } else {
            questionText.setText(R.string.fate_spared);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    newQuestion();
                }
            }, MESSAGE_DURATION_MS);
        }
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
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    newQuestion();
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
            enablePlayersButtons(false);
            if (isTaskMaster) {
                me.attack(player);
                // TODO startAttackAnimation(player);
                sendPlayerDetails(player);
            } else {
                GameMessage msg = new GameMessage();
                msg.setType(GameMessage.GAME_MESSAGE_TYPE_ATTACK);
                msg.attackInfo.victim = player.getName();
                broadcastMessage(msg);
                // TODO startAttackAnimation(player);
            }
            buttonHeal.setEnabled(true);
            buttonAttack.setEnabled(true);
            buttonDefend.setEnabled(true);
            return;
        }

        // Check if it was an answer button
        QuizPool.Answer answer = (QuizPool.Answer) v.getTag(R.id.id_answer);
        if (answer != null) {
            if (state != STATE_WAITING_FOR_ANSWER) {
                Log.e(TAG, "Unexpected answer click while in state " + state);
                return;
            }
            state = STATE_WAITING_FOR_QUESTION;

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

            if (isTaskMaster) {
                // Wait for all players to answer, then send a new question
                handler.post(doCheckAllPlayersAnswered);
            }
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
        } else {
            newQuestion();
        }
    }

    private void removePlayer(String name) {
        // Disable the victim
        recursiveSetEnabled(false, victim_container);
        // Check if we won
        boolean everybodyDead = true;
        for (Player player : otherPlayers) {
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
     * @param endpointId the sender
     * @param payload    the message
     */
    private void onPayloadReceivedInternal(String endpointId, Payload payload) {
        if (payload.getType() != Payload.Type.BYTES) {
            Log.e(TAG, "Cannot handle messages of type " + payload.getType());
            return;
        }
        GameMessage msg = GameMessage.fromBytes(payload.asBytes());
        if (msg == null) {
            Log.e(TAG, "Cannot parse messages from endpoint " + endpointId);
            cleanupAndDisconnectEndpoint(endpointId);
            return;
        }
        switch (msg.getType()) {
            case GameMessage.GAME_MESSAGE_TYPE_PLAYER_INFO:
                // The first time we receive a player's info, we add it to the UI
                Player player = getPlayerByName(msg.playerInfo.name);
                if (player == null) {
                    // Check if we already reached the maximum number of players
                    if (otherPlayers.size() == MAX_PLAYERS) {
                        connectionsClient.disconnectFromEndpoint(endpointId);
                        if (isTaskMaster)
                            connectionsClient.stopAdvertising();
                        else
                            connectionsClient.stopDiscovery();
                        return;
                    }
                    addPlayer(msg.playerInfo.character, msg.playerInfo.name);
                    player = getPlayerByName(msg.playerInfo.name);
                }

                // Don't listen to what other players think their health is. Only taskmaster knows the truth
                if (!isTaskMaster) {
                    player.setHealth(msg.playerInfo.health);
                }
                player.setCombatMode(msg.playerInfo.battle);
                updateOtherPlayersUi();

                if (isTaskMaster) {
                    // Forward the message to the other players (except the sender)
                    for (String recipient : mEstablishedConnections.keySet()) {
                        if (!endpointId.equals(recipient)) {
                            sendMessage(recipient, msg);
                        }
                    }
                }
                break;
            case GameMessage.GAME_MESSAGE_TYPE_ATTACK:
                if (!isTaskMaster) {
                    Log.e(TAG, "Only taskmaster receives attack requests");
                    return;
                }
                Player attacker = getPlayerByEndpoint(endpointId);
                Player victim = getPlayerByName(msg.attackInfo.victim);
                if (attacker == null || victim == null) {
                    return;
                }
                attacker.attack(victim);
                sendPlayerDetails(victim);
                break;
            default:
                Log.wtf(TAG, "Unhandled game message type: " + msg.getType());
        }
    }

    private Player getPlayerByEndpoint(String endpointId) {
        if (me.getEndpointId().equals(endpointId)) {
            return me;
        }
        for (Player player: otherPlayers) {
            if (player.getEndpointId().equals(endpointId)) {
                return player;
            }
        }
        return null;
    }

    private Player getPlayerByName(String name) {
        if (me.getName().equals(name)) {
            return me;
        }
        for (Player player: otherPlayers) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    private void broadcastMessage(GameMessage msg) {
        for (String endpointId : mEstablishedConnections.keySet()) {
            sendMessage(endpointId, msg);
        }
    }

    private void sendMessage(String endpointId, GameMessage msg) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(msg.toBytes()));
    }

    private void sendPlayerDetails(Player player) {
        GameMessage msg = new GameMessage();
        msg.setType(GameMessage.GAME_MESSAGE_TYPE_PLAYER_INFO);
        msg.playerInfo.name = player.getName();
        msg.playerInfo.character = player.getCharacterName();
        msg.playerInfo.health = player.getHealth();
        msg.playerInfo.battle = player.getCombatMode();
        broadcastMessage(msg);
    }

    private void cleanupAndDisconnectEndpoint(String endpointId) {
        String name = mEstablishedConnections.get(endpointId);
        connectionsClient.disconnectFromEndpoint(endpointId);
        mEstablishedConnections.remove(endpointId);
        removePlayer(name);
    }

    // --- A bunch of "Internal" functions used by the connection callbacks ----

    /**
     * This is called when a device finds the taskmaster
     * @param endpointId a unique string representing the endpoint found (taskmaster)
     * @param info (unused)
     */
    private void onEndpointFoundInternal(String endpointId, DiscoveredEndpointInfo info) {
        Log.i(TAG, "Found taskmaster");
        // Immediately request connection and stop looking
        connectionsClient.requestConnection(me.getName(), endpointId, connectionLifecycleCallback);
        connectionsClient.stopDiscovery();
    }

    /**
     * This is called when the device looses the taskmaster
     * @param endpointId (unused)
     */
    private void onEndpointLostInternal(String endpointId) {
        Log.e(TAG, "Lost taskmaster!");
        finish();
    }

    private void onConnectionInitiatedInternal(String endpointId, ConnectionInfo connectionInfo) {
        if (isTaskMaster) {
            if (me.getName().equals(connectionInfo.getEndpointName())) {
                Log.e(TAG, "This game is too small for both of us " + connectionInfo.getEndpointName());
                connectionsClient.rejectConnection(endpointId);
                return;
            }
            for (Player player : otherPlayers) {
                if (player.getName().equals(connectionInfo.getEndpointName())) {
                    Log.e(TAG, "Sorry, we already have a player named " + connectionInfo.getEndpointName());
                    connectionsClient.rejectConnection(endpointId);
                    return;
                }
            }
        }
        Log.i(TAG, "onConnectionInitiated: accepting connection");
        connectionsClient.acceptConnection(endpointId, payloadCallback);
        mPendingConnections.put(endpointId, connectionInfo.getEndpointName());
    }

    private void onConnectionResultInternal(String endpointId, ConnectionResolution result) {
        if (result.getStatus().isSuccess()) {
            Log.i(TAG, "onConnectionResult: connection successful");
            mEstablishedConnections.put(endpointId, mPendingConnections.remove(endpointId));
            // Once the connection is established, we send our player info to the taskmaster
            if (!isTaskMaster) {
                sendPlayerDetails(me);
            }
        } else {
            Log.i(TAG, "onConnectionResult: connection failed");
            mPendingConnections.remove(endpointId);
        }
    }

    private void onDisconnectedInternal(String endpointId) {
        if (isTaskMaster) {
            Player player;
            for (int i = 0; i < otherPlayers.size(); i++) {
                player = otherPlayers.get(i);
                if (player.getName().equals(mEstablishedConnections.get(endpointId))) {
                    player.setHealth(0);
                    victim_container = (LinearLayout) otherPlayersLayout.getChildAt(i);
                    ImageButton imageButton = (ImageButton) victim_container.getChildAt(0);
                    imageButton.setImageResource(player.getCharacter().getDeadImageId());
                    ProgressBar healthBar = (ProgressBar) victim_container.getChildAt(2);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        healthBar.setProgress(0, true);
                    } else {
                        healthBar.setProgress(0);
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            removePlayer(victim.getName());
                        }
                    }, LONG_MESSAGE_DURATION_MS);
                    break;
                }
            }
        } else {
            finish();
        }
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
}
package com.sombright.simultanea;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.sombright.simultanea.Constants.SERVICE_ID;
import static com.sombright.simultanea.Constants.STRATEGY;
import static com.sombright.simultanea.MainActivity.TAG;

public class TaskMasterActivity extends ConnectionsActivity implements PlayersViewAdapter.OnClickPlayerListener, OpenTriviaDatabase.Listener {
    private static final String TAG = "TaskMaster";

    private PreferencesProxy mPrefs;
    private PlayersViewAdapter mPlayersViewAdapter;
    private GridView mPlayersView;
    private Button mButtonStartGame, mButtonQuestion, mButtonBattle;
    private LinearLayout mQuestionsLayout;
    private QuizPool quizPool = null;
    private OpenTriviaDatabase opentdb = null;
    private Random random = new Random();
    private Spinner mQuestionCategorySpinner;
    private Spinner mQuestionDifficultySpinner;
    private Spinner mQuestionTypeSpinner;
    private Button mButtonApply, mButtonPickQuestion;
    private ArrayAdapter<CharSequence> mQuestionCategoryAdapter;
    private ArrayAdapter<CharSequence> mQuestionDifficultyAdapter;
    private ArrayAdapter<CharSequence> mQuestionTypeAdapter;
    private TextView mQuestionTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_master);
        mPlayersView = findViewById(R.id.otherPlayersLayout);
        mButtonStartGame = findViewById(R.id.buttonStartGame);
        mButtonQuestion = findViewById(R.id.buttonQuestion);
        mQuestionsLayout = findViewById(R.id.questionLayout);
        mQuestionCategorySpinner = findViewById(R.id.spinnerCategory);
        mQuestionDifficultySpinner = findViewById(R.id.spinnerDifficulty);
        mQuestionTypeSpinner = findViewById(R.id.spinnerType);
        mButtonApply = findViewById(R.id.buttonApply);
        mButtonPickQuestion = findViewById(R.id.buttonPickQuestion);
        mQuestionTextView = findViewById(R.id.questionTextView);

        mPrefs = new PreferencesProxy(this);
        mPlayersViewAdapter = new PlayersViewAdapter(this, R.layout.players_example_item, this);
        mPlayersView.setAdapter(mPlayersViewAdapter);
        //quizPool = new QuizPool(this);
        startAdvertising();

        mQuestionsLayout.setVisibility(View.GONE);
        if (mPrefs.shouldUseOpenTriviaDatabase()) {
            opentdb = new OpenTriviaDatabase(this);
            opentdb.setListener(this);
        } else {
            quizPool = new QuizPool(this);
            mQuestionCategorySpinner.setEnabled(false);
            mQuestionDifficultySpinner.setEnabled(false);
            mQuestionTypeSpinner.setEnabled(false);
            mButtonApply.setEnabled(false);
            mButtonPickQuestion.setEnabled(true);
        }

        // Category Spinner
        mQuestionCategoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mQuestionCategoryAdapter.add(getString(R.string.question_category_any));
        mQuestionCategorySpinner.setAdapter(mQuestionCategoryAdapter);

        // Difficulty Spinner
        mQuestionDifficultyAdapter = ArrayAdapter.createFromResource(this, R.array.question_difficulty_array, android.R.layout.simple_spinner_item);
        mQuestionDifficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mQuestionDifficultySpinner.setAdapter(mQuestionDifficultyAdapter);
        mQuestionDifficultySpinner.setSelection(OpenTriviaDatabase.DIFFICULTY_EASY);

        // Question Type Spinner
        mQuestionTypeAdapter = ArrayAdapter.createFromResource(this, R.array.question_type_array, android.R.layout.simple_spinner_item);
        mQuestionTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mQuestionTypeSpinner.setAdapter(mQuestionTypeAdapter);
        mQuestionTypeSpinner.setSelection(OpenTriviaDatabase.TYPE_MULTIPLE_CHOICE);
    }

    @Override
    protected String getName() {
        Log.d(TAG, "Name=" + mPrefs.getMultiPlayerAlias());
        return mPrefs.getMultiPlayerAlias();
    }

    @Override
    protected String getServiceId() {
        Log.d(TAG, "SERVICE_ID=" + SERVICE_ID);
        return SERVICE_ID;
    }

    @Override
    protected Strategy getStrategy() {
        Log.d(TAG, "STRATEGY=" + STRATEGY);
        return STRATEGY;
    }

    @Override
    protected void onAdvertisingStarted() {
        Log.v(TAG, "onAdvertisingStarted");
        Toast.makeText(this, R.string.waiting_for_players, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onAdvertisingFailed() {
        Log.e(TAG, "onAdvertisingFailed");
        Toast.makeText(this, R.string.on_advertising_failed, Toast.LENGTH_LONG).show();
        // Schedule the finish for later (let the user see the Toast.
        if (!BuildConfig.DEBUG) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3500);
        }
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        Log.v(TAG, "onConnectionInitiated: " + endpoint.getName());
        // For now, let's accept everyone automatically
        acceptConnection(endpoint);
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        Log.e(TAG, "onConnectionFailed: " + endpoint.getName());
        String msg = String.format(getString(R.string.connection_failed), endpoint.getName());
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Log.v(TAG, "onEndpointConnected: " + endpoint.getName());
        Player player = new Player(this);
        player.setEndpoint(endpoint);
        player.setName(endpoint.getName());
        mPlayersViewAdapter.add(player);
        if (mPlayersViewAdapter.getCount() == 2) {
            mButtonStartGame.setText(R.string.start_game);
            mButtonStartGame.setEnabled(true);
        }
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Log.v(TAG, "onEndpointDisconnected: " + endpoint.getName());
        Player player = mPlayersViewAdapter.getPlayerByEndpointId(endpoint.getId());
        mPlayersViewAdapter.remove(player);
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        GameMessage msg = GameMessage.fromBytes(payload.asBytes());
        assert msg != null;
        switch (msg.getType()) {
            case GameMessage.GAME_MESSAGE_TYPE_PLAYER_INFO:
                onReceivePlayerInfo(endpoint, msg);
                break;
            case GameMessage.GAME_MESSAGE_TYPE_ANSWER:
                onReceiveAnswer(endpoint, msg);
                break;
            case GameMessage.GAME_MESSAGE_TYPE_ATTACK:
                onReceiveAttackInfo(endpoint, msg);
                break;
        }
    }

    private void onReceivePlayerInfo(Endpoint endpoint, GameMessage msg) {
        Log.d(TAG, "onReceivePlayerInfo");
        Player player = mPlayersViewAdapter.getPlayerByEndpointId(endpoint.getId());
        boolean isNew = player.getCharacter() == null;
        player.setPlayerDetails(msg);
        mPlayersViewAdapter.notifyDataSetChanged();
        broadcastMessage(msg);
        // Inform new player of other players already connected
        if (isNew) {
            for (int i = 0; i < mPlayersViewAdapter.getCount(); i++) {
                player = mPlayersViewAdapter.getItem(i);
                if (player.getEndpoint() != endpoint) {
                    msg = player.getPlayerDetails();
                    send(Payload.fromBytes(msg.toBytes()), endpoint);
                }
            }
        }
    }

    private void onReceiveAnswer(Endpoint endpoint, GameMessage msg) {
        Log.d(TAG, "onReceiveAnswer");
        Player player = mPlayersViewAdapter.getPlayerByEndpointId(endpoint.getId());
        player.setAnswered(true);
        if (msg.answerInfo.correct) {
            player.setPoints(player.getPoints() + 1);
            sendPlayerDetails(player);
        }
    }

    private void sendPlayerDetails(Player player) {
        Log.d(TAG, "sendPlayerDetails" + player.getName());
        GameMessage msg = player.getPlayerDetails();
        send(Payload.fromBytes(msg.toBytes()));
    }

    private void onReceiveAttackInfo(Endpoint endpoint, GameMessage msg) {
        Log.d(TAG, "onReceiveAttack");
        Player player = mPlayersViewAdapter.getPlayerByEndpointId(endpoint.getId());
        Player victim = mPlayersViewAdapter.getPlayer(msg.attackInfo.victimId);

        // Calculate results and inform both parties
        msg.attackInfo.attackerId = player.getUniqueID();
        msg.attackInfo.defending = victim.getCombatMode() == Player.COMBAT_MODE_DEFEND;
        int damage = player.getCharacter().getAttack();
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
        player.setPoints(player.getPoints()-1);
        // Send attack details
        send(Payload.fromBytes(msg.toBytes()));
        // Send updated player info
        sendPlayerDetails(victim);
        sendPlayerDetails(player);
    }

    /**
     * Called when the taskmaster clicks the start game button.
     * @param view: the start game button
     */
    public void startGame(View view) {
        Log.v(TAG, "startGame");
        mButtonStartGame.setVisibility(View.GONE);
        mQuestionsLayout.setVisibility(View.VISIBLE);
        stopAdvertising();
        startRound();
    }

    public void startRound() {
        Log.v(TAG, "startRound");
        // TaskMaster chooses a question type
    }
/*
    private void newQuestion() {
        Log.v(TAG, "newQuestion");
        QuizPool.Entry entry = quizPool.getQuestion();
        GameMessage msg = new GameMessage();
        msg.setType(GameMessage.GAME_MESSAGE_TYPE_QUESTION);
        msg.questionInfo.entry = entry;
        mPlayersViewAdapter.setAnswered(false);
        broadcastMessage(msg);
        onReceiveQuestion(msg);
    }
*/
    @Override
    public void onClickPlayer(Player player) {
        Log.v(TAG, "onClickPlayer: " + player.getName());
    }

    public void showQuestion(View v) {
        Log.d(TAG, "showQuestion");
    }

    public void onClickApply(View v) {
        if (opentdb != null) {
            opentdb.setQuestionAttributes(
                    mQuestionCategorySpinner.getSelectedItemPosition() - 1,
                    mQuestionDifficultySpinner.getSelectedItemPosition(),
                    mQuestionTypeSpinner.getSelectedItemPosition());
        }
        mQuestionTextView.setText(null);
    }

    @Override
    public void onCategoriesChanged(List<String> categories) {
        mQuestionCategoryAdapter.setNotifyOnChange(false);
        mQuestionCategoryAdapter.clear();
        mQuestionCategoryAdapter.add(getString(R.string.question_category_any));
        mQuestionCategoryAdapter.addAll(categories);
        mQuestionCategoryAdapter.setNotifyOnChange(true);
        mQuestionCategoryAdapter.notifyDataSetChanged();
        if (mQuestionCategoryAdapter.getCount() > 1) {
            mQuestionCategorySpinner.setSelection(1);
            onClickApply(null);
        }
    }

    @Override
    public void onQuestionsAvailable(boolean available) {
        mButtonPickQuestion.setEnabled(available);
    }

    public void onClickPickQuestion(View v) {
        mPlayersViewAdapter.setAnswered(false);
        QuizPool.Entry entry;
        if (opentdb != null) {
            OpenTriviaDatabase.Question question = opentdb.getQuestion();
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
        mQuestionTextView.setText(entry.question);
        GameMessage msg = new GameMessage();
        msg.setType(GameMessage.GAME_MESSAGE_TYPE_QUESTION);
        msg.questionInfo.entry = entry;
        broadcastMessage(msg);
    }

    private void broadcastMessage(GameMessage msg) {
        Log.d(TAG, "broadcastMessage");
        if (!getConnectedEndpoints().isEmpty())
            send(Payload.fromBytes(msg.toBytes()));
    }
}

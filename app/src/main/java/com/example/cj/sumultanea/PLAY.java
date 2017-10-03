package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.cj.sumultanea.simultanea.CHARACTER1;
import static com.example.cj.sumultanea.simultanea.CHARACTER3;
import static com.example.cj.sumultanea.simultanea.CHARACTER4;
import static com.example.cj.sumultanea.simultanea.TAG;


public class PLAY extends AppCompatActivity {
    private QuizPool quizPool;
    private Random random;
    TextView questionText;
    private LinearLayout answersLayout;
    private Player me;
    private List<Player> otherPlayers = new ArrayList<>();
    private LinearLayout otherPlayersLayout;
    private LinearLayout localPlayerLivesLayout;
    private Handler handler = new Handler();
    private MediaPlayer ring;
    private final static int MESSAGE_DURATION_MS = 1500;
    private final static int LONG_MESSAGE_DURATION_MS = 3000;
    private Animation fadeOutAnimation;
    private ImageView fadingLifeImg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the character selection sent by the main activity as part of the "intent"
        Intent intent = getIntent();
        int character = intent.getIntExtra(simultanea.CHARACTER_KEY, CHARACTER1);
        me = new Player(this, character, "Me");
        Log.d(TAG, "Playing as character " + character);

        // Apply the initial layout (we will modify below)
        setContentView(R.layout.activity_play);

        // These are the portions of the layout that we will modify during the game
        questionText = findViewById(R.id.textView4);
        answersLayout = findViewById(R.id.answersLayout);
        otherPlayersLayout = findViewById(R.id.otherPlayersLayout);
        localPlayerLivesLayout = findViewById(R.id.localPlayerLivesLayout);

        // Set our character image
        ImageView imageView = findViewById(R.id.imageView2);
        imageView.setImageDrawable(me.animation);
        me.animation.start();

        // Display our lives
        // Remove the fake content we put in the initial layout (for designing)
        localPlayerLivesLayout.removeAllViews();
        for (int i=0; i<me.lives; i++) {
            ImageView lifeImg = new ImageView(this);
            lifeImg.setImageResource(R.drawable.heart);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(30, 30);
            lp.gravity = Gravity.CENTER;
            lifeImg.setLayoutParams(lp);
            //lifeImg.setAdjustViewBounds(true);
            localPlayerLivesLayout.addView(lifeImg);
        }

        // Add fake players for now
        otherPlayers.add(new Player(this, CHARACTER3, "Arthur"));
        otherPlayers.add(new Player(this, CHARACTER4, "Delia"));
        otherPlayers.add(new Player(this, CHARACTER1, "Joaquin"));

        // Remove the fake content we put in the initial layout (for designing)
        otherPlayersLayout.removeAllViews();
        // Fill other players row
        for (Player player: otherPlayers) {
            ImageButton btn = new ImageButton(this);

            // Attach the player data to the button
            btn.setTag(R.id.id_player, player);

            // Prepare the button style
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btn.setScaleType(ImageView.ScaleType.CENTER_CROP);
            btn.setImageDrawable(player.animation);
            player.animation.start();
            btn.setAdjustViewBounds(true);
            btn.setEnabled(false);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(onClickOtherPlayer);

            // Add the button to the players row
            otherPlayersLayout.addView(btn);
        }

        // Let's get started!
        quizPool = new QuizPool(this);
        random = new Random();
        newQuestion();
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

    private void newQuestion() {
        QuizPool.Entry currentQuestion = quizPool.getQuestion();
        questionText.setText(currentQuestion.question);

        // We clear-out the old buttons, and create new ones for the current question
        answersLayout.removeAllViews();
        int count = 0;
        for (QuizPool.Answer answer: currentQuestion.answers) {
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
        answersLayout.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener onClickAnswer = new View.OnClickListener() {
        public void onClick(View v) {
            // First thing: prevent user from clicking other answers while we handle this one.
            disableAnswerButtons();
            // Retrieve the answer associated with this button
            QuizPool.Answer answer = (QuizPool.Answer) v.getTag(R.id.id_answer);
            if (answer.correct) {
                Log.d(TAG, "Correct!");
                questionText.setText(R.string.answer_correct);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.GREEN, 150));
                enablePlayersButtons(true);
            } else {
                Log.d(TAG, "Incorrect!");
                questionText.setText(R.string.answer_incorrect);
                v.setBackgroundColor(ColorUtils.setAlphaComponent(Color.RED, 150));
                handler.postDelayed(waitForYourFate, MESSAGE_DURATION_MS);
            }
        }
    };

    private void disableAnswerButtons() {
        for (int i=0; i<answersLayout.getChildCount(); i++) {
            View v = answersLayout.getChildAt(i);
            v.setEnabled(false);
        }
    }

    private void enablePlayersButtons(boolean enabled) {
        for (int i=0; i<otherPlayersLayout.getChildCount(); i++) {
            View v = otherPlayersLayout.getChildAt(i);
            v.setEnabled(enabled);
        }
    }

    private Runnable waitForYourFate = new Runnable() {
        @Override
        public void run() {
            if (random.nextInt(2) == 0) {
                questionText.setText(R.string.fate_attacked);
                if (fadeOutAnimation == null) {
                    fadeOutAnimation = new AlphaAnimation(1, 0);
                    fadeOutAnimation.setDuration(MESSAGE_DURATION_MS / 6);
                    fadeOutAnimation.setInterpolator(new LinearInterpolator());
                    fadeOutAnimation.setRepeatCount(Animation.INFINITE);
                    fadeOutAnimation.setRepeatMode(Animation.REVERSE);
                }
                me.lives--;
                fadingLifeImg = (ImageView) localPlayerLivesLayout.getChildAt(me.lives);
                fadingLifeImg.startAnimation(fadeOutAnimation);
                handler.postDelayed(doAnnounceDamage, MESSAGE_DURATION_MS);
            } else {
                questionText.setText(R.string.fate_spared);
                handler.postDelayed(doNextQuestion, MESSAGE_DURATION_MS);
            }
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
                String msg_without_lives = getResources().getString(R.string.game_not_over);
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
    private View victim_view = null;
    private View.OnClickListener onClickOtherPlayer = new View.OnClickListener() {
        public void onClick(View v) {
            enablePlayersButtons(false);
            // Check which answer correspond that button.
            victim = (Player) v.getTag(R.id.id_player);
            victim_view = v;
            String msg_without_player = getResources().getString(R.string.do_attack);
            String msg_with_player = String.format(msg_without_player, victim.name);
            questionText.setText(msg_with_player);
            handler.postDelayed(doAttackOtherPlayer, MESSAGE_DURATION_MS);
        }
    };

    private Runnable doAttackOtherPlayer = new Runnable() {
        @Override
        public void run() {
            if (victim.lives == 1) {
                String msg_without_victim = getResources().getString(R.string.attack_killed);
                String msg_with_victim = String.format(msg_without_victim, victim.name);
                questionText.setText(msg_with_victim);
                handler.postDelayed(doRemovePlayer, MESSAGE_DURATION_MS);
            } else {
                victim.lives--;
                newQuestion();
            }
        }
    };

    private Runnable doRemovePlayer = new Runnable() {
        @Override
        public void run() {
            otherPlayersLayout.removeView(victim_view);
            otherPlayers.remove(victim);
            if (otherPlayers.isEmpty()) {
                questionText.setText(R.string.game_won);
                handler.postDelayed(doFinishGame, LONG_MESSAGE_DURATION_MS);
            } else {
                newQuestion();
            }
        }
    };
}

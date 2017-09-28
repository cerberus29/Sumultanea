package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.cj.sumultanea.simultanea.CHARACTER1;
import static com.example.cj.sumultanea.simultanea.CHARACTER2;
import static com.example.cj.sumultanea.simultanea.CHARACTER3;
import static com.example.cj.sumultanea.simultanea.CHARACTER4;
import static com.example.cj.sumultanea.simultanea.TAG;


public class PLAY extends AppCompatActivity implements View.OnClickListener {
    private QuizPool quizPool;
    private Random random;
    private QuizPool.Entry currentQuestion;
    TextView questionText;
    private LinearLayout answersLayout;
    private Player me;
    private List<Player> otherPlayers = new ArrayList<>();
    private LinearLayout otherPlayersLayout;
    private Handler handler = new Handler();
    private MediaPlayer ring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ring = MediaPlayer.create(this, R.raw.fight2);
        ring.setLooping(true);
        ring.start();

        Intent intent = getIntent();
        int character = intent.getIntExtra(simultanea.CHARACTER_KEY, CHARACTER1);
        me = new Player(this, character);
        Log.d(TAG, "Playing as character " + character);
        setContentView(R.layout.activity_play);
        questionText = (TextView) findViewById(R.id.textView4);
        answersLayout = (LinearLayout) findViewById(R.id.answersLayout);
        otherPlayersLayout = (LinearLayout) findViewById(R.id.otherPlayersLayout);
        ImageView imageView = (ImageView) findViewById(R.id.imageView2);
        imageView.setImageDrawable(me.animation);
        me.animation.start();
        quizPool = new QuizPool(this);
        random = new Random();

        // Add fake players for now
        otherPlayers.add(new Player(this, CHARACTER3));
        otherPlayers.add(new Player(this, CHARACTER4));
        otherPlayers.add(new Player(this, CHARACTER1));
        // Remove the fake content used for designing the layout, then add our players buttons
        otherPlayersLayout.removeAllViews();
        for (Player player: otherPlayers) {
            ImageButton btn = new ImageButton(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btn.setScaleType(ImageView.ScaleType.CENTER_CROP);
            btn.setImageDrawable(player.animation);
            player.animation.start();
            btn.setAdjustViewBounds(true);
            btn.setEnabled(false);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(this);
            otherPlayersLayout.addView(btn);
        }

        newQuestion();
    }

    private void newQuestion() {
        answersLayout.setEnabled(false);
        currentQuestion = quizPool.getQuestion();
        questionText.setText(currentQuestion.question);

        // We clear-out the old buttons, and create new ones for the current question
        answersLayout.removeAllViews();
        int count = 0;
        for (QuizPool.Answer answer: currentQuestion.answers) {
            Button button = new Button(this);
            button.setText(answer.text);
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
        answersLayout.setVisibility(View.VISIBLE);
    }

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

    @Override
    public void onClick(View v) {
        if (v.getParent() == answersLayout) {
            disableAnswerButtons();
            Button button = (Button) v;
            // Check which answer correspond that button.
            for (QuizPool.Answer answer : currentQuestion.answers) {
                if (answer.text.equals(button.getText())) {
                    if (answer.correct) {
                        Log.d(TAG, "Correct!");
                        questionText.setText("Correct! Please choose a player to attack.");
                        enablePlayersButtons(true);
                    } else {
                        Log.d(TAG, "Incorrect!");
                        questionText.setText("Incorrect! Please wait for other players to decide your fate.");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (random.nextInt(2) == 0) {
                                    questionText.setText("You got attacked");
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (me.lives == 1) {
                                                questionText.setText("You have no more lives, GAME OVER!");
                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ring.stop();
                                                        finish();
                                                    }
                                                }, 1500);
                                            } else {
                                                me.lives--;
                                                questionText.setText("You have " + me.lives + " lives left.");
                                                handler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        newQuestion();
                                                    }
                                                }, 1500);
                                            }
                                        }
                                    }, 1500);
                                } else {
                                    questionText.setText("You got spared, this time...");
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            newQuestion();
                                        }
                                    }, 1500);
                                }
                            }
                        }, 1500);
                    }
                    return;
                }
            }
        } else if (v.getParent() == otherPlayersLayout) {
            enablePlayersButtons(false);
            final ImageButton button = (ImageButton) v;
            // Check which answer correspond that button.
            final int index = otherPlayersLayout.indexOfChild(v);
            final Player player = otherPlayers.get(index);
            questionText.setText("Attacking player " + index);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (player.lives == 1) {
                        questionText.setText("You killed player " + index);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                otherPlayersLayout.removeView(button);
                                otherPlayers.remove(index);
                                if (otherPlayers.isEmpty()) {
                                    questionText.setText("You are the last player standing, YOU WIN!!!");
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            ring.stop();
                                            finish();
                                        }
                                    }, 3000);
                                }
                            }
                        }, 1500);
                    } else {
                        player.lives--;
                    }
                    newQuestion();
                }
            }, 1500);
        }
    }
}

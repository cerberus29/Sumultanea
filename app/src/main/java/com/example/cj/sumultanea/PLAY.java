package com.example.cj.sumultanea;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    Button clk1, clk2;
    MediaPlayer mdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clk1 = (Button) findViewById(R.id.PLAY);
        clk2 = (Button) findViewById(R.id.CHARACTERS);



        MediaPlayer ring= MediaPlayer.create(PLAY.this,R.raw.fight2);
        ring.setLooping(true);
        ring.start();

        Intent intent = getIntent();
        int character = intent.getIntExtra(simultanea.CHARACTER_KEY, CHARACTER1);
        Log.d(TAG, "Playing as character " + character);
        setContentView(R.layout.activity_play);
        ImageView imageView = (ImageView) findViewById(R.id.imageView2);
        imageView.clearAnimation();

        switch (character) {
            case CHARACTER1:
                imageView.setImageResource(R.drawable.bluepanda);
                break;
            case CHARACTER2:
                imageView.setImageResource(R.drawable.duel);
                break;
            case CHARACTER3:
                imageView.setImageResource(R.drawable.pump);
                break;
            case CHARACTER4:
                imageView.setImageResource(R.drawable.war);
                break;

        }
        AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
        anim.start();
        quizPool = new QuizPool(this);
        random = new Random();
        newQuestion();
    }

    private void newQuestion() {
        currentQuestion = quizPool.getQuestion();
        TextView questionText = (TextView) findViewById(R.id.textView4);
        questionText.setText(currentQuestion.question);
        LinearLayout answersLayout = (LinearLayout) findViewById(R.id.answersLayout);

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
    }

    @Override
    public void onClick(View v) {
        Button button = (Button) v;
        // Check which answer correspond that button.
        for (QuizPool.Answer answer: currentQuestion.answers) {
            if (answer.text.equals(button.getText())) {
                if (answer.correct) {
                    Log.d(TAG, "Correct!");
                    // TODO: increment score, display something
                } else {
                    Log.d(TAG, "Incorrect!");
                    // TODO: display something
                }
                newQuestion();
                return;
            }
        }
    }
}

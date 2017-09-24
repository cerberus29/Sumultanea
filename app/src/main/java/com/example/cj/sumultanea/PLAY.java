package com.example.cj.sumultanea;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.cj.sumultanea.R.styleable.View;
import static com.example.cj.sumultanea.simultanea.CHARACTER1;
import static com.example.cj.sumultanea.simultanea.CHARACTER2;
import static com.example.cj.sumultanea.simultanea.CHARACTER3;
import static com.example.cj.sumultanea.simultanea.CHARACTER4;
import static com.example.cj.sumultanea.simultanea.TAG;


public class PLAY extends AppCompatActivity {
    private QuizPool quizPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        newQuestion();
    }

    private void newQuestion() {
        QuizPool.Entry entry = quizPool.getQuestion();
        TextView questionText = (TextView) findViewById(R.id.textView4);
        questionText.setText(entry.question);
    }
}

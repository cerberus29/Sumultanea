package com.example.cj.sumultanea;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.cj.sumultanea.simultanea.DEFAULT_CHARACTER;
import static com.example.cj.sumultanea.simultanea.TAG;


public class PLAY extends AppCompatActivity {
    private QuizPool quizPool;
    private Random random = new Random();
    TextView questionText;
    private LinearLayout answersLayout;
    private Player me;
    private ImageView localPlayerThumb;
    private List<Player> otherPlayers = new ArrayList<>();
    private LinearLayout otherPlayersLayout;
    private LinearLayout localPlayerLivesLayout;
    private Handler handler = new Handler();
    private MediaPlayer ring;
    private final static int MESSAGE_DURATION_MS = 1500;
    private final static int LONG_MESSAGE_DURATION_MS = 3000;

    // Animation for when a player looses a life
    private Animation fadeOutAnimation;
    // This is to keep track of which heart is currently animated (only one at a time), so we can stop the animation later
    private ImageView fadingLifeImg = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the character selection sent by the main activity as part of the "intent"
        Intent intent = getIntent();
        int character = intent.getIntExtra(simultanea.CHARACTER_KEY, DEFAULT_CHARACTER);
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
        localPlayerThumb = findViewById(R.id.imageViewLocalPlayer);
        localPlayerThumb.setImageDrawable(me.animation);
        me.animation.start();

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

        // Add fake players for now
        otherPlayers.add(new Player(this, random.nextInt(CharacterPool.charactersList.length), "Jack"));
        otherPlayers.add(new Player(this, random.nextInt(CharacterPool.charactersList.length), "Eve"));
        otherPlayers.add(new Player(this, random.nextInt(CharacterPool.charactersList.length), "Pandora"));

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
            btn.setScaleType(ImageView.ScaleType.CENTER_CROP);
            btn.setImageDrawable(player.animation);
            player.animation.start();
            btn.setAdjustViewBounds(true);
            btn.setEnabled(false);
            btn.setLayoutParams(new LinearLayout.LayoutParams(170, 170));
            btn.setOnClickListener(onClickOtherPlayer);
            // Add the player button to the vertical container (on top of the lives)
            playerAndLivesContainer.addView(btn);

            // Sub-Container for 3 lives (row)
            LinearLayout livesContainer = new LinearLayout(this);
            // Horizontal, because we want the hearts lined-up side-by-side
            livesContainer.setOrientation(LinearLayout.HORIZONTAL);
            // Center horizontally based on parent container width
            livesContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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

        // Prepare an animation object for when we loose a life
        fadeOutAnimation = new AlphaAnimation(1, 0);
        fadeOutAnimation.setDuration(MESSAGE_DURATION_MS / 6);
        fadeOutAnimation.setInterpolator(new LinearInterpolator());
        fadeOutAnimation.setRepeatCount(Animation.INFINITE);
        fadeOutAnimation.setRepeatMode(Animation.REVERSE);

        // Let's get started!
        quizPool = new QuizPool(this);
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
        for (QuizPool.Answer answer : currentQuestion.answers) {
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

    private void disableAnswerButtons() {
        recursiveSetEnabled(false, answersLayout);
    }

    private void enablePlayersButtons(boolean enabled) {
        recursiveSetEnabled(enabled, otherPlayersLayout);
    }

    private Runnable waitForYourFate = new Runnable() {
        @Override
        public void run() {
            if (random.nextInt(2) == 0) {
                questionText.setText(R.string.fate_attacked);
                me.lives--;

                // Animate the heart
                fadingLifeImg = (ImageView) localPlayerLivesLayout.getChildAt(me.lives);
                fadingLifeImg.startAnimation(fadeOutAnimation);

                // Animate the character (hurt or death)
                zoomImageFromThumb(localPlayerThumb, me, doAnnounceDamage);
            } else {
                questionText.setText(R.string.fate_spared);
                handler.postDelayed(doNextQuestion, MESSAGE_DURATION_MS);
            }
        }
    };

    /**
     * Animation code from https://developer.android.com/training/animation/zoom.html
     * Modifications:
     * - Do not allow cancelling the animation (removed mCurrentAnimator).
     * - Make the animation time longer (removed mShortAnimationDuration).
     * - Add a character animation (hurt or die) between the zoom-in and zoom-out.
     * - Launch the next animation immediately (replace OnClickListener with postDelayed).
     */
    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator mCurrentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int mShortAnimationDuration = 500;

    // Some variables we need to keep for the zoom-out animation
    private View mCurrentAnimThumbView;
    private Player mCurrentAnimPlayer;
    private ImageView expandedImageView;
    private Rect startBounds;
    private float startScaleFinal;
    private Runnable mDoThisAfterAnimation;

    private void zoomImageFromThumb(final View thumbView, final Player player, Runnable doThisAfterAnimation) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Save a few things for the zoom-out animation
        mCurrentAnimThumbView = thumbView;
        mCurrentAnimPlayer = player;
        mDoThisAfterAnimation = doThisAfterAnimation;
        int imageResId = player.mCharacter.getImageResource();

        // Load the high-resolution "zoomed-in" image.
        expandedImageView = (ImageView) findViewById(
                R.id.expanded_image);
        expandedImageView.setImageResource(imageResId);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.questionLayout)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        //startBounds.offset(-globalOffset.x, -globalOffset.y);
        //finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        final float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
                // Start the hurt/death animation now
                int resId;
                if (player.lives > 0) {
                    resId = player.mCharacter.getImageResourceHurt();
                } else {
                    resId = player.mCharacter.getImageResourceDeath();
                }
                expandedImageView.setImageResource(resId);
                AnimationDrawable anim = (AnimationDrawable) expandedImageView.getDrawable();
                anim.start();
                // Let the hurt/death animation run for a certain time before zooming out
                if (player.lives > 0) {
                    handler.postDelayed(zoomImageBackToThumb, 1000);
                } else {
                    // Death animation takes a bit longer
                    handler.postDelayed(zoomImageBackToThumb, 2000);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon finishing the hurt/death animation, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        startScaleFinal = startScale;
    }

    private Runnable zoomImageBackToThumb = new Runnable() {
        @Override
        public void run() {
            if (mCurrentAnimator != null) {
                mCurrentAnimator.cancel();
            }

            // Stop the hurt or death animation
            AnimationDrawable anim = (AnimationDrawable) expandedImageView.getDrawable();
            anim.stop();

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator
                    .ofFloat(expandedImageView, View.X, startBounds.left))
                    .with(ObjectAnimator
                            .ofFloat(expandedImageView,
                                    View.Y,startBounds.top))
                    .with(ObjectAnimator
                            .ofFloat(expandedImageView,
                                    View.SCALE_X, startScaleFinal))
                    .with(ObjectAnimator
                            .ofFloat(expandedImageView,
                                    View.SCALE_Y, startScaleFinal));
            set.setDuration(mShortAnimationDuration);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Only re-enable thumb view if the player is not dead
                    if (mCurrentAnimPlayer.lives != 0) {
                        mCurrentAnimThumbView.setAlpha(1f);
                    }
                    expandedImageView.setVisibility(View.GONE);
                    mCurrentAnimator = null;
                    handler.post(mDoThisAfterAnimation);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCurrentAnimThumbView.setAlpha(1f);
                    expandedImageView.setVisibility(View.GONE);
                    mCurrentAnimator = null;
                }
            });
            set.start();
            mCurrentAnimator = set;
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
            String msg_without_player = getResources().getString(R.string.do_attack);
            String msg_with_player = String.format(msg_without_player, victim.name);
            questionText.setText(msg_with_player);
            View victim_image = victim_container.getChildAt(0);
            zoomImageFromThumb(victim_image, victim, doAttackOtherPlayer);
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
                String msg_without_victim = getResources().getString(R.string.attack_killed);
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
            // Remove the victim's entire layout, including lives and icon containers
            otherPlayersLayout.removeView(victim_container);
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
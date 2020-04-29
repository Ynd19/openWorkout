/*
 * Copyright (C) 2020 by olie.xdev@googlemail.com All Rights Reserved
 */

package com.health.openworkout.gui.workout;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.health.openworkout.R;
import com.health.openworkout.core.OpenWorkout;
import com.health.openworkout.core.datatypes.WorkoutItem;
import com.health.openworkout.core.datatypes.WorkoutSession;
import com.health.openworkout.gui.utils.SoundUtils;

public class WorkoutSlideFragment extends Fragment {
    private enum WORKOUT_STATE {INIT, PREPARE, START, BREAK, FINISH};
    private ConstraintLayout constraintLayout;
    private TextView nameView;
    private CardView videoCardView;
    private VideoView videoView;
    private ImageView infoView;
    private TextView descriptionView;
    private TextView stateInfoView;
    private ScrollView scrollView;
    private TableLayout workoutOverviewView;
    private TextView countdownView;
    private ProgressBar progressView;
    private FloatingActionButton nextWorkoutStepView;
    private AdView adView;

    private CountDownTimer countDownTimer;
    private int remainingSec;
    private SoundUtils soundUtils;

    private WorkoutSession workoutSession;
    private WorkoutItem nextWorkoutItem;
    private WORKOUT_STATE workoutState;
    private long workoutItemIdFromFragment;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_workoutslide, container, false);

        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        adView = root.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        constraintLayout = root.findViewById(R.id.constraintLayout);
        nameView = root.findViewById(R.id.nameView);
        videoCardView = root.findViewById(R.id.videoCardView);
        videoView = root.findViewById(R.id.videoView);
        infoView = root.findViewById(R.id.infoView);

        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewGroup.LayoutParams layoutParams = videoCardView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.width = 0;
            videoCardView.setLayoutParams(layoutParams);
        } else {
            ViewGroup.LayoutParams layoutParams = videoCardView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = 0;

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.videoCardView, ConstraintSet.BOTTOM, R.id.descriptionView, ConstraintSet.TOP,8);
            constraintSet.connect(R.id.descriptionView, ConstraintSet.BOTTOM, R.id.stateInfoView, ConstraintSet.TOP, 8);
            constraintSet.applyTo(constraintLayout);

            videoCardView.setLayoutParams(layoutParams);
        }

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                mp.setLooping(true);
            }
        });

        infoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (descriptionView.getVisibility() == View.GONE) {
                    descriptionView.setVisibility(View.VISIBLE);
                } else {
                    descriptionView.setVisibility(View.GONE);
                }
            }
        });

        descriptionView = root.findViewById(R.id.descriptionView);
        stateInfoView = root.findViewById(R.id.stateInfoView);
        scrollView = root.findViewById(R.id.scrollView);
        workoutOverviewView = root.findViewById(R.id.workoutOverviewView);
        countdownView = root.findViewById(R.id.countdownView);
        progressView = root.findViewById(R.id.progressView);

        nextWorkoutStepView = root.findViewById(R.id.nextWorkoutStepView);
        nextWorkoutStepView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinishWorkoutItem();
                nextWorkoutState();
            }
        });

        soundUtils = new SoundUtils();
        initWorkout();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        soundUtils.release();
    }

    private void initWorkout() {
        long workoutSessionId = WorkoutSlideFragmentArgs.fromBundle(getArguments()).getSessionWorkoutId();
        workoutItemIdFromFragment = WorkoutSlideFragmentArgs.fromBundle(getArguments()).getWorkoutItemId();
        workoutSession = OpenWorkout.getInstance().getWorkoutSession(workoutSessionId);

        workoutState = WORKOUT_STATE.INIT;
        nextWorkoutState();
    }

    private void nextWorkoutState() {
        switch (workoutState) {
            case INIT:
                nextWorkout();
                prepareWorkout();
                break;
            case PREPARE:
                startWorkout();
                break;
            case START:
                onFinishWorkoutItem();
                nextWorkout();
                breakWorkout();
                break;
            case BREAK:
                prepareWorkout();
                break;
            case FINISH:
                break;
        }
    }

    private void nextWorkout() {
        // if no workout item was selected use the next not finished workout item in the session list
        if (workoutItemIdFromFragment == -1L) {
            if (workoutSession.getNextWorkoutItem() == null) {
                onFinishSession();
                return;
            }

            nextWorkoutItem = workoutSession.getNextWorkoutItem();
        } else {
            // otherwise use the workout item as a starting point which was selected in the workout fragment
            for (WorkoutItem workoutItem : workoutSession.getWorkoutItems()) {
                if (workoutItem.getWorkoutItemId() == workoutItemIdFromFragment) {
                    nextWorkoutItem = workoutItem;
                    workoutItemIdFromFragment = -1L;
                    break;
                }
            }
        }

        int workoutItemPos = workoutSession.getWorkoutItems().indexOf(nextWorkoutItem) + 1;
        nameView.setText(nextWorkoutItem.getName() + " (" + workoutItemPos + "/" + workoutSession.getWorkoutItems().size() + ")");

        if (nextWorkoutItem.isVideoPathExternal()) {
            videoView.setVideoURI(Uri.parse(nextWorkoutItem.getVideoPath()));
        } else {
            if (OpenWorkout.getInstance().getCurrentUser().isMale()) {
                videoView.setVideoPath("content://com.health.openworkout.videoprovider/video/male/" + nextWorkoutItem.getVideoPath());
            } else {
                videoView.setVideoPath("content://com.health.openworkout.videoprovider/video/female/" + nextWorkoutItem.getVideoPath());
            }
        }

        videoCardView.animate().alpha(1.0f);
        videoView.seekTo(100);

        descriptionView.setText(nextWorkoutItem.getDescription());
    }

    private void prepareWorkout() {
        workoutState = WORKOUT_STATE.PREPARE;
        hideWorkoutOverview();

        stateInfoView.setText(R.string.label_prepare);
        stateInfoView.setTextColor(getContext().getResources().getColor(R.color.colorRed));
        countdownView.setTextColor(getContext().getResources().getColor(R.color.colorRed));
        progressView.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorRed)));
        nextWorkoutStepView.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorRed)));

        activateCountdownTimer(nextWorkoutItem.getPrepTime());
    }

    private void startWorkout() {
        workoutState = WORKOUT_STATE.START;

        stateInfoView.setText(R.string.label_workout);
        stateInfoView.setTextColor(getContext().getResources().getColor(R.color.colorLightBlue));
        countdownView.setTextColor(getContext().getResources().getColor(R.color.colorLightBlue));
        progressView.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorLightBlue)));
        nextWorkoutStepView.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorLightBlue)));

        videoView.start();

        if (nextWorkoutItem.isTimeMode()) {
            activateCountdownTimer(nextWorkoutItem.getWorkoutTime());
        } else {
            countdownView.setText(String.format(getString(R.string.label_repetition_info), nextWorkoutItem.getRepetitionCount(), nextWorkoutItem.getName()));
            progressView.setVisibility(View.INVISIBLE);
        }
    }

    private void breakWorkout() {
        workoutState = WORKOUT_STATE.BREAK;
        showWorkoutOverview();

        stateInfoView.setText(R.string.label_break);
        stateInfoView.setTextColor(getContext().getResources().getColor(R.color.colorGreen));
        countdownView.setTextColor(getContext().getResources().getColor(R.color.colorGreen));
        progressView.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorGreen)));
        nextWorkoutStepView.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorGreen)));

        activateCountdownTimer(nextWorkoutItem.getBreakTime());
    }

    private void onFinishWorkoutItem() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        nextWorkoutItem.setFinished(true);
        OpenWorkout.getInstance().updateWorkoutItem(nextWorkoutItem);
    }

    private void onFinishSession() {
        workoutSession.setFinished(true);
        OpenWorkout.getInstance().updateWorkoutSession(workoutSession);
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigateUp();
    }


    private void showWorkoutOverview() {
        descriptionView.setVisibility(View.GONE);
        workoutOverviewView.setVisibility(View.VISIBLE);

        workoutOverviewView.removeAllViews();

        for (WorkoutItem workoutItem : workoutSession.getWorkoutItems()) {
            final OverviewWorkoutItemEntry overviewWorkoutItemEntry = new OverviewWorkoutItemEntry(getContext(), workoutItem);
            workoutOverviewView.addView(overviewWorkoutItemEntry);

            if (workoutItem.getWorkoutItemId() == nextWorkoutItem.getWorkoutItemId()) {
                overviewWorkoutItemEntry.setHighlight();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.smoothScrollTo(0, overviewWorkoutItemEntry.getTop()-50);
                    }
                });
            }
        }
    }

    private void hideWorkoutOverview() {
        descriptionView.setVisibility(View.GONE);
        workoutOverviewView.setVisibility(View.GONE);
    }

    private class OverviewWorkoutItemEntry extends TableRow {
        private ImageView status;
        private TextView reps;
        private TextView name;

        public OverviewWorkoutItemEntry(Context context, WorkoutItem workoutItem) {
            super(context);

            status = new ImageView(context);
            reps = new TextView(context);
            name = new TextView(context);

            name.setText(workoutItem.getName());

            if (workoutItem.isTimeMode()) {
                reps.setText(workoutItem.getWorkoutTime() + context.getString(R.string.seconds_unit));
            } else {
                reps.setText(Integer.toString(workoutItem.getRepetitionCount()) + "x");
            }
            status.setPadding(0, 0, 20, 0);
            reps.setPadding(0, 0, 20, 0);

            if (workoutItem.isFinished()) {
                status.setImageResource(R.drawable.ic_workout_done);
            }

            addView(status);
            addView(reps);
            addView(name);

            setPadding(10, 10, 10, 10);
        }

        public void setHighlight() {
            name.setTypeface(null, Typeface.BOLD);
            status.setImageResource(R.drawable.ic_workout_select);
        }
    }

    private void activateCountdownTimer(int sec) {
        remainingSec = sec;
        progressView.setMax(remainingSec);
        progressView.setProgress(remainingSec);
        progressView.setVisibility(View.VISIBLE);
        countdownView.setText(remainingSec + getString(R.string.seconds_unit));

        final int halftimeSec = remainingSec / 2;

        countDownTimer = new CountDownTimer(remainingSec * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                remainingSec = (int)(millisUntilFinished / 1000);
                countdownView.setText(remainingSec + getString(R.string.seconds_unit));
                progressView.setProgress(remainingSec);

                switch (workoutState) {
                    case PREPARE:
                        if ((remainingSec == 3) || (remainingSec == 2) || (remainingSec == 1)) {
                            soundUtils.playSound(SoundUtils.SOUND.WORKOUT_COUNT_BEFORE_START);
                        }
                        break;
                    case START:
                        if (remainingSec == halftimeSec) {
                            soundUtils.textToSpeech(getContext().getString(R.string.speak_halftime));
                        } else if ((remainingSec == 3) || (remainingSec == 2) || (remainingSec == 1)) {
                            soundUtils.playSound(SoundUtils.SOUND.WORKOUT_COUNT_BEFORE_START);
                        }
                        break;
                }
            }

            public void onFinish() {
                switch (workoutState) {
                    case PREPARE:
                        soundUtils.playSound(SoundUtils.SOUND.WORKOUT_START);
                        break;
                    case START:
                        soundUtils.playSound(SoundUtils.SOUND.WORKOUT_STOP);
                        break;
                }

                nextWorkoutState();
            }
        };

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countDownTimer.start();
            }
        });
    }
}

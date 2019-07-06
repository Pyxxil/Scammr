package com.example.scamcam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private TextView riskLevelText;
    private TextView transcribedText;
    private Button callButton;
    private ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        riskLevelText = findViewById(R.id.riskLevelText);
        transcribedText = findViewById(R.id.transcribedText);
        progressBar = findViewById(R.id.progressBar);

        Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
        progressDrawable.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.setProgressDrawable(progressDrawable);

        callButton = findViewById(R.id.callButton);

        try {
            // a unique number within the application to allow
            // correlating permission request responses with the request.
            int permissionRequestId = 5;

            // Request permissions needed for speech recognition
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.INTERNET,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.READ_PHONE_STATE,
                    }, permissionRequestId);
        } catch (Exception ex) {
//            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
            transcribedText.setText("Could not initialize: " + ex.toString());
        }

        // create config
        final SpeechConfig speechConfig;
        try {
            // Replace below with your own subscription key
            String speechSubscriptionKey = BuildConfig.CognitiveServicesAPIKey;
            // Replace below with your own service region (e.g., "westus").
            String serviceRegion = "westus";
            speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }

        callButton.setOnClickListener(new View.OnClickListener() {
            //            private static final String logTag = "reco 3";
            private boolean continuousListeningStarted = false;
            private AudioConfig audioInput = null;
            private SpeechRecognizer recogniser = null;
            private RiskDetector riskDetector = null;
            private THRESHOLD current_threshold = null;

            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                disableButtons();
                if (continuousListeningStarted) {
                    if (recogniser != null) {
                        final Future<Void> task = recogniser.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
//                            Log.i(logTag, "Recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(getResources().getString(R.string.begin_call));
                                riskLevelText.setText(getResources().getString(R.string.not_currently_in_call));
                                clearTextBox();
                                progressBar.setProgress(0);
                                Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
                                progressDrawable.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
                                progressBar.setProgressDrawable(progressDrawable);
                            });
                            enableButtons();
                        });
                    }

                    continuousListeningStarted = false;

                    return;
                }

                clearTextBox();

                try {
                    audioInput = AudioConfig.fromDefaultMicrophoneInput();
//                    audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    recogniser = new SpeechRecognizer(speechConfig, audioInput);

                    riskDetector = new RiskDetector();
                    progressBar.setProgress(0);
                    riskLevelText.setText("Waiting for person to speak...");
                    current_threshold = THRESHOLD.LOW;

                    recogniser.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();

                        if (s.isEmpty()) {
                            return;
                        }

//                        Log.i(logTag, "Final result received: " + s);

                        if (continuousListeningStarted) {
                            riskDetector.parseText(s);
//                            Log.i("Risk Value", String.format("%d", riskDetector.getRiskValue()));
                            riskLevelText.setText(riskDetector.getRisk() + " (" + riskDetector.getRiskValue() + ")");
                            setTranscribedText(s);
                            progressBar.setProgress(riskDetector.getRiskValue());

                            // DONT SHAKE WHEN:
                            // risk == low
                            // curr == medium && risk == medium
                            // curr == high && risk == high
                            if (riskDetector.getRiskValue() < riskDetector.getMediumThreshold() ||
                                    (current_threshold == THRESHOLD.HIGH && riskDetector.getRiskValue() >= riskDetector.getHighThreshold()) ||
                                    (current_threshold == THRESHOLD.MEDIUM && riskDetector.getRiskValue() < riskDetector.getHighThreshold())
                            ) {
//                                Log.i("THRESHOLD", "riskDetector value < medium == " + (riskDetector.getRiskValue() < riskDetector.getMediumThreshold())
//                                        + ", Current threshold == " + current_threshold);
                                return;
                            }

                            current_threshold = riskDetector.getRiskValue() >= riskDetector.getHighThreshold() ? THRESHOLD.HIGH : THRESHOLD.MEDIUM;

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Objects.requireNonNull(vibrator).vibrate(VibrationEffect.createOneShot(current_threshold == THRESHOLD.HIGH ? 2000 : 1000, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //deprecated in API 26
                                Objects.requireNonNull(vibrator).vibrate(current_threshold == THRESHOLD.HIGH ? 2000 : 1000);
                            }

                            Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
                            progressDrawable.setColorFilter(current_threshold == THRESHOLD.HIGH ? Color.RED : Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN);
                            progressBar.setProgressDrawable(progressDrawable);
                        }
                    });

                    final Future<Void> task = recogniser.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            clickedButton.setText(getResources().getString(R.string.end_call));
                            clickedButton.setEnabled(true);
                        });
                    });

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_home:
                    break;
                case R.id.action_report:
                    reportPage();
                    break;
                case R.id.action_settings:
                    settingsPage();
                    break;
            }
            return true;
        });
    }

    void settingsPage() {
        Intent settings = new Intent(this, Settings.class);
        startActivity(settings);
    }

    void reportPage() {
        Intent report = new Intent(this, Report.class);
        startActivity(report);
    }

    private void displayException(Exception ex) {
        transcribedText.setText(String.format("%s%s%s", ex.getMessage(), System.lineSeparator(), TextUtils.join(System.lineSeparator(), ex.getStackTrace())));
    }

    private void clearTextBox() {
        AppendTextLine("");
    }

    private void setTranscribedText(final String s) {
        AppendTextLine(s);
    }

    private void AppendTextLine(final String s) {
        MainActivity.this.runOnUiThread(() -> transcribedText.setText(s));
    }

    private void disableButtons() {
        MainActivity.this.runOnUiThread(() -> callButton.setEnabled(false));
    }

    private void enableButtons() {
        MainActivity.this.runOnUiThread(() -> callButton.setEnabled(true));
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    public enum THRESHOLD {
        LOW,
        MEDIUM,
        HIGH
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }
}

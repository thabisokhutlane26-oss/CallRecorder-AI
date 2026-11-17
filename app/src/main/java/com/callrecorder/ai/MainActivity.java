
package com.callrecorder.ai;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 101;

    private LinearLayout root;
    private LinearLayout recordingsList;
    private TextView statusText;
    private TextView timerText;
    private Button recordButton;

    private MediaRecorder recorder;
    private File recordingsFolder;
    private File currentFile;
    private boolean isRecording = false;

    private final int BG = 0xFF071411;
    private final int CARD = 0xFF10251D;
    private final int GREEN = 0xFF00E88F;
    private final int WHITE = 0xFFFFFFFF;
    private final int MUTED = 0xFF9AB5A7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recordingsFolder = new File(
                getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "CallRecorderAI"
        );

        if (!recordingsFolder.exists()) {
            recordingsFolder.mkdirs();
        }

        buildInterface();
        loadRecordings();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(25), dp(20), dp(25));
        root.setBackgroundColor(BG);

        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("CALLRECORDER AI", 25, GREEN, true);
        root.addView(brand);

        TextView subtitle = text(
                "PERSONAL AUDIO RECORDER",
                11, MUTED, true
        );
        subtitle.setPadding(0, dp(5), 0, dp(25));
        root.addView(subtitle);

        LinearLayout statusCard = card();
        statusCard.setOrientation(LinearLayout.VERTICAL);

        statusText = text("● READY TO RECORD", 14, GREEN, true);
        statusCard.addView(statusText);

        timerText = text("Your recordings stay on this device", 13, MUTED, false);
        timerText.setPadding(0, dp(12), 0, 0);
        statusCard.addView(timerText);

        root.addView(statusCard);

        TextView recordTitle = text("Recording studio", 21, WHITE, true);
        recordTitle.setPadding(0, dp(28), 0, dp(15));
        root.addView(recordTitle);

        recordButton = new Button(this);
        recordButton.setText("●  START RECORDING");
        recordButton.setTextColor(BG);
        recordButton.setTextSize(16);
        recordButton.setAllCaps(false);
        recordButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(GREEN)
        );

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        root.addView(recordButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        TextView notice = text(
                "Microphone recording only. This version does not directly capture cellular call audio. Always follow local recording and consent laws.",
                12, MUTED, false
        );
        notice.setPadding(dp(3), dp(14), dp(3), dp(10));
        root.addView(notice);

        TextView libraryTitle = text("My recordings", 21, WHITE, true);
        libraryTitle.setPadding(0, dp(25), 0, dp(15));
        root.addView(libraryTitle);

        recordingsList = new LinearLayout(this);
        recordingsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(recordingsList);
    }

    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION
            );
            return;
        }

        try {
            String timestamp = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
            ).format(new Date());

            currentFile = new File(
                    recordingsFolder,
                    "Recording_" + timestamp + ".m4a"
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(this);
            } else {
                recorder = new MediaRecorder();
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(currentFile.getAbsolutePath());

            recorder.prepare();
            recorder.start();

            isRecording = true;
            recordButton.setText("■  STOP RECORDING");
            recordButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF5C67)
            );
            statusText.setText("● RECORDING IN PROGRESS");
            statusText.setTextColor(0xFFFF5C67);
            timerText.setText("Recording audio from microphone...");

        } catch (Exception e) {
            releaseRecorder();

            if (currentFile != null && currentFile.exists()) {
                currentFile.delete();
            }

            Toast.makeText(
                    this,
                    "Could not start recording: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopRecording() {
        if (!isRecording || recorder == null) {
            return;
        }

        try {
            recorder.stop();

            Toast.makeText(
                    this,
                    "Recording saved successfully",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (RuntimeException e) {
            if (currentFile != null && currentFile.exists()) {
                currentFile.delete();
            }

            Toast.makeText(
                    this,
                    "Recording was too short. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            releaseRecorder();
            isRecording = false;
            currentFile = null;

            recordButton.setText("●  START RECORDING");
            recordButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(GREEN)
            );

            statusText.setText("● READY TO RECORD");
            statusText.setTextColor(GREEN);
            timerText.setText("Your recordings stay on this device");

            loadRecordings();
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (Exception ignored) {
            }

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }
    }

    private void loadRecordings() {
        if (recordingsList == null) {
            return;
        }

        recordingsList.removeAllViews();

        File[] files = recordingsFolder.listFiles();

        if (files == null || files.length == 0) {
            TextView empty = text(
                    "No recordings yet. Start your first recording above.",
                    13, MUTED, false
            );
            empty.setPadding(dp(15), dp(20), dp(15), dp(20));
            recordingsList.addView(empty);
            return;
        }

        for (int i = files.length - 1; i >= 0; i--) {
            File file = files[i];

            if (!file.isFile() || !file.getName().endsWith(".m4a")) {
                continue;
            }

            LinearLayout item = card();
            item.setOrientation(LinearLayout.VERTICAL);

            TextView name = text(file.getName(), 14, WHITE, true);
            item.addView(name);

            String size = String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    file.length() / (1024.0 * 1024.0)
            );

            TextView details = text(
                    size + "  •  Saved on this device",
                    12, MUTED, false
            );
            details.setPadding(0, dp(7), 0, 0);
            item.addView(details);

            recordingsList.addView(item);
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(17), dp(17), dp(17), dp(17));
        box.setBackgroundColor(CARD);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(12);
        box.setLayoutParams(params);

        return box;
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == AUDIO_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else if (requestCode == AUDIO_PERMISSION) {
            Toast.makeText(
                    this,
                    "Microphone permission is required to record audio.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }

            releaseRecorder();
            isRecording = false;
        }

        super.onDestroy();
    }
}

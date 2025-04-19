package com.example.sonarappv2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.media.AudioFormat;
import DSP.DSP;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Sonar extends Thread {

    public int thresholdPeak = 0;
    private Context context;
    private final int bufferSize = 32768;
    private final int sampleRate = 44100;
    private final int durationMs = 500; // Chirp duration in milliseconds
    private final int startFreq = 500;  // Start frequency in Hz
    private final int endFreq = 2000;   // End frequency in Hz
    public int deadZoneLength = 60;
    public double maxDistanceMeters = 5;
    public int threshold = 10000;
    public static Result result;
    public static double f0 = 4000;
    public static double f1 = 8000;
    public static double t1 = 0.01;
    public static double phase = 0;
    public static int delay = 0;
    short[] pulse;


    public Sonar(int thresholdPeak, Context context) {
        this.thresholdPeak = thresholdPeak;
        this.context = context;

        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        // start();
    }

    @Override
    public void run() {
        Log.i("Audio", "Running Audio Thread");
        //AudioRecord recorder = null;
        Log.i("Test", "Reached this statement");
        //short[] buffer = new short[bufferSize];
        //int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
        int numSamples = (int) Math.round(t1 * sampleRate);
        // short[] pulse = new short[numSamples];
        pulse = DSP.ConvertToShort(DSP.padSignal(DSP.HanningWindow(
                        DSP.linearChirp(phase, f0, f1, t1, sampleRate), 0, numSamples),
                bufferSize, delay));
        short[] recordedBuffer = new short[bufferSize];

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Audio", "Microphone permission not granted!");
            return; // 🚨 Exit to prevent crash
        }


//        for (int i = 0; i < numSamples; i++) {
//            double t = (double) i / sampleRate;
//            double freq = startFreq + (endFreq - startFreq) * (t / (durationMs / 1000.0));
//            pulse[i] = (short) (Short.MAX_VALUE * Math.sin(2 * Math.PI * freq * t));
//        }


        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                pulse.length * 2, AudioTrack.MODE_STATIC
        );

        track.write(pulse, 0, pulse.length);

        AudioRecord recorder = new AudioRecord(AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2);

        try {
            recorder.startRecording();
            track.play();
            Thread.sleep(durationMs);
            recorder.read(recordedBuffer, 0, recordedBuffer.length);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            track.stop();
            track.release();
            recorder.stop();
            recorder.release();
        }

        result = FilterAndClean.Distance(recordedBuffer, pulse, sampleRate, threshold,
                maxDistanceMeters, deadZoneLength, thresholdPeak, 15);
        Log.d("Sonar.java", "result is " + (result != null) + "result.distance: " + result.distance);
//        recorder = new AudioRecord(AudioSource.MIC, sampleRate,
//                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                bufferSize * 2);
//
//        try {
//            recorder.startRecording();
//        }
    }

    // 🔹 Getter method to access the pulse
    public short[] getPulse() {
        return pulse;
    }

    public void setChirpDuration(double durationSeconds) {
        t1 = durationSeconds;
    }
}

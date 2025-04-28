package com.example.sonarappv2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {

    public double[] sumArray;
    public double[] xcorrSum;
    public boolean recording = false;

    private static final int REQUEST_MIC_PERMISSION = 1;
    private GraphView graphView;
    private LineGraphSeries<DataPoint> series; // Graph series for plotting data
    private GraphView graphViewSignal;
    private LineGraphSeries<DataPoint> seriesSignal;
    private LineGraphSeries<DataPoint> thresholdSeries;
    private GraphView graphViewPulse;
    private LineGraphSeries<DataPoint> seriesPulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔹 Check for Microphone Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MIC_PERMISSION);
        } else {
            initializeSonar();  // If permission is granted, initialize the sonar
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSonar();  // Start Sonar if permission is granted
            } else {
                Toast.makeText(this, "Microphone permission is required!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeSonar() {
        Button measureButton = findViewById(R.id.measureButton);
        final TextView seekBarValue = findViewById(R.id.ProgressValue);
        final SeekBar seekBar = findViewById(R.id.seekBar1);
        final TextView readingView = findViewById(R.id.readingsView);
        //final TextView feetView = findViewById(R.id.feetView);
        final EditText durationInput = findViewById(R.id.durationInput);

        // 🔹 Initialize GraphView
        graphView = findViewById(R.id.graph);  // GraphView must be added to XML
        series = new LineGraphSeries<>();
        graphView.addSeries(series);
        thresholdSeries = new LineGraphSeries<>();
        graphView.addSeries(thresholdSeries);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(2000);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScrollable(true);

        // 🔹 Initialize the second graph for `result.signal`
        graphViewSignal = findViewById(R.id.graph_signal);  // Add this in XML
        seriesSignal = new LineGraphSeries<>();
        graphViewSignal.addSeries(seriesSignal);

        graphViewSignal.getViewport().setXAxisBoundsManual(true);
        graphViewSignal.getViewport().setMinX(0);
        graphViewSignal.getViewport().setMaxX(2000);
        graphViewSignal.getViewport().setScalable(true);
        graphViewSignal.getViewport().setScrollable(true);

        // 🔹 Initialize Graph for Pulse
        graphViewPulse = findViewById(R.id.graph_pulse);
        seriesPulse = new LineGraphSeries<>();
        graphViewPulse.addSeries(seriesPulse);

        graphViewPulse.getViewport().setXAxisBoundsManual(true);
        graphViewPulse.getViewport().setMinX(0);
        graphViewPulse.getViewport().setMaxX(2000);
        graphViewPulse.getViewport().setScalable(true);
        graphViewPulse.getViewport().setScrollable(true);
        graphViewPulse.getViewport().setYAxisBoundsManual(true);
        graphViewPulse.getViewport().setMinY(-500);  // Adjust as needed
        graphViewPulse.getViewport().setMaxY(500);   // Adjust as needed

        seekBar.setProgress(29);
        seekBarValue.setText(String.valueOf(seekBar.getProgress()));

        final Sonar sonsys = new Sonar(seekBar.getProgress(), getApplicationContext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue.setText(String.valueOf(progress));

                // Update the threshold line
                updateThresholdLine(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        measureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sonsys.thresholdPeak = seekBar.getProgress();
                String durationText = durationInput.getText().toString().trim();
                if (!durationText.isEmpty()) {
                    try {
                        double durationMs = Double.parseDouble(durationText);
                        double durationSec = durationMs / 1000.0;
                        sonsys.setChirpDuration(durationSec);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getApplicationContext(), "Invalid chirp duration!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                sonsys.run();
                final DecimalFormat df = new DecimalFormat("#.####");
                String distanceMeters;
                String distanceFeet;
                if (Sonar.result != null) {
                    distanceMeters = df.format(sonsys.result.distance);
                    //distanceFeet = df.format(sonsys.result.distance*3.28084);
                    readingView.setText(distanceMeters);
                    //feetView.setText(distanceFeet);

                    series.resetData(generateSeries(sonsys.result.xcorr));
                    seriesSignal.resetData(generateSeries(sonsys.result.signal));
                    // 🔹 Fetch pulse data and plot it
                    short[] pulseData = sonsys.getPulse();
                    if (pulseData != null) {
                        seriesPulse.resetData(generateSeries(pulseData));
                    } else {
                        Log.e("MainActivity", "Pulse data is null!");
                    }
                } else {
                    Log.e("MainActivity", "Sonar.result is null!");
                }


            }
        });
    }

    // 🔹 Updated generateSeries method
    public DataPoint[] generateSeries(double[] signal) {
        int length = signal.length;
        DataPoint[] data = new DataPoint[length];
        for (int i = 0; i < length; i++) {
            data[i] = new DataPoint(i, signal[i]);
        }
        return data;
    }

    // 🔹 Overloaded method for short[] to convert it to double[]
    public DataPoint[] generateSeries(short[] signal) {
        int length = signal.length;
        DataPoint[] data = new DataPoint[length];
        for (int i = 0; i < length; i++) {
            data[i] = new DataPoint(i, (double) signal[i]);  // Convert short to double
        }
        return data;
    }

    private void updateThresholdLine(double threshold) {
        double multiple = 1000000000.0;
        double multipliedThreshold = threshold * multiple;
        DataPoint[] thresholdData = new DataPoint[]{
                new DataPoint(0, multipliedThreshold),      // Start of the graph
                new DataPoint(2000, multipliedThreshold)    // End of the graph (same max X as the viewport)
        };

        thresholdSeries.resetData(thresholdData);
    }
}

package com.example.rawconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarItemView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class EditActivity extends AppCompatActivity {

    int toneSelection, curveSelection;

    LibRaw libraw;
    ImageView imageView;
    Group whiteBalancingGroup, toneMappingGroup, extraGroup;
    Button processButton;
    ProgressBar progressCircle;
    RadioGroup whiteBalancingRadioGroup, toneRadioGroup, curveRadioGroup;
    SeekBar brightnessSeek, gammaSeek;
    ToneCurveView toneCurveView;
    NavigationBarView bottomNavigation;
    Button resetButton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        libraw = LibRaw.newInstance();

        // Set default radio selections
        toneSelection = 0;
        curveSelection = 0;

        // Get view elements
        imageView = findViewById(R.id.imageView);
        whiteBalancingGroup = findViewById(R.id.white_balancing_group);
        toneMappingGroup = findViewById(R.id.tone_mapping_group);
        extraGroup = findViewById(R.id.extra_group);
        progressCircle = findViewById(R.id.img_loading_circle);
        processButton = findViewById(R.id.process_btn);
        whiteBalancingRadioGroup = findViewById(R.id.radio_group_white_balancing);
        toneRadioGroup = findViewById(R.id.radio_group_tone);
        curveRadioGroup = findViewById(R.id.radio_group_curve);
        brightnessSeek = findViewById(R.id.brightness_seekbar);
        gammaSeek = findViewById(R.id.gamma_seekbar);
        toneCurveView = findViewById(R.id.tone_curve);
        toneCurveView.setVisibility(View.INVISIBLE);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        resetButton = findViewById(R.id.reset_btn);

        // Hide options
        whiteBalancingGroup.setVisibility(View.VISIBLE);
        toneMappingGroup.setVisibility(View.GONE);
        extraGroup.setVisibility(View.GONE);

        // Get bitmap
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            progressCircle.setVisibility(View.VISIBLE);
            new Thread() {
                @Override
                public void run() {
                    Uri imagePath = (Uri) extras.get("imagePath");

                    InputStream iStream = null;
                    byte[] inputData = null;

                    try {
                        iStream = getContentResolver().openInputStream(imagePath);
                        inputData = getBytes(iStream);
                        iStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (inputData != null) {
                        int status = libraw.openBuffer(inputData, inputData.length);
                        final Bitmap bitmap = libraw.decodeAsBitmap(true, false);
                        if (bitmap != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressCircle.setVisibility(View.GONE);
                                    imageView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    }
                }
            }.start();
        }

        // Wire process button
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processRaw(false);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toneCurveView.resetColorCurves(toneSelection);
                libraw.applyToneCurve(toneCurveView.maxBoundary_x, toneCurveView.maxBoundary_y,
                        toneCurveView.firstCPArr[toneSelection], toneCurveView.secondCPArr[toneSelection],
                        toneCurveView.knotsList.get(toneSelection), toneSelection);
                processRaw(true);

            }
        });

        // Wire radio groups
        whiteBalancingRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                // TODO: Check user mul values
                if (findViewById(i) == findViewById(R.id.radio_no_white_balance)) {
                    libraw.setUserMul(new float[]{1f, 1f, 1f, 1f});
                    libraw.setCameraWb(false);
                    libraw.setAutoWb(false);
                } else if (findViewById(i) == findViewById(R.id.radio_camera_white_balance)) {
                    libraw.setUserMul(new float[]{0f, 0f, 0f, 0f});
                    libraw.setCameraWb(true);
                    libraw.setAutoWb(false);
                } else if (findViewById(i) == findViewById(R.id.radio_auto_white_balance)) {
                    libraw.setUserMul(new float[]{0f, 0f, 0f, 0f});
                    libraw.setCameraWb(false);
                    libraw.setAutoWb(true);
                }
            }
        });
        toneRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (findViewById(i) == findViewById(R.id.radio_tone_red)) {
                    toneSelection = 0;
                } else if (findViewById(i) == findViewById(R.id.radio_tone_green)) {
                    toneSelection = 1;
                } else if (findViewById(i) == findViewById(R.id.radio_tone_blue)) {
                    toneSelection = 2;
                }
                toneCurveView.changeColor(toneSelection);
            }
        });
        curveRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                // TODO: Set curve selection
                curveSelection = i;
            }
        });


        // Wire bottom menu
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_white_balancing) {
                    whiteBalancingGroup.setVisibility(View.VISIBLE);
                    toneMappingGroup.setVisibility(View.GONE);
                    extraGroup.setVisibility(View.GONE);
                    toneCurveView.setVisibility(View.INVISIBLE);
                    return true;
                } else if (item.getItemId() == R.id.menu_tone_mapping) {
                    whiteBalancingGroup.setVisibility(View.GONE);
                    toneMappingGroup.setVisibility(View.VISIBLE);
                    extraGroup.setVisibility(View.GONE);
                    toneCurveView.setVisibility(View.VISIBLE);
                    return true;
                } else if (item.getItemId() == R.id.menu_extra) {
                    whiteBalancingGroup.setVisibility(View.GONE);
                    toneMappingGroup.setVisibility(View.GONE);
                    extraGroup.setVisibility(View.VISIBLE);
                    toneCurveView.setVisibility(View.INVISIBLE);
                    return true;
                }
                return false;
            }
        });

        brightnessSeek.setProgress(20);
        brightnessSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekVal = seekBar.getProgress();
                float bright = 5f / 100.f * seekVal;
                Toast.makeText(getApplicationContext(), String.valueOf(bright), Toast.LENGTH_SHORT).show();
                libraw.setBright(bright);
            }
        });

        gammaSeek.setProgress(36);
        gammaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                double seekVal = seekBar.getProgress();
                double gamma = 6f / 100.f * seekVal + 0.3f;
                Toast.makeText(getApplicationContext(), String.valueOf(gamma), Toast.LENGTH_SHORT).show();
                libraw.setGammPower(1 / gamma);
            }
        });

        toneCurveView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    // TODO: Check indices
                    libraw.applyToneCurve(toneCurveView.maxBoundary_x, toneCurveView.maxBoundary_y,
                            toneCurveView.firstCPArr[toneSelection], toneCurveView.secondCPArr[toneSelection],
                            toneCurveView.knotsList.get(toneSelection), toneSelection);
                    processRaw(true);
                }
                return false;
            }
        });

    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        // https://stackoverflow.com/questions/10296734/image-uri-to-bytesarray
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        byte[] bytesResult = null;

        try {
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        } finally {
            try {
                byteBuffer.close();
            } catch (IOException ignored) { /* do nothing */ }
        }
        return bytesResult;
    }

    public void processRaw(boolean onlyMem) {
        progressCircle.setVisibility(View.VISIBLE);
        new Thread() {
            @Override
            public void run() {
                final Bitmap bitmap = libraw.decodeAsBitmap(true, onlyMem);
                if (bitmap != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressCircle.setVisibility(View.GONE);
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            }
        }.start();
    }
}
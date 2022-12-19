package com.example.rawconverter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditActivity extends AppCompatActivity {

    LibRaw libraw;
    ImageView imageView;
    LinearLayout whiteOptions, toneOptions;
    Switch whiteSwitch, toneSwitch;
    Button processButton;
    ProgressBar progressCircle;
    CheckBox cameraWbCheck;
    CheckBox autoWbCheck;
    CheckBox noWbCheck;
    SeekBar brightnessSeek;
    SeekBar gammaSeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        libraw = LibRaw.newInstance();

        // Get view elements
        imageView = findViewById(R.id.imageView);
        whiteOptions = findViewById(R.id.white_options);
        toneOptions = findViewById(R.id.tone_options);
        whiteSwitch = findViewById(R.id.white_switch);
        toneSwitch = findViewById(R.id.tone_switch);
        progressCircle = findViewById(R.id.img_loading_circle);
        processButton = findViewById(R.id.process_btn);
        cameraWbCheck = findViewById(R.id.checkBox_camera_white_balance);
        autoWbCheck = findViewById(R.id.checkBox_auto_white_balance);
        noWbCheck = findViewById(R.id.checkBox_no_white_balance);
        brightnessSeek = findViewById(R.id.brightness_seekbar);
        gammaSeek = findViewById(R.id.gamma_seekbar);


        // Hide options
        whiteOptions.setVisibility(View.GONE);
        toneOptions.setVisibility(View.GONE);

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
                        final Bitmap bitmap = libraw.decodeAsBitmap(true);
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

        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processRaw();
            }
        });

        // Wire toggle switches
        whiteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                whiteOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        toneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toneOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // White balance Checkboxes
        cameraWbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                libraw.setCameraWb(isChecked);
                if (isChecked) {
                    autoWbCheck.setChecked(false);
                    noWbCheck.setChecked(false);
                }
            }
        });
        autoWbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                libraw.setAutoWb(isChecked);
                if (isChecked) {
                    cameraWbCheck.setChecked(false);
                    noWbCheck.setChecked(false);
                }
            }
        });
        autoWbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    float[] arr = new float[] {1f, 1f, 1f, 1f};
                    libraw.setUserMul(arr);
                    cameraWbCheck.setChecked(false);
                    autoWbCheck.setChecked(false);
                }
                else {
                    float[] arr = new float[] {0f, 0f, 0f, 0f};
                    libraw.setUserMul(arr);
                }
            }
        });

        brightnessSeek.setProgress(20);
        brightnessSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekVal = seekBar.getProgress();
                float bright = 5f / 100.f * seekVal;
                Toast.makeText(getApplicationContext(),String.valueOf(bright), Toast.LENGTH_SHORT).show();
                libraw.setBright(bright);
            }
        });

        gammaSeek.setProgress(36);
        gammaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                double seekVal = seekBar.getProgress();
                double gamma = 6f / 100.f * seekVal + 0.3f;
                Toast.makeText(getApplicationContext(),String.valueOf(gamma), Toast.LENGTH_SHORT).show();
                libraw.setGammPower(1 / gamma);
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

    public void processRaw() {
        progressCircle.setVisibility(View.VISIBLE);
        new Thread() {
            @Override
            public void run() {
                final Bitmap bitmap = libraw.decodeAsBitmap(true);
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
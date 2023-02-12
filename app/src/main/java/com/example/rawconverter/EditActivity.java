package com.example.rawconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class EditActivity extends AppCompatActivity {

    int toneSelection, curveSelection;

    LibRaw libraw;
    ImageView imageView;
    Group whiteBalancingGroup, colorCorrectionGroup, tonemapGroup, extraGroup;
    Button processButton, resetButton, resetGainButton;
    ProgressBar progressCircle;
    RadioGroup toneRadioGroup;
    SeekBar brightnessSeek, gammaSeek;
    ToneCurveView toneCurveView;
    NavigationBarView bottomNavigation;
    SeekBar brightnessToneSeek, contrastToneSeek, seekR, seekB;
    FloatingActionButton saveButton;
    Switch toneCurveSwitch;
    Spinner tonemapSpinner, wbSpinner, wbctSpinner, wbOptionSpinner;
    LinearLayout wbSeekGroup;

    View saveView;
    ConstraintLayout saveLayout;

    int[] wbct_ind;
    float[] wbct_labels;
    int[] wb_ind;

    ArrayList<String> wbOptionsLabel = new ArrayList<String>();

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
        colorCorrectionGroup = findViewById(R.id.color_correction_group);
        tonemapGroup = findViewById(R.id.tonemap_group);
        extraGroup = findViewById(R.id.extra_group);
        progressCircle = findViewById(R.id.img_loading_circle);
        processButton = findViewById(R.id.process_btn);
        toneRadioGroup = findViewById(R.id.radio_group_tone);
        brightnessSeek = findViewById(R.id.brightness_seekbar);
        gammaSeek = findViewById(R.id.gamma_seekbar);
        toneCurveView = findViewById(R.id.tone_curve);
        toneCurveView.setVisibility(View.INVISIBLE);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        resetButton = findViewById(R.id.reset_btn);
        resetGainButton = findViewById(R.id.reset_gain_btn);
        brightnessToneSeek = findViewById(R.id.brightness_tone_seekbar);
        contrastToneSeek = findViewById(R.id.contrast_tone_seekbar);
        saveButton = findViewById(R.id.floatingActionButton);
        toneCurveSwitch = findViewById(R.id.tone_curve_switch);
        tonemapSpinner = findViewById(R.id.tonemap_spinner);
        wbSpinner = findViewById(R.id.wb_spinner);
        wbctSpinner = findViewById(R.id.wbct_spinner);
        wbOptionSpinner = findViewById(R.id.wb_option_spinner);
        saveView = findViewById(R.id.saveView);
        saveLayout = findViewById(R.id.saveLayout);
        wbSeekGroup = findViewById(R.id.wb_seek_group);

        seekR = findViewById(R.id.seek_r);
        seekB = findViewById(R.id.seek_b);

        // Populate tonemap Spinner
        ArrayAdapter<CharSequence> tonemap_adapter = ArrayAdapter.createFromResource(this, R.array.tonemap_array, android.R.layout.simple_spinner_dropdown_item);
        tonemapSpinner.setAdapter(tonemap_adapter);

        // Populate wb options spinner
        wbOptionsLabel.add("Camera Whitebalance");
        wbOptionsLabel.add("Auto Whitebalance");
        wbOptionsLabel.add("Custom Whitebalance");

        ArrayAdapter<String> wbOptionsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wbOptionsLabel);
        wbOptionSpinner.setAdapter(wbOptionsAdapter);

        // Hide options
        whiteBalancingGroup.setVisibility(View.VISIBLE);
        colorCorrectionGroup.setVisibility(View.GONE);
        tonemapGroup.setVisibility(View.GONE);
        extraGroup.setVisibility(View.GONE);
        resetButton.setVisibility(View.INVISIBLE);
        saveView.setVisibility(View.GONE);
        saveLayout.setVisibility(View.GONE);
        wbSpinner.setVisibility(View.GONE);
        wbctSpinner.setVisibility(View.GONE);
        wbSeekGroup.setVisibility(View.GONE);

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
                        if (status == 0) {
                            final Bitmap bitmap = libraw.decodeAsBitmap(true, false);
                            if (bitmap != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressCircle.setVisibility(View.GONE);
                                        imageView.setImageBitmap(bitmap);

                                        wb_ind = libraw.getWBInd();
                                        wbct_ind = libraw.getWBCTInd();
                                        wbct_labels = libraw.getWBCTTemp();
                                        fillWbSpinner();
                                    }
                                });
                            }
                        }
                    }
                }
            }.start();

        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("DO SAVING", "SAVING");
//                libraw.writeTxtFile(EditActivity.this.getFilesDir());
                saveView.setVisibility(View.VISIBLE);
                saveLayout.setVisibility(View.VISIBLE);

                new Thread() {
                    @Override
                    public void run() {
                        final Bitmap bitmap = libraw.decodeAsBitmap(false, false);
                        if (bitmap != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    saveImage(bitmap);
                                    saveView.setVisibility(View.GONE);
                                    saveLayout.setVisibility(View.GONE);
                                    Toast.makeText(getApplicationContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }.start();

            }
        });

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
                libraw.applyToneCurve(toneCurveView.maxBoundary_x, toneCurveView.maxBoundary_y, toneCurveView.firstCPArr[toneSelection], toneCurveView.secondCPArr[toneSelection], toneCurveView.knotsList.get(toneSelection), toneSelection);
                processRaw(true);

            }
        });

        resetGainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                brightnessToneSeek.setProgress(50);
                contrastToneSeek.setProgress(10);
                libraw.applyBrightness(0f, toneSelection);
                libraw.applyContrast(1.0f, toneSelection);
                processRaw(true);

            }
        });

        wbOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch(pos) {
                    case 0:
                        libraw.setUserMul(new float[]{0f, 0f, 0f, 0f});
                        libraw.setCameraWb(true);
                        libraw.setAutoWb(false);
                        wbSpinner.setVisibility(View.GONE);
                        wbctSpinner.setVisibility(View.GONE);
                        wbSeekGroup.setVisibility(View.GONE);
                        break;
                    case 1:
                        libraw.setUserMul(new float[]{0f, 0f, 0f, 0f});
                        libraw.setCameraWb(false);
                        libraw.setAutoWb(true);
                        wbSpinner.setVisibility(View.GONE);
                        wbctSpinner.setVisibility(View.GONE);
                        wbSeekGroup.setVisibility(View.GONE);
                        break;
                    case 2:
                        wbSpinner.setVisibility(View.GONE);
                        wbctSpinner.setVisibility(View.GONE);
                        libraw.setCameraWb(false);
                        libraw.setAutoWb(false);
                        wbSeekGroup.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        libraw.setCameraWb(false);
                        libraw.setAutoWb(false);
                        wbSpinner.setVisibility(View.VISIBLE);
                        wbctSpinner.setVisibility(View.GONE);
                        wbSeekGroup.setVisibility(View.GONE);
                        libraw.applyWBUserMul(wb_ind[wbSpinner.getSelectedItemPosition()]);
                        break;
                    case 4:
                        libraw.setCameraWb(false);
                        libraw.setAutoWb(false);
                        wbSpinner.setVisibility(View.GONE);
                        wbctSpinner.setVisibility(View.VISIBLE);
                        wbSeekGroup.setVisibility(View.GONE);
                        libraw.applyWBUserMul(wbct_ind[wbctSpinner.getSelectedItemPosition()]);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
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
                } else if (findViewById(i) == findViewById(R.id.radio_all)) {
                    toneSelection = 3;
                }
                toneCurveView.changeColor(toneSelection);
            }
        });

        // Wire bottom menu
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_white_balancing) {
                    whiteBalancingGroup.setVisibility(View.VISIBLE);
                    colorCorrectionGroup.setVisibility(View.GONE);
                    tonemapGroup.setVisibility(View.GONE);
                    extraGroup.setVisibility(View.GONE);
                    toneCurveView.setVisibility(View.INVISIBLE);
                    return true;
                } else if (item.getItemId() == R.id.menu_color_correction) {
                    whiteBalancingGroup.setVisibility(View.GONE);
                    colorCorrectionGroup.setVisibility(View.VISIBLE);
                    tonemapGroup.setVisibility(View.GONE);
                    extraGroup.setVisibility(View.GONE);
                    if (toneCurveSwitch.isChecked()) {
                        toneCurveView.setVisibility(View.VISIBLE);
                    }
                    else {
                        toneCurveView.setVisibility(View.INVISIBLE);
                    }
                    return true;
                } else if (item.getItemId() == R.id.menu_tonemap) {
                    whiteBalancingGroup.setVisibility(View.GONE);
                    colorCorrectionGroup.setVisibility(View.GONE);
                    tonemapGroup.setVisibility(View.VISIBLE);
                    extraGroup.setVisibility(View.GONE);
                    toneCurveView.setVisibility(View.INVISIBLE);
                    return true;
                } else if (item.getItemId() == R.id.menu_extra) {
                    whiteBalancingGroup.setVisibility(View.GONE);
                    colorCorrectionGroup.setVisibility(View.GONE);
                    tonemapGroup.setVisibility(View.GONE);
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
                    libraw.applyToneCurve(toneCurveView.maxBoundary_x, toneCurveView.maxBoundary_y, toneCurveView.firstCPArr[toneSelection], toneCurveView.secondCPArr[toneSelection], toneCurveView.knotsList.get(toneSelection), toneSelection);
                    processRaw(true);
                }
                return false;
            }
        });

        toneCurveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toneCurveView.setVisibility(View.VISIBLE);
                    resetButton.setVisibility(View.VISIBLE);
                } else {
                    toneCurveView.setVisibility(View.INVISIBLE);
                    resetButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        brightnessToneSeek.setProgress(50);
        brightnessToneSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekVal = seekBar.getProgress();
                float brightnessVal = 65535 * seekVal / 100 - (65535f / 2); // Scale to range 0-2
                libraw.applyBrightness(brightnessVal, toneSelection);
                processRaw(true);
            }
        });

        contrastToneSeek.setProgress(10);
        contrastToneSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekVal = seekBar.getProgress();
                float contrastVal = 10 * seekVal / 100; // Scale to range 0-2
                libraw.applyContrast(contrastVal, toneSelection);
                processRaw(true);
            }
        });

        seekR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekValR = seekBar.getProgress();
                float valR = 5 * seekValR/100;
                float seekValB = seekB.getProgress();
                float valB = 5 * seekValB/100;
                float[] userMul = {valR, 1.0f, valB, 1.0f};
                libraw.setUserMul(userMul);
            }
        });

        seekB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float seekValB = seekBar.getProgress();
                float valB = 5 * seekValB/100;
                float seekValR = seekR.getProgress();
                float valR = 5 * seekValR/100;
                float[] userMul = {valR, 1.0f, valB, 1.0f};
                libraw.setUserMul(userMul);
            }
        });

        tonemapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                libraw.setToneMap(pos);
                processRaw(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        wbSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                libraw.applyWBUserMul(wb_ind[pos]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        wbctSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                libraw.applyWBCTUserMul(wbct_ind[pos]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
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

    public void saveImage(Bitmap bitmap) {
        OutputStream fos;
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, ts + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Objects.requireNonNull(fos);
            }
        } catch (Exception e) {
            Log.d("error", e.toString());
        }
    }

    public void fillWbSpinner() {
        // Fill WB spinner
        ArrayList<String> wbLabels = new ArrayList<String>();
        if (wb_ind != null) {
            wbOptionsLabel.add("Presets");
            for (int i = 1; i <= wb_ind.length; i++) {
                wbLabels.add("Option " + i);
            }
        } else {
            wbctSpinner.setVisibility(View.GONE);
        }
        ArrayAdapter<String> wbSpinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wbLabels);
        wbSpinner.setAdapter(wbSpinnerArrayAdapter);

        // Fill WBCT spinner
        ArrayList<String> wbctLabels = new ArrayList<String>();
        if (wbct_ind != null) {
            wbOptionsLabel.add("Temperature");
            for (int i = 0; i < wbct_ind.length; i++) {
                wbctLabels.add(String.valueOf(wbct_labels[i]) + " K");
            }
        }
        else {
            wbSpinner.setVisibility(View.GONE);
        }
        ArrayAdapter<String> wbctSpinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wbctLabels);
        wbctSpinner.setAdapter(wbctSpinnerArrayAdapter);

        ArrayAdapter<String> wbOptionsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wbOptionsLabel);
        wbOptionSpinner.setAdapter(wbOptionsAdapter);
    }

}
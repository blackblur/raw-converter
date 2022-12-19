package com.example.rawconverter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditActivity extends AppCompatActivity {

    ImageView imageView;
    LinearLayout whiteOptions, toneOptions;
    Switch whiteSwitch, toneSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        LibRaw libraw = LibRaw.newInstance();

        // Get view elements
        imageView = findViewById(R.id.imageView);
        whiteOptions = findViewById(R.id.white_options);
        toneOptions = findViewById(R.id.tone_options);
        whiteSwitch = findViewById(R.id.white_switch);
        toneSwitch = findViewById(R.id.tone_switch);

        // Hide options
        whiteOptions.setVisibility(View.GONE);
        toneOptions.setVisibility(View.GONE);

        // Get bitmap
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
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
                Bitmap bitmap = libraw.decodeAsBitmap(true);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
            libraw.close();
        }

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
            } catch (IOException ignored){ /* do nothing */ }
        }
        return bytesResult;
    }
}
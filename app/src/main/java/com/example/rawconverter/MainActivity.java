package com.example.rawconverter;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<String> openRaw;
    ImageView thumbnail;

    static {
        System.loadLibrary("rawconverter");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Python.isStarted())
            Python.start(new AndroidPlatform(this));

        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("test");

        thumbnail = (ImageView) findViewById(R.id.thumbnail);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        openRaw = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {

                        InputStream iStream = null;
                        byte[] inputData = null;

                        try {
                            iStream = getContentResolver().openInputStream(result);
                            inputData = getBytes(iStream);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (inputData != null) {
                            byte[] res = pyobj.callAttr("open_raw", inputData).toJava(byte[].class);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(res, 0, res.length);
                            thumbnail.setImageBitmap(bitmap);
                        } else {
                            Log.i("PYTHON STATUS", "false");
                        }
                    }
                }
        );

        Log.i("C++ String", stringFromJNI());
    }

    public void loadRawFile(View v) {
        openRaw.launch("*/*");

    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        // https://stackoverflow.com/questions/10296734/image-uri-to-bytesarray
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public native String stringFromJNI();

}
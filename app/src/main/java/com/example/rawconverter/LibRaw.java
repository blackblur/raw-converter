package com.example.rawconverter;
import android.graphics.Bitmap;
import android.util.Log;

public class LibRaw implements AutoCloseable {
    static {
        System.loadLibrary("rawconverter");
    }

    boolean isInstance = false;

    public LibRaw(int flags) {
        init(flags);
        isInstance = true;
    }

    public LibRaw() {
        this(0);
    }

    public static LibRaw newInstance() {
        return new LibRaw();
    }

    public void close() {
        recycle();
        isInstance = false;
    }

    public boolean isClosed() {
        return isInstance;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!isInstance) {
            Log.w("LibRaw", "Failed to call close()");
            close();
        }
    }

    public Bitmap decodeAsBitmap(boolean halfSize) {
        Bitmap b = null;

        if (isInstance) {
            setOutputBps(8);
            setQuality(2);
            setHalfSize(halfSize);

            int[] pixels = getPixels8();
            if (pixels != null) {
                b=Bitmap.createBitmap(pixels, getBitmapWidth(), getBitmapHeight(), Bitmap.Config.ARGB_8888);
            }

            recycle();
            return b;
        }

        return null;
    }

    /**
     * Include libRaw methods from cpp
     */
    public native String stringFromJNI();  // Test Function

    public native void recycle();
    public native void init(int flags);
    public native int openFile(String file);
    public native int openBuffer(byte[] buffer, int size);
    public native byte[] getThumbnail(byte[] buffer, int size);
    public native int[] getPixels8();
    public native void setOutputBps(int outputBps);
    public native void setQuality(int quality);
    public native void setHalfSize(boolean halfSize);
    public native int getBitmapWidth();
    public native int getBitmapHeight();
}

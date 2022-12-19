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
            setUserQual(2);
            setHalfSize(halfSize);

            getInfo();

            int[] pixels = getPixels8();
            if (pixels != null) {
                b=Bitmap.createBitmap(pixels, getBitmapWidth(), getBitmapHeight(), Bitmap.Config.ARGB_8888);
            }

//            recycle();
            return b;
        }

        return null;
    }

    /**
     * Include libRaw methods from cpp
     */
    public native String stringFromJNI();  // Test Function
    public native void getInfo();

    /**
     * Methods Loading Data from a File
     */
    public native void init(int flags);
    public native int openFile(String file);
    public native int openBuffer(byte[] buffer, int size);
    public native byte[] getThumbnail(byte[] buffer, int size);

    /**
     * To Java image
     */
    public native int[] getPixels8();

    /**
     * Auxiliary Functions
     */
    public native void recycle();

    /**
     * Image Getter and Setter
     */
    public native int getBitmapWidth();
    public native int getBitmapHeight();

    /**
     * libraw_output_params_t: Management of dcraw-Style Postprocessing
     */
    public native void setGreyBox(int[] greyBox);
    public native void setCropBox(int[] cropBox);
    public native void setAber(double[] aber);
    public native void setGamm(double[] gamm);
    public native void setGammPower(double gamm);
    public native void setUserMul(float[] userMul);
    public native void setShotSelect(int shotSelect);
    public native void setBright(float bright);
    public native void setThreshold(float threshold);
    public native void setHalfSize(boolean halfSize);
    public native void setFourColorRGB(boolean fourColorRGB);
    public native void setHighlight(int highlight);
    public native void setAutoWb(boolean autoWb);
    public native void setCameraWb(boolean camWb);
    public native void setCameraMatrix(int cameraMatrix);
    public native void setOutputColor(int outputColor);
    public native void setOutputBps(int outputBps);
    public native void setOutputTiff(int outputTiff);
    public native void setUserFlip(int userFlip);
    public native void setUserQual(int quality);
    public native void setUserBlack(int black);
    public native void setUserCBlack(int[] cBlack);
    public native void setUserSat(int sat);
    public native void setMedPasses(int sat);
    public native void setNoAutoBright(int notAutoBright);
    public native void setAutoBrightThr(float autoBrightThr);
    public native void setAdjustMaximumThr(float adjustMaximumThr);
    public native void setGreenMatching(int greenMatching);
    public native void setDcbIterations(int dcbIterations);
    public native void setDcbEnhanceFL(int dcbEnhanceFl);
    public native void setFbddNoiserd(int fbddNoiserd);
    public native void setExpCorrec(int expCorrec);
    public native void setExpShift(float expShift);
    public native void setExpPreser(float expPreser);
    public native void setRawSpeed(int rawSpeed);
    public native void setDngSdk(int dngSdk);
    public native void setNoAutoScale(int noAutoScale);
    public native void setNoInterpolation(int noInterpol);
    public native void setRawProcessingOptions(int options);
    public native void setMaxRawMemoryMb(int maxRawMemoryMb);
}

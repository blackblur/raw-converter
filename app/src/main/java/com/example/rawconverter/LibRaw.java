package com.example.rawconverter;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.rawconverter.utils.Point;

import java.util.List;

import java.io.File;
import java.io.FileWriter;


public class LibRaw implements AutoCloseable {
    static {
        System.loadLibrary("rawconverter");
    }

    boolean isInstance = false;
    int[] pointsY;
    int[] pknots_x;
    int[] pknots_y;
    float[][] wbctCoeffs;
    int[][] wbCoeffs;

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

    public Bitmap decodeAsBitmap(boolean halfSize, boolean onlyMem) {
        Bitmap b = null;

        if (isInstance) {
            setOutputBps(8);
            setUserQual(2);
            setHalfSize(halfSize);
//            setOutputColor(2);

            getInfo();

            int[] pixels;

            if (onlyMem) {
                pixels = getPixels8OnlyMem();
            } else {
                pixels = getPixels8();
            }
            if (pixels != null) {
                b = Bitmap.createBitmap(pixels, getBitmapWidth(), getBitmapHeight(), Bitmap.Config.ARGB_8888);
            }

//            recycle();
            return b;
        }

        return null;
    }

    public void applyToneCurve(float maxBoundaryX, float maxBoundaryY, Point[] firstCP, Point[] secondCP, List<Point> knots, int rgb) {
        Log.i("LIBRAW", "APPLY TONECURVE");
//        int maximumColor = getMaximumColor();
        int maximumColor = 65535;

        if (knots.size() > 2) {
            int[] x = new int[firstCP.length + secondCP.length + knots.size()];
            int[] y = new int[firstCP.length + secondCP.length + knots.size()];
            float factorX = (maximumColor + 1) / maxBoundaryX;
            float factorY = 65536 / maxBoundaryY;
            for (int i = 0, j = 0; i < x.length - 3; i += 3, j++) {
                x[i] = Math.max(Math.min((int) (knots.get(j).x * factorX), maximumColor), 0);
                y[i] = Math.max(Math.min((int) (knots.get(j).y * factorY), maximumColor), 0);
                x[i + 1] = Math.max(Math.min((int) (firstCP[j].x * factorX), maximumColor), 0);
                y[i + 1] = Math.max(Math.min((int) (firstCP[j].y * factorY), maximumColor), 0);
                x[i + 2] = Math.max(Math.min((int) (secondCP[j].x * factorX), maximumColor), 0);
                y[i + 2] = Math.max(Math.min((int) (secondCP[j].y * factorY), maximumColor), 0);
            }
            x[x.length - 1] = Math.max(Math.min((int) (knots.get(knots.size() - 1).x * factorX), maximumColor), 0);
            y[y.length - 1] = Math.max(Math.min((int) (knots.get(knots.size() - 1).y * factorY), maximumColor), 0);

            applyToneCurve(x, y, rgb);
//            pointsY = getToneCurve();
//            pknots_x = x;
//            pknots_y = y;
        } else {
            int[] x = new int[2];
            int[] y = new int[2];

            float factorX = (maximumColor + 1) / maxBoundaryX;
            float factorY = (maximumColor + 1) / maxBoundaryY;

            x[0] = Math.max(Math.min((int) (knots.get(0).x * factorX), maximumColor), 0);
            y[0] = Math.max(Math.min((int) (knots.get(0).y * factorY), maximumColor), 0);
            x[1] = Math.max(Math.min((int) (knots.get(1).x * factorX), maximumColor), 0);
            y[1] = Math.max(Math.min((int) (knots.get(1).y * factorY), maximumColor), 0);

            applyToneCurve(x, y, rgb);
//            pointsY = getToneCurve();
        }
    }

    public void writeTxtFile(File dir) {
        File file = new File(dir, "text");
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            File gpxfile = new File(file, "sample.txt");
            FileWriter writer = new FileWriter(gpxfile);

            for (int i = 0; i < pointsY.length; i++) {
                writer.append(String.valueOf(pointsY[i]) + "\n");
            }
            writer.flush();
            writer.close();
            Log.i("SAVING", "DONE");

            gpxfile = new File(file, "knots.txt");
            writer = new FileWriter(gpxfile);

            for (int i = 0; i < pknots_x.length; i++) {
                writer.append(String.valueOf(pknots_x[i]) + "; " + String.valueOf(pknots_y[i]) + "\n");
            }

            writer.flush();
            writer.close();

        } catch (Exception e) {
            Log.i("EXCEPTION", String.valueOf(e));
        }
    }

    /**
     * Include libRaw methods from cpp
     */
    public native String stringFromJNI();  // Test Function

    public native void getInfo();

    public native void applyToneCurve(int[] pointsX, int[] pointsY, int rgb);

    public native void applyContrast(float value, int rgb);

    public native void applyBrightness(float value, int rgb);

    public native int[] getWBInd();

    public native int[] getWBCTInd();

    public native float[] getWBCTTemp();

    public native void applyWBUserMul(int index);

    public native void applyWBCTUserMul(int index);

    public native void setToneMap(boolean index);

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

    public native int[] getPixels8OnlyMem();

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

    public native void setBright(float bright);

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

    public native void setUserSat(int sat);

    public native void setNoAutoBright(int notAutoBright);
}


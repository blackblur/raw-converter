// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("rawconverter");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("rawconverter")
//      }
//    }

#include <libraw/libraw.h>
#include <jni.h>
#include <string>
#include <android/log.h>
#include "beziercurve.h"


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_rawconverter_LibRaw_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


/**
 * Libraw Object and methods
 */
libraw_data_t *libRawData = nullptr;
libraw_processed_image_t *image = nullptr;
ushort initCurve[0x10000];
ushort toneCurveR[0x10000];
ushort toneCurveG[0x10000];
ushort toneCurveB[0x10000];
ushort *toneCurves[3] = {toneCurveR, toneCurveG, toneCurveB};

float toneValR[2];
float toneValG[2];
float toneValB[2];
float *toneVals[3] = {toneValR, toneValG, toneValB};

void cleanup() {
    if (libRawData != nullptr) {
        libraw_recycle(libRawData);
        libRawData = nullptr;
    }
    if (image != nullptr) {
        libraw_dcraw_clear_mem(image);
        image = nullptr;
    }
}

libraw_processed_image_t *decode(int *error) {
    int dcraw = libraw_dcraw_process(libRawData);
    dcraw = libraw_dcraw_process_2(libRawData, toneCurves, toneVals);
    return libraw_dcraw_make_mem_image(libRawData, error);
}

libraw_processed_image_t *decodeOnlyMem(int *error) {
    int dcraw = libraw_dcraw_process_2(libRawData, toneCurves, toneVals);
    return libraw_dcraw_make_mem_image(libRawData, error);
}

// TODO TEST
extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_getInfo(JNIEnv *env, jobject jLibRaw) {
    int use_camera_wb = libRawData->params.use_camera_wb;
    int auto_wb = libRawData->params.use_auto_wb;
//    __android_log_print(ANDROID_LOG_INFO, "libraw", "Camera WB: %d, Auto WB: %d", use_camera_wb,
//                        auto_wb);
//    __android_log_print(ANDROID_LOG_INFO, "libraw", "Mul %f %f %f %f",
//                        libRawData->params.user_mul[0],
//                        libRawData->params.user_mul[1],
//                        libRawData->params.user_mul[2],
//                        libRawData->params.user_mul[3]);
//    __android_log_print(ANDROID_LOG_INFO, "libraw", "Brightness %f", libRawData->params.bright);
//    __android_log_print(ANDROID_LOG_INFO, "libraw", "Gamma %f %f",
//                        libRawData->params.gamm[0],
//                        libRawData->params.gamm[1]);
//
//    __android_log_print(ANDROID_LOG_INFO, "libraw", "Output Color %d",
//                        libRawData->params.output_color);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_applyToneCurve(JNIEnv *env, jobject jLibRaw,
                                                    jintArray pointsX, jintArray pointsY,
                                                    jint rgb) {
    jsize len = env->GetArrayLength(pointsX);
    jint *bodyX = env->GetIntArrayElements(pointsX, nullptr);
    jint *bodyY = env->GetIntArrayElements(pointsY, nullptr);

    if (len > 2) {
        vector<double> xX;
        vector<double> yY;
        vector<double> yCurve;

        int index = 0;
        for (int i = 0; i < len - 1; i += 3) {
            xX = {(double) bodyX[i], (double) bodyX[i + 1], (double) bodyX[i + 2],
                  (double) bodyX[i + 3]};
            yY = {(double) bodyY[i], (double) bodyY[i + 1], (double) bodyY[i + 2],
                  (double) bodyY[i + 3]};

            yCurve = computeBesierCurve2D(xX, yY, bodyX[i], bodyX[i + 3]);

            for (double curve: yCurve) {
                if (rgb == 3) {
                    toneCurves[0][index] = (int) curve;
                    toneCurves[1][index] = (int) curve;
                    toneCurves[2][index] = (int) curve;
                } else {
                    toneCurves[rgb][index] = (int) curve;
                }
                index++;
            }

        }
        if (rgb == 3) {
            toneCurves[0][index] = bodyY[len - 1];
            toneCurves[1][index] = bodyY[len - 1];
            toneCurves[2][index] = bodyY[len - 1];
        } else {
            toneCurves[rgb][index] = bodyY[len - 1];
        }
    } else {
        for (int i = 0; i < 0x10000; i++) {
            if (rgb == 3) {
                toneCurves[0][i] = (int) (
                        (double) i * (double) (bodyY[1] - bodyY[0]) / (bodyX[1] - bodyX[0]) +
                        (double) bodyY[0]);;
                toneCurves[1][i] = (int) (
                        (double) i * (double) (bodyY[1] - bodyY[0]) / (bodyX[1] - bodyX[0]) +
                        (double) bodyY[0]);;
                toneCurves[2][i] = (int) (
                        (double) i * (double) (bodyY[1] - bodyY[0]) / (bodyX[1] - bodyX[0]) +
                        (double) bodyY[0]);;
            } else {
                toneCurves[rgb][i] = (int) (
                        (double) i * (double) (bodyY[1] - bodyY[0]) / (bodyX[1] - bodyX[0]) +
                        (double) bodyY[0]);
            }
        }
    }

    env->ReleaseIntArrayElements(pointsX, bodyX, 0);
    env->ReleaseIntArrayElements(pointsY, bodyY, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_applyContrast(JNIEnv *env, jobject jLibRaw, jfloat value,
                                                   jint rgb) {
    if (rgb == 3) {
        toneVals[0][1] = value;
        toneVals[1][1] = value;
        toneVals[2][1] = value;
        __android_log_print(ANDROID_LOG_INFO, "libraw", "Contrast Val %f",
                            value);
    } else {
        toneVals[rgb][1] = value;
        __android_log_print(ANDROID_LOG_INFO, "libraw", "Contrast Val %f",
                            value);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_applyBrightness(JNIEnv *env, jobject jLibRaw, jfloat value,
                                                   jint rgb) {
    if (rgb == 3) {
        toneVals[0][0] = value;
        toneVals[1][0] = value;
        toneVals[2][0] = value;
        __android_log_print(ANDROID_LOG_INFO, "libraw", "Brightness Val %f",
                            value);
    } else {
        toneVals[rgb][0] = value;
        __android_log_print(ANDROID_LOG_INFO, "libraw", "Brightness Val %f",
                            value);
    }
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_rawconverter_LibRaw_getToneCurve(JNIEnv *env, jobject jLibRaw) {
    jintArray jintArray = env->NewIntArray(0x10000);
    jint fill[0x10000];
    for (int i = 0; i < 0x10000; i++) {
        fill[i] = toneCurves[0][i];
    }
    env->SetIntArrayRegion(jintArray, 0, 0x10000, fill);
    return jintArray;
}

extern "C" JNIEXPORT int JNICALL
Java_com_example_rawconverter_LibRaw_getMaximumColor(JNIEnv *env, jobject jLibRaw) {
    return libRawData->color.maximum;
}

/**
 * Methods Loading Data from a File
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_init(JNIEnv *env, jobject jLibRaw, int flags) {
    cleanup();
    for (auto & toneVal : toneVals) {
        toneVal[0] = 0.f;
        toneVal[1] = 1.f;
    }
    libRawData = libraw_init(flags);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_openFile(JNIEnv *env, jobject obj, jstring file) {
    if (libRawData != nullptr) {
        const char *nativeString = env->GetStringUTFChars(file, nullptr);
        int result = libraw_open_file(libRawData, nativeString);
        if (result == 0) {
            result = libraw_unpack(libRawData);
        }
        env->ReleaseStringUTFChars(file, nativeString);
        return result;
    }

    return 2;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_openBuffer(JNIEnv *env, jobject obj, jbyteArray buffer,
                                                jint size) {
    if (libRawData != nullptr) {
        auto ptr = env->GetPrimitiveArrayCritical(buffer, nullptr);
        int result = libraw_open_buffer(libRawData, ptr, size);
        if (result == 0) {
            result = libraw_unpack(libRawData);
        }

        // Copy initial color curve
        for (int i = 0; i < 0x10000; i++) {
            initCurve[i] = libRawData->color.curve[i];
            toneCurves[0][i] = i;
            toneCurves[1][i] = i;
            toneCurves[2][i] = i;
        }

        env->ReleasePrimitiveArrayCritical(buffer, ptr, 0);
        return result;
    }

    return 2;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_rawconverter_LibRaw_getThumbnail(JNIEnv *env, jobject obj, jbyteArray buffer,
                                                  jint size) {
    if (libRawData != nullptr) {
        auto ptr = env->GetPrimitiveArrayCritical(buffer, nullptr);
        int result = libraw_open_buffer(libRawData, ptr, size);
        if (result == 0) {
            result = libraw_unpack_thumb(libRawData);
        }
        env->ReleasePrimitiveArrayCritical(buffer, ptr, 0);

        if (result == 0) {
            jbyteArray jbyteArray = env->NewByteArray(libRawData->thumbnail.tlength);
            if (jbyteArray == nullptr)
                return nullptr;
            env->SetByteArrayRegion(jbyteArray, 0, libRawData->thumbnail.tlength,
                                    (jbyte *) libRawData->thumbnail.thumb);
            return jbyteArray;
        }
    }
    return nullptr;
}


/**
 * To Java image
 */
extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_rawconverter_LibRaw_getPixels8(JNIEnv *env, jobject obj) {
    int error;
    image = decode(&error);
    if (image != nullptr) {
        int *image8 = (int *) malloc(sizeof(int) * image->width * image->height);
        if (image8 == nullptr) {
            __android_log_print(ANDROID_LOG_INFO, "libraw", "getPixels8 oom");
            return nullptr;
        }
        __android_log_print(ANDROID_LOG_INFO, "libraw", "getPixels8 image colors %d",
                            image->colors);

        __android_log_print(ANDROID_LOG_INFO, "libraw", "asdfasdf %d %d",
                            image->width, image->height);
        int x, y;
        for (y = 0; y < image->height; y++) {
            for (x = 0; x < image->width; x++) {
                int pos = (x + y * image->width) * 3;
                image8[x + y * image->width] =
                        0xff000000 | (image->data[pos] << 16) | (image->data[pos + 1] << 8) |
                        (image->data[pos + 2]);
            }
        }
        jintArray jintArray = env->NewIntArray(image->width * image->height);
        env->SetIntArrayRegion(jintArray, 0, image->width * image->height, image8);
        free(image8);
        return jintArray;
    }
    __android_log_print(ANDROID_LOG_INFO, "libraw", "error getPixels8 %d", error);
    return nullptr;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_rawconverter_LibRaw_getPixels8OnlyMem(JNIEnv *env, jobject obj) {
    int error;
    image = decodeOnlyMem(&error);
    if (image != nullptr) {
        int *image8 = (int *) malloc(sizeof(int) * image->width * image->height);
        if (image8 == nullptr) {
            __android_log_print(ANDROID_LOG_INFO, "libraw", "getPixels8 oom");
            return nullptr;
        }
        __android_log_print(ANDROID_LOG_INFO, "libraw", "getPixels8 image colors %d",
                            image->colors);
        int x, y;
        for (y = 0; y < image->height; y++) {
            for (x = 0; x < image->width; x++) {
                int pos = (x + y * image->width) * 3;
                image8[x + y * image->width] =
                        0xff000000 | (image->data[pos] << 16) | (image->data[pos + 1] << 8) |
                        (image->data[pos + 2]);
            }
        }
        jintArray jintArray = env->NewIntArray(image->width * image->height);
        env->SetIntArrayRegion(jintArray, 0, image->width * image->height, image8);
        free(image8);
        return jintArray;
    }
    __android_log_print(ANDROID_LOG_INFO, "libraw", "error getPixels8 %d", error);
    return nullptr;
}


/**
 * Auxiliary Functions
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_recycle(JNIEnv *env, jobject jLibRaw) {
    cleanup();
}


/**
 * Image Getter and Setter
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_getBitmapWidth(JNIEnv *env, jobject obj) {
    return image->width;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_getBitmapHeight(JNIEnv *env, jobject obj) {
    return image->height;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_getWBCTCoeff(JNIEnv *env, jobject obj, jint cameraIndex, jint coeffIndex) {
    return libRawData->color.WBCT_Coeffs[cameraIndex][coeffIndex];
}

/**
 * libraw_output_params_t: Management of dcraw-Style Postprocessing
 * Documentation https://www.libraw.org/docs/API-datastruct.html#libraw_output_params_t
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setGreyBox(JNIEnv *env, jobject obj, jintArray greyBox) {
    jsize len = env->GetArrayLength(greyBox);
    if (len != 4) {
        throw std::invalid_argument("Grey Box Array should have a length of 4");
    }
    jint *body = env->GetIntArrayElements(greyBox, nullptr);
    for (int i = 0; i < 4; i++) {
        libRawData->params.greybox[i] = body[i];
    }
    env->ReleaseIntArrayElements(greyBox, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setCropBox(JNIEnv *env, jobject obj, jintArray cropBox) {
    jsize len = env->GetArrayLength(cropBox);
    if (len != 4) {
        throw std::invalid_argument("Crop Box Array should have a length of 4");
    }
    jint *body = env->GetIntArrayElements(cropBox, nullptr);
    for (int i = 0; i < 4; i++) {
        libRawData->params.cropbox[i] = body[i];
    }
    env->ReleaseIntArrayElements(cropBox, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setAber(JNIEnv *env, jobject obj, jdoubleArray aber) {
    jsize len = env->GetArrayLength(aber);
    if (len != 4) {
        throw std::invalid_argument("Crop Box Array should have a length of 4");
    }
    jdouble *body = env->GetDoubleArrayElements(aber, nullptr);
    for (int i = 0; i < 4; i++) {
        libRawData->params.aber[i] = body[i];
    }
    env->ReleaseDoubleArrayElements(aber, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setGamm(JNIEnv *env, jobject obj, jdoubleArray gamm) {
    jsize len = env->GetArrayLength(gamm);
    if (len != 6) {
        throw std::invalid_argument("Gamma Array should have a length of 6");
    }
    jdouble *body = env->GetDoubleArrayElements(gamm, nullptr);
    for (int i = 0; i < 6; i++) {
        libRawData->params.aber[i] = body[i];
    }
    env->ReleaseDoubleArrayElements(gamm, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setGammPower(JNIEnv *env, jobject obj, jdouble gamm) {
    libRawData->params.gamm[0] = gamm;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserMul(JNIEnv *env, jobject obj, jfloatArray userMul) {
    jsize len = env->GetArrayLength(userMul);
    if (len != 4) {
        throw std::invalid_argument("userMul Array should have a length of 4");
    }
    jfloat *body = env->GetFloatArrayElements(userMul, nullptr);
    for (int i = 0; i < 4; i++) {
        libRawData->params.user_mul[i] = body[i];
    }
    env->ReleaseFloatArrayElements(userMul, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setShotSelect(JNIEnv *env, jobject obj, jint shotSelect) {
    libRawData->params.shot_select = shotSelect;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setBright(JNIEnv *env, jobject obj, jfloat bright) {
    libRawData->params.bright = bright;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setThreshold(JNIEnv *env, jobject obj, jfloat threshold) {
    libRawData->params.threshold = threshold;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setHalfSize(JNIEnv *env, jobject obj, jboolean half_size) {
    libRawData->params.half_size = half_size;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setFourColorRGB(JNIEnv *env, jobject obj,
                                                     jboolean fourColorRGB) {
    libRawData->params.four_color_rgb = fourColorRGB;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setHighlight(JNIEnv *env, jobject obj, jint highlight) {
    libRawData->params.highlight = highlight;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setAutoWb(JNIEnv *env, jobject obj, jboolean autoWb) {
    libRawData->params.use_auto_wb = autoWb;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setCameraWb(JNIEnv *env, jobject obj, jboolean camWb) {
    libRawData->params.use_camera_wb = camWb;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setCameraMatrix(JNIEnv *env, jobject obj, jint cameraMatrix) {
    libRawData->params.use_camera_matrix = cameraMatrix;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setOutputColor(JNIEnv *env, jobject obj, jint outputColor) {
    libRawData->params.output_color = outputColor;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setOutputBps(JNIEnv *env, jobject obj, jint output_bps) {
    libRawData->params.output_bps = output_bps;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setOutputTiff(JNIEnv *env, jobject obj, jint output_tiff) {
    libRawData->params.output_tiff = output_tiff;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserFlip(JNIEnv *env, jobject obj, jint userFlip) {
    libRawData->params.user_flip = userFlip;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserQual(JNIEnv *env, jobject obj, jint quality) {
    libRawData->params.user_qual = quality;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserBlack(JNIEnv *env, jobject obj, jint black) {
    libRawData->params.user_black = black;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserCBlack(JNIEnv *env, jobject obj, jintArray cBlack) {
    jsize len = env->GetArrayLength(cBlack);
    if (len != 4) {
        throw std::invalid_argument("cBlack Array should have a length of 4");
    }
    jint *body = env->GetIntArrayElements(cBlack, nullptr);
    for (int i = 0; i < 4; i++) {
        libRawData->params.aber[i] = body[i];
    }
    env->ReleaseIntArrayElements(cBlack, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setUserSat(JNIEnv *env, jobject obj, jint sat) {
    libRawData->params.user_sat = sat;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setMedPasses(JNIEnv *env, jobject obj, jint medPasses) {
    libRawData->params.med_passes = medPasses;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setNoAutoBright(JNIEnv *env, jobject obj, jint noAutoBright) {
    libRawData->params.no_auto_bright = noAutoBright;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setAutoBrightThr(JNIEnv *env, jobject obj,
                                                      jfloat autoBrightThr) {
    libRawData->params.auto_bright_thr = autoBrightThr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setAdjustMaximumThr(JNIEnv *env, jobject obj,
                                                         jfloat adjustMaximumThr) {
    libRawData->params.adjust_maximum_thr = adjustMaximumThr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setGreenMatching(JNIEnv *env, jobject obj,
                                                      jint greenMatching) {
    libRawData->params.green_matching = greenMatching;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setDcbIterations(JNIEnv *env, jobject obj,
                                                      jint dcbIterations) {
    libRawData->params.dcb_iterations = dcbIterations;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setDcbEnhanceFL(JNIEnv *env, jobject obj, jint dcbEnhanceFl) {
    libRawData->params.dcb_enhance_fl = dcbEnhanceFl;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setFbddNoiserd(JNIEnv *env, jobject obj, jint fbddNoiserd) {
    libRawData->params.fbdd_noiserd = fbddNoiserd;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setExpCorrec(JNIEnv *env, jobject obj, jint expCorrec) {
    libRawData->params.exp_correc = expCorrec;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setExpShift(JNIEnv *env, jobject obj, jfloat expShift) {
    libRawData->params.exp_shift = expShift;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setExpPreser(JNIEnv *env, jobject obj, jfloat expPreser) {
    libRawData->params.exp_preser = expPreser;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setRawSpeed(JNIEnv *env, jobject obj, jint rawSpeed) {
    libRawData->params.use_rawspeed = rawSpeed;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setDngSdk(JNIEnv *env, jobject obj, jint dngSdk) {
    libRawData->params.use_dngsdk = dngSdk;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setNoAutoScale(JNIEnv *env, jobject obj, jint noAutoScale) {
    libRawData->params.no_auto_scale = noAutoScale;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setNoInterpolation(JNIEnv *env, jobject obj, jint noInterpol) {
    libRawData->params.no_interpolation = noInterpol;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setRawProcessingOptions(JNIEnv *env, jobject obj,
                                                             jint options) {
    libRawData->params.raw_processing_options = options;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setMaxRawMemoryMb(JNIEnv *env, jobject obj,
                                                       jint maxRawMemoryMb) {
    libRawData->params.max_raw_memory_mb = maxRawMemoryMb;
}


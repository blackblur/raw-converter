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

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_rawconverter_LibRaw_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

libraw_data_t *libRawData = NULL;
libraw_processed_image_t *image = NULL;
char *image16 = NULL;

void cleanup() {
    if (libRawData != NULL) {
        libraw_recycle(libRawData);
        libRawData = NULL;
    }
    if (image != NULL) {
        libraw_dcraw_clear_mem(image);
        image = NULL;
    }
}

libraw_processed_image_t *decode(int *error) {
    int dcraw = libraw_dcraw_process(libRawData);
    return libraw_dcraw_make_mem_image(libRawData, error);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_recycle(JNIEnv *env, jobject jLibRaw) {
    cleanup();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_init(JNIEnv *env, jobject jLibRaw, int flags) {
    cleanup();
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

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setOutputBps(JNIEnv *env, jobject obj, jint output_bps) {
    libRawData->params.output_bps = output_bps;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setQuality(JNIEnv *env, jobject obj, jint quality) {
    libRawData->params.user_qual = quality;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_rawconverter_LibRaw_setHalfSize(JNIEnv *env, jobject obj, jboolean half_size) {
    libRawData->params.half_size = half_size;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_getBitmapWidth(JNIEnv *env, jobject obj) {
    return image->width;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_example_rawconverter_LibRaw_getBitmapHeight(JNIEnv *env, jobject obj) {
    return image->height;
}
import rawpy
import imageio
import os
import io
import time

# "/data/user/0/com.example.rawconverter/files/img/_DSC2047.NEF"
def convert_to_jpg(open_path, save_path, file_name):
    if os.path.exists(open_path):
        with rawpy.imread(open_path) as raw:
            rgb = raw.postprocess()

        # if os.path.exists(save_path):
        #     imageio.imsave(save_path + file_name, rgb)
        #     return True
        # else:
        #     return False

    else:
        return False


def get_current_dir():
    return str(os.getcwd())


def list_dir():
    return os.listdir("/")


def open_raw(content):
    t = time.time()

    content = bytes(content)
    content_file = io.BytesIO(content)

    result = True

    try:
        with rawpy.imread(content_file) as raw:
            rgb = raw.num_colors
    except:
        result = False

    return time.time() - t

import rawpy
import imageio
import os


def convert_to_jpg(save_path, file_name):
    if os.path.exists("/data/user/0/com.example.rawconverter/files/img/_DSC2047.NEF"):
        with rawpy.imread("/data/user/0/com.example.rawconverter/files/img/_DSC2047.NEF") as raw:
            rgb = raw.postprocess()

        if os.path.exists(save_path):
            imageio.imsave(save_path + file_name, rgb)
            return True
        else:
            return False

    else:
        return False


def get_current_dir():
    return str(os.getcwd())


def list_dir():
    return os.listdir("/")

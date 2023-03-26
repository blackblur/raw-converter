# raw-converter

## Setup notes

Put test images in the emulator:
/data/user/0/com.example.rawconverter/files/img/_DSC2047.NEF

In /app/build.gradle change buildPython to your path where your python.exe is

           python {
            version "3.8"
            buildPython "F:\\workspaces\\playground\\venv\\Scripts\\python.exe"  // Change that

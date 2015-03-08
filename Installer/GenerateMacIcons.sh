#!/bin/bash

# This script is used on the Mac / OSX to create an icns file containing a
# complete set of all the required icon resolutions from a single 1024x1024 png
# file.
#
# In the nbproject/project.properties file, app.icon.icns holds the path to the
# application's icon on Mac. If not set, "${harness.dir}/etc/applicationIcon.icns"
# is used by default.

# Set this SRC to the 1024x1024 png original that will be used to generate all
# the other versions.
SRC=VirtMus1024x1024.png

mkdir VirtMus.iconset
sips -z 16 16     $SRC --out VirtMus.iconset/icon_16x16.png
sips -z 32 32     $SRC --out VirtMus.iconset/icon_16x16@2x.png
sips -z 32 32     $SRC --out VirtMus.iconset/icon_32x32.png
sips -z 64 64     $SRC --out VirtMus.iconset/icon_32x32@2x.png
sips -z 128 128   $SRC --out VirtMus.iconset/icon_128x128.png
sips -z 256 256   $SRC --out VirtMus.iconset/icon_128x128@2x.png
sips -z 256 256   $SRC --out VirtMus.iconset/icon_256x256.png
sips -z 512 512   $SRC --out VirtMus.iconset/icon_256x256@2x.png
sips -z 512 512   $SRC --out VirtMus.iconset/icon_512x512.png
cp $SRC VirtMus.iconset/icon_512x512@2x.png
iconutil -c icns VirtMus.iconset
rm -R VirtMus.iconset

#!/bin/bash

#=======================================================================
# CONFIGURATION:
# - extract tar.gz archives 
# - set the NDK variable below
#=======================================================================

export NDK=${HOME}/android-ndk

SYSROOT=$NDK/platforms/android-14/arch-arm
TOOLCHAIN=`echo $NDK/toolchains/arm-linux-androideabi-4.7/prebuilt/*-x86`
export PATH=$TOOLCHAIN/bin:$PATH

#=======================================================================
# build lame based on content from 
# https://github.com/intervigilium/liblame
#=======================================================================

cd liblame
$NDK/ndk-build
mv libs/armeabi/liblame.so libs/armeabi/libmp3lame.so
mv libs/armeabi-v7a/liblame.so libs/armeabi-v7a/libmp3lame.so

# copy libmp3lame files into android-ndk appropriate folders, to let the ffmpeg configure script find them
cp -rn jni/lame $SYSROOT/usr/include
cp -n libs/armeabi/libmp3lame.so $SYSROOT/usr/lib # copying only the armeabi version

cd ..

#=======================================================================
# build FFmpeg adapting content from 
# http://bambuser.com/opensource
# and using FFmpeg build 1.2
#=======================================================================

BASE_DIR=`pwd`

rm -rf build/ffmpeg
mkdir -p build/ffmpeg
cd ffmpeg-1.2

for version in armv5te armv7a; do

	DEST=$BASE_DIR/build/ffmpeg
	FLAGS="--target-os=linux --cross-prefix=arm-linux-androideabi- --arch=arm"
	FLAGS="$FLAGS --sysroot=$SYSROOT"
	FLAGS="$FLAGS --enable-small"
	FLAGS="$FLAGS --enable-gpl --enable-version3"
	FLAGS="$FLAGS --disable-ffplay --disable-ffprobe --disable-ffserver"

    FLAGS="$FLAGS --enable-libmp3lame"

	case "$version" in
		armv7a)
			EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp"
			EXTRA_LDFLAGS=""
			ABI="armeabi-v7a"
			;;
		*)
			EXTRA_CFLAGS=""
			EXTRA_LDFLAGS=""
			ABI="armeabi"
			;;
	esac
	DEST="$DEST/$ABI"
	FLAGS="$FLAGS --prefix=$DEST"

	mkdir -p $DEST
	echo $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" > $DEST/info.txt
	./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" | tee $DEST/configuration.txt
	[ $PIPESTATUS == 0 ] || exit 1
	make clean
	make -j4 || exit 1
	make prefix=$DEST install || exit 1

done


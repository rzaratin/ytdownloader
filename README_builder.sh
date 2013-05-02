#!/bin/bash

dest=README.md

echo "YouTube Downloader
================================================================

Android 3+ App to download videos from YouTube

################################################################
" > $dest

code=`grep -oE 'versionCode=".+"' dentex.youtube.downloader/AndroidManifest.xml`

name=`grep -oE 'versionName=".+"' dentex.youtube.downloader/AndroidManifest.xml`

md5=`md5sum dentex.youtube.downloader_v*.apk | sed 's/  /\` /'`

echo '`'$code'`' >> $dest

echo -e "\n\`$name\`" >> $dest

echo -e "\nMD5 checksum: \`"$md5 >> $dest

echo "
LICENSE
================================================================
Copyright (C) 2012  Samuele Rini

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
" >> $dest

echo "
OTHER LICENSES
================================================================
    Additional licenses informations about code used in this project
    is available from within the App's \"About\" menu:" >> $dest

text0=`sed -n '/string name="credits_text0"/,/<\/string>/p' dentex.youtube.downloader.strings/res/values/strings.xml | grep -v -E '\/string|string name'`
echo -e $text0 | sed -e 's/^/    /' -e 's/&#169;/(C)/g' -e 's/<http>//g' -e 's/<\/http>//g' -e 's/\\./`/g' >> $dest

text1=`sed -n '/string name="credits_text1"/,/<\/string>/p' dentex.youtube.downloader.strings/res/values/strings.xml | grep -v -E '\/string|string name'`
echo -e $text1 | sed -e 's/^/    /' -e 's/&#169;/(C)/g' -e 's/<http>//g' -e 's/<\/http>//g' -e 's/\\./`/g' >> $dest

echo "
     
    Furthermore, the device-framed screenshots in the project's directory have been 
    generated with the \"Device Frame Generator\" Android App by Prateek 
    Srivastava, available at 
    <https://github.com/f2prateek/Device-Frame-Generator/>.
    The generated artwork is released ander the \"Creative Commons 
    Attribution 3.0 Unported License\". 
    For further details, <http://creativecommons.org/licenses/by/3.0/>.
" >> $dest

echo "
CHANGELOG
================================================================" >> $dest

changelog=`sed -n '/string name="changelog"/,/<\/string>/p' dentex.youtube.downloader/res/values/donottranslate.xml | grep -v -E '\/string|string name'`

echo -e $changelog | sed 's/^/    /' >> $dest

echo "
TO-DO LIST
================================================================" >> $dest

cat TODO >> $dest

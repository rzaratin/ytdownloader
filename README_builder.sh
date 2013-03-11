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
CHANGELOG
================================================================" >> $dest

changelog=`sed -n '/string name="changelog"/,/<\/string>/p' dentex.youtube.downloader/res/values/not-to-localize_strings.xml | grep -v -E '\/string|string name'`

echo -e $changelog | sed 's/^/    /' >> $dest

echo "
TO-DO LIST
================================================================" >> $dest

cat TODO >> $dest

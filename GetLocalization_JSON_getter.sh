#!/bin/bash

# ---- config ----
ASSETS="$HOME/workspace_master/dentex.youtube.downloader/assets"
# ----------------

LANG_FILE=$ASSETS/languages

ID=dentex
PW=`zenity --entry --text="GetLocalization password:" --hide-text`
PWexit=$?

if [ "$PWexit" -ne 0 ]; then
	exit
fi

LANGS=`curl --user $ID:$PW https://api.getlocalization.com/ytdownloader/api/translations/list/json/ | \
	sed 's/}, {/}\n }/g' | awk '{print $4}' | sed 's/["|",]//g' | sort -u`
	
echo $LANGS | sed 's/ /\n/g' | tee $LANG_FILE

for i in `cat $LANG_FILE`; do curl --user $ID:$PW https://api.getlocalization.com/ytdownloader/api/translators/$i/json/ > $ASSETS/$i; done

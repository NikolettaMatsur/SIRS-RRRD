#!/bin/bash
inotifywait -e close_write,moved_to,create -m . |
while read -r directory events filename; do

if [[ ${filename: -4}  != ".txt" && $filename != .goutputstream* ]]; then
	echo  "Shutting Down"
    #sudo shutdown -h now
fi
done

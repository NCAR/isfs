#! /bin/bash

cd $HOME
(cat isfs/scripts/nidas_eol_relay/eol_relay.crontab; echo; cat weather/etc/eol-rt-data/crontab.eol-rt-data ) | crontab

echo "-------- Current crontab: --------"
crontab -l


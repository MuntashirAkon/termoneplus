#! /bin/sh

set -e

cd `dirname $0`

SOURCE=../docs/termoneplus-notification-icon.svg

for DENSITY in l m h xh xxh xxxh ; do
  #for VERSION in 11 9 '' ; do
  for VERSION in 11 ; do
    QUALIFIERS=
    test -n "$DENSITY" && QUALIFIERS="-$DENSITY"dpi
    case $DENSITY/$VERSION in
    l/11)	W=18; H=18;;
    #l/9)	W=12; H=19;;
    #l/)		W=19; H=19;;
    m/11)	W=24; H=24;;
    #m/9)	W=16; H=25;;
    #m/)		W=25; H=25;;
    h/11)	W=36; H=36;;
    #h/9)	W=24; H=38;;
    #h/)		W=38; H=38;;
    xh/11)	W=48; H=48;;
    #xh/9)	W=32; H=50;;
    #xh/)	W=50; H=50;;
    xxh/11)	W=72; H=72;;
    #xxh/9)	W=48; H=75;;
    #xxh/)	W=75; H=75;;
    xxxh*)	# TODO
      continue;;
    *)
      echo "unsupported density-version: $DENSITY-$VERSION" >&2
      exit 1
      ;;
    esac
    test -n "$VERSION" && QUALIFIERS=$QUALIFIERS-v$VERSION

    rsvg-convert -w $W -h $H $SOURCE -o notification-icon$QUALIFIERS.png

    #DIR=../term/src/main/res/drawable$QUALIFIERS
    DIR=../term/src/main/res/drawable"-$DENSITY"dpi
    test -d $DIR || mkdir -p $DIR
    mv -v notification-icon$QUALIFIERS.png $DIR/ic_stat_service_notification_icon.png
  done
done

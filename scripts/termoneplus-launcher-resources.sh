#! /bin/sh

set -e

cd `dirname $0`

XCFFILE=../docs/termoneplus-launcher-icon.xcf

for MODE in '' l m h xh xxh xxxh ; do
  case "$MODE" in
  l)	dpi=120;;
  m)	dpi=160;;
  h)	dpi=240;;
  xh)	dpi=320;;
  xxh)	dpi=480;;
  xxxh)	dpi=640;;
  *)	dpi=160;;
  esac

  # launcher icon size = 32 dp * ( dpi / 160 ) * 1.5
  size=`expr $dpi \* 3 / 10`

  qualifier=
  test -z "$MODE" || qualifier=-"$MODE"dpi

  PNGFILE=drawable"$qualifier"/ic_launcher.png
  echo creating .../$PNGFILE ... >&2

  # Start gimp with python-fu batch-interpreter
  gimp -i --batch-interpreter=python-fu-eval -b - << EOF
import gimpfu

def convert(xcf_file, new_size, png_file):
    img = pdb.gimp_file_load(xcf_file, xcf_file)
    layer = pdb.gimp_image_merge_visible_layers(img, 1)

    #pdb.gimp_image_convert_indexed(img, NO_DITHER, MAKE_PALETTE, 255, False, True, '');
    pdb.gimp_image_scale(img, new_size, new_size);

    pdb.gimp_file_save(img, layer, png_file, png_file)
    pdb.gimp_image_delete(img)

convert('${XCFFILE}', '${size}', '../term/src/main/res/${PNGFILE}')

pdb.gimp_quit(1)
EOF
done

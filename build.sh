#!/bin/bash
# Builds Aster: compiles the patched ImageJ sources, draws the app
# icon, and packages a self-contained macOS app + DMG with jpackage.
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$PWD"
SRC="$ROOT/imagej-src"
OUT="${TMPDIR:-/tmp}/aster-build"   # outside iCloud: codesign rejects iCloud xattrs
VERSION="1.0.0"     # Aster 1.0.0, based on ImageJ 1.54u
APP="Aster"

rm -rf "$OUT/classes" "$OUT/input" "$OUT/icon" "$OUT/dist"
mkdir -p "$OUT"; mkdir -p "$OUT/classes" "$OUT/input" "$OUT/icon/Aster.iconset" "$OUT/dist"

echo "==> Compiling ImageJ"
(cd "$SRC" && find ij -name "*.java" > "$OUT/sources.txt" &&
  javac -nowarn -encoding utf-8 -d "$OUT/classes" --release 8 @"$OUT/sources.txt" 2>&1 | grep -v "^Note\|warning: \[options\]\|^1 warning" || true)
cp "$SRC/IJ_Props.txt" "$SRC/images/microscope.gif" "$SRC/images/about.jpg" "$OUT/classes/"
cp "$SRC"/plugins/MacAdapter*.class "$OUT/classes/ij/plugin/"
cp -R "$SRC/macros" "$OUT/classes/"
printf "Main-Class: ij.StudioLauncher\n" > "$OUT/manifest.txt"
jar cfm "$OUT/input/ij.jar" "$OUT/manifest.txt" -C "$OUT/classes" .

echo "==> Drawing icon"
javac -d "$OUT/icon" "$ROOT/tools/IconGen.java"
java -Djava.awt.headless=true -cp "$OUT/icon" IconGen "$OUT/icon/icon_1024.png"
for s in 16 32 128 256 512; do
  sips -z $s $s "$OUT/icon/icon_1024.png" --out "$OUT/icon/Aster.iconset/icon_${s}x${s}.png" >/dev/null
  d=$((s*2))
  sips -z $d $d "$OUT/icon/icon_1024.png" --out "$OUT/icon/Aster.iconset/icon_${s}x${s}@2x.png" >/dev/null
done
iconutil -c icns "$OUT/icon/Aster.iconset" -o "$OUT/icon/Aster.icns"

cat > "$OUT/tif.properties" <<EOF
extension=tif
mime-type=image/tiff
description=TIFF Image
EOF
cat > "$OUT/tiff.properties" <<EOF
extension=tiff
mime-type=image/tiff
description=TIFF Image
EOF

COMMON=(--name "$APP" --app-version "$VERSION" --vendor "Aster"
  --input "$OUT/input" --main-jar ij.jar --main-class ij.StudioLauncher
  --icon "$OUT/icon/Aster.icns"
  --mac-package-identifier "io.github.ardaodabas.aster" --mac-package-name "$APP"
  --file-associations "$OUT/tif.properties" --file-associations "$OUT/tiff.properties"
  --java-options "-XX:MaxRAMPercentage=75 -Dapple.laf.useScreenMenuBar=true -Dfile.encoding=UTF-8"
  --add-modules java.base,java.desktop,java.prefs,java.scripting,java.sql,java.xml,java.logging,java.management,jdk.zipfs,java.datatransfer,jdk.unsupported,java.net.http,java.compiler,jdk.compiler,jdk.jsobject,jdk.charsets,jdk.crypto.ec,java.naming
  --dest "$OUT/dist")

echo "==> Building .app"
jpackage --type app-image "${COMMON[@]}"
echo "==> Building .dmg"
jpackage --type dmg "${COMMON[@]}"
cp "$OUT/dist/"*.dmg "$ROOT/"
ls -lh "$ROOT/"*.dmg

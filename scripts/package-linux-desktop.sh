#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -pl desktop dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package-input
cp desktop/target/coregnition-desktop-0.0.1-SNAPSHOT.jar desktop/target/package-input/desktop.jar
cp backend/target/coregnition-backend-0.0.1-SNAPSHOT.jar desktop/target/package-input/backend.jar
# A unique output directory keeps previous builds available.
APP_VERSION=$(sed 's/^v//; s/+.*//' VERSION)
OUTPUT=$(mktemp -d "$ROOT/desktop/target/linux-package-XXXXXX")
jpackage --type app-image --name Coregnition --dest "$OUTPUT" \
  --input desktop/target/package-input --main-jar desktop.jar \
  --main-class io.github.marcuzapl.coregnition.desktop.DesktopLauncher \
  --app-version "$APP_VERSION" --vendor Coregnition \
  --jlink-options '--strip-debug --no-man-pages --no-header-files' \
  --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs
printf 'Linux desktop application: %s/Coregnition/bin/Coregnition\n' "$OUTPUT"
tar -czf "$OUTPUT/coregnition-linux-x64.tar.gz" -C "$OUTPUT" Coregnition
printf 'Portable Linux archive: %s/coregnition-linux-x64.tar.gz\n' "$OUTPUT"

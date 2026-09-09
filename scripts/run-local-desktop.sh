#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
./mvnw --batch-mode --no-transfer-progress package -DskipTests
./mvnw --batch-mode --no-transfer-progress -pl desktop dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/runtime
exec java -cp "$ROOT/desktop/target/classes:$ROOT/desktop/target/runtime/*" \
  io.github.marcuzapl.coregnition.desktop.DesktopLauncher \
  "--backend-jar=$ROOT/backend/target/coregnition-backend-0.0.1-SNAPSHOT.jar" \
  "--data-dir=$ROOT/data"

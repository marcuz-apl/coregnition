#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ ! -f backend/target/coregnition-backend-0.0.1-SNAPSHOT.jar ]; then
  ./mvnw --batch-mode --no-transfer-progress package -DskipTests
fi

if [ ! -f web/node_modules/vite/bin/vite.js ]; then
  npm --prefix web ci
fi

java -jar backend/target/coregnition-backend-0.0.1-SNAPSHOT.jar >/tmp/coregnition-backend.log 2>&1 &
backend_pid=$!
(cd web && exec node node_modules/vite/bin/vite.js --host 0.0.0.0) >/tmp/coregnition-vite.log 2>&1 &
vite_pid=$!

cleanup() {
  kill "$vite_pid" "$backend_pid" 2>/dev/null || true
  wait "$vite_pid" 2>/dev/null || true
  wait "$backend_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

printf 'Coregnition web app: http://localhost:3040\n'
printf 'Backend health: http://127.0.0.1:3041/api/v1/health\n'
printf 'WSL address: run hostname -I if localhost forwarding is unavailable.\n'
printf 'Press Ctrl-C to stop both services.\n'
wait "$vite_pid"

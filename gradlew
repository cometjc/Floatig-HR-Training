#!/bin/sh
set -eu

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)
PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
GRADLE_VERSION=$(sed -n 's#.*gradle-\([0-9.]*\)-bin.zip#\1#p' "$PROPERTIES")
DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$GRADLE_VERSION-bin"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$DIST_DIR"
    ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
    if [ ! -f "$ZIP" ]; then
        curl -L -o "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    fi
    unzip -q "$ZIP" -d "$DIST_DIR"
fi

exec "$GRADLE_BIN" "$@"

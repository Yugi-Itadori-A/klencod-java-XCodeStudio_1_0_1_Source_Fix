#!/usr/bin/env sh
# XCode Studio Gradle bootstrapper. Does not require gradle-wrapper.jar.
set -eu

GRADLE_VERSION="8.11.1"
DIST_NAME="gradle-${GRADLE_VERSION}-bin"
BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/xcode-studio"
DIST_DIR="$BASE_DIR/$DIST_NAME"
GRADLE_BIN="$DIST_DIR/gradle-${GRADLE_VERSION}/bin/gradle"
ZIP_FILE="$BASE_DIR/$DIST_NAME.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$BASE_DIR"
  echo "XCode Studio: downloading Gradle ${GRADLE_VERSION}..."
  rm -f "$ZIP_FILE"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 20 \
      "https://services.gradle.org/distributions/${DIST_NAME}.zip" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_FILE" "https://services.gradle.org/distributions/${DIST_NAME}.zip"
  else
    echo "Error: curl or wget is required to download Gradle." >&2
    exit 1
  fi
  command -v unzip >/dev/null 2>&1 || {
    echo "Error: unzip is required to extract Gradle." >&2
    exit 1
  }
  TMP_DIR="$BASE_DIR/.extract-$$"
  rm -rf "$TMP_DIR"
  mkdir -p "$TMP_DIR"
  unzip -q "$ZIP_FILE" -d "$TMP_DIR"
  rm -rf "$DIST_DIR"
  mkdir -p "$DIST_DIR"
  mv "$TMP_DIR/gradle-${GRADLE_VERSION}" "$DIST_DIR/"
  rm -rf "$TMP_DIR"
  rm -f "$ZIP_FILE"
fi

exec "$GRADLE_BIN" "$@"

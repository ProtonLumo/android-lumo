#!/bin/bash
set -e

VERSION_NAME="$1"

if [[ -z "$VERSION_NAME" ]]; then
  echo "❌ No version name provided to set_version.sh"
  exit 1
fi

echo "🔢 Setting version to: $VERSION_NAME"

./gradlew setVersionName -PversionName="$VERSION_NAME"
./gradlew bumpVersionCode
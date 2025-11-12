#!/bin/bash
set -e

if [[ -z "$1" ]]; then
  echo "❌ Usage: $0 <version>"
  exit 1
fi

version="$1"
branch="release/$version"

# Verify branch doesn't exist
if git show-ref --verify --quiet "refs/heads/$branch"; then
  echo "⚠️ Branch '$branch' already exists — aborting"
  exit 1
fi

# Create and switch
git checkout -b "$branch"
echo "🟢 Created and switched to $branch"

# Bump version
bash ci/scripts/set_version.sh "$version"
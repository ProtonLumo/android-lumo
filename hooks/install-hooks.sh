#!/usr/bin/env bash
# This script configures git to use the hooks in this directory

HOOKS_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Configuring git to use hooks from: $HOOKS_DIR"
git config core.hooksPath "$HOOKS_DIR"

echo "✓ Git hooks installed successfully!"
echo ""
echo "The following hooks are now active:"
for hook in "$HOOKS_DIR"/*; do
  if [ -f "$hook" ] && [ -x "$hook" ] && [ "$(basename "$hook")" != "install-hooks.sh" ] && [ "$(basename "$hook")" != "README.md" ]; then
    echo "  - $(basename "$hook")"
  fi
done

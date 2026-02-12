# Git Hooks

This directory contains shared git hooks for the project.

## Installation

After cloning the repository, run:

```bash
./hooks/install-hooks.sh
```

This will configure your local git to use these hooks.

## Available Hooks

### pre-commit

Runs `detekt` before each commit to ensure code quality standards are met. If detekt finds any issues, the commit will be blocked until they are resolved.

## Manual Installation

If you prefer to install manually, run:

```bash
git config core.hooksPath hooks
```

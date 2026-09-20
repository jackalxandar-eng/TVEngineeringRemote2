#!/usr/bin/env bash
set -e
# Run this from the root of the extracted project inside a fresh GitHub Codespace.
git add .
git commit -m "Initial complete TV Engineering Remote app" || true
git push origin main

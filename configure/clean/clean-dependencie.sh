#!/bin/bash
HOME=$(pwd)

echo "Clean dependencies"
cd "$HOME/core/metaforge-ui"

echo "Task: check dependencies"
depcheck --json > depcheck-output.json

echo "Task: remove unused dependencies"
UNUSED_DEPENDENCIES=$(jq -r '.dependencies[]' depcheck-output.json)
for PACKAGE in $UNUSED_DEPENDENCIES; do
  pnpm remove $PACKAGE
done

echo "Task: remove unused dev dependencies"
UNUSED_DEV_DEPENDENCIES=$(jq -r '.devDependencies[]' depcheck-output.json)
for PACKAGE in $UNUSED_DEV_DEPENDENCIES; do
  pnpm remove $PACKAGE --save-dev
done

echo "Task: install dependencies again"
pnpm install --fix


echo "Clean dependencies done"
cd "$HOME"
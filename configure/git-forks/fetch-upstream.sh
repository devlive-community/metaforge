#!/bin/sh

git remote add upstream git@github.com:EdurtIO/metaforge.git
git remote -v
git fetch upstream
git merge upstream/dev

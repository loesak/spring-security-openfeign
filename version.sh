#!/bin/bash

GIT_DESCRIBE=$(git describe --tags --dirty)

if [[ $GIT_DESCRIBE =~ ^[0-9]\.[0-9]\.[0-9]$ ]]
then
  echo $GIT_DESCRIBE
else
  echo "$GIT_DESCRIBE-SNAPSHOT"
fi
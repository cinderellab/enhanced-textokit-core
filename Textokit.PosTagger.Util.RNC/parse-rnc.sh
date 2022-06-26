#!/bin/bash
if [[ -z $1 || -z $2  ]] ; then
  echo "Usage: <ruscorpora-text-dir> <output-dir>"
  exit 1
fi
java_app_arguments="--enable-dictionary-aligning"
java_app_arguments="$java
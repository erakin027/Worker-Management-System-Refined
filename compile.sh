#!/bin/bash

# CONFIGURE
JAR="lib/gson-2.13.2.jar"
SRC="src"
MAIN="app.Main"

if [ -z "$1" ]; then
  echo "Usage: ./compile.sh [compile|run|all|clean]"
  exit 1
fi

case "$1" in
  compile)
    echo "Compiling Java sources..."
    javac -cp "$JAR:$SRC" $SRC/app/*.java
    if [ $? -ne 0 ]; then
      echo
      echo "*** Compilation failed."
      exit 1
    fi
    echo "Compilation succeeded."
    ;;

  run)
    echo "Running $MAIN..."
    java -cp "$JAR:$SRC" $MAIN
    ;;

  all)
    ./compile.sh compile || exit 1
    ./compile.sh run
    ;;

  clean)
    echo "Cleaning .class files..."
    rm -f $SRC/app/*.class
    echo "Clean complete."
    ;;

  *)
    echo "Usage: ./compile.sh [compile|run|all|clean]"
    exit 1
    ;;
esac

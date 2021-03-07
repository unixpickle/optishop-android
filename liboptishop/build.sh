#!/bin/bash

if ! [ -d build ]; then
    mkdir build
fi

gomobile bind -target android -o build/liboptishop.aar -javapkg liboptishop .

#!/usr/bin/env bash

set -e

klib install native/build/konan/libs/linux_x64/multilib.klib

cp -rf $KLIB/multilib/targets/linux_x64 /tmp/

klib install native/build/konan/libs/wasm32/multilib.klib

mv /tmp/linux_x64 $KLIB/multilib/targets/
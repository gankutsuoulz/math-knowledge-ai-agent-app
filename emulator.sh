#!/usr/bin/env bash
# mathApp 安卓仿真封装：补 /dev/kvm、修 AVD 路径、等待启动、容器内 adb 截图
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
AVD_HOME="$ROOT/.avd"
SHOTS="$ROOT/shots"
NAME=android-emu
IMAGE=android-emulator:test-avd
ADB_IN=/opt/android-sdk/platform-tools/adb
APK="$ROOT/math-workspace/app/build/outputs/apk/debug/math-app-1.1.26.apk"

ensure_kvm() {
  [ -c /dev/kvm ] || mknod /dev/kvm c 10 232
  chmod 666 /dev/kvm
}

prepare_avd() {
  mkdir -p "$AVD_HOME" "$SHOTS"
  cat > "$AVD_HOME/test-avd.ini" <<INI
path=/opt/android-sdk/.android/avd/test-avd.avd
pathRel=avd/test-avd.avd
INI
}

wait_boot() {
  for i in $(seq 1 60); do
    b=$(docker exec "$NAME" "$ADB_IN" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
    [ "$b" = "1" ] && { echo "booted in ${i}x2s"; return 0; }
    sleep 2
  done
  echo "boot timeout"; return 1
}

start() {
  ensure_kvm; prepare_avd
  docker rm -f "$NAME" >/dev/null 2>&1 || true
  docker run -d --name "$NAME" --network=host --privileged \
    -v /dev/kvm:/dev/kvm \
    -v "$AVD_HOME:/root/.android/avd" \
    -v "$SHOTS:/projects/screenshots" \
    --entrypoint /bin/bash "$IMAGE" -lc '
      mkdir -p "/root/avd SD Card"
      /opt/android-sdk/emulator-2/qemu-img create -f qcow2 "/root/avd SD Card/test-avd.avd.sdcard" 512M >/dev/null 2>&1 || true
      exec /opt/android-sdk/emulator-2/emulator -avd test-avd -no-window \
        -gpu swiftshader_indirect -no-snapshot -no-audio -wipe-data -read-only -port 5554' >/dev/null
  wait_boot
  # 系统蓝牙在镜像里会反复崩溃，禁用掉（失败不影响使用）
  docker exec "$NAME" "$ADB_IN" shell pm disable-user --user 0 com.android.bluetooth >/dev/null 2>&1 || true
}

stop() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }

adb() { docker exec "$NAME" "$ADB_IN" "$@"; }

install_app() {
  docker cp "$APK" "$NAME:/tmp/app.apk"
  adb install -r /tmp/app.apk
  adb shell am force-stop com.mathkatex.verify
  adb shell am start -n com.mathkatex.verify/.MainActivity
}

shot() { adb exec-out screencap -p > "$SHOTS/${1:-screen}.png"; echo "$SHOTS/${1:-screen}.png"; }

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  install) install_app ;;
  shot) shot "${2:-screen}" ;;
  adb) shift; adb "$@" ;;
  *) echo "usage: $0 start|stop|install|shot <name>|adb <cmd...>"; exit 2 ;;
esac

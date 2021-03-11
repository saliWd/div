# Project documentation

* Goal: have a running tetris on the pico

## Example

See [heise article](https://www.heise.de/tests/Ausprobiert-Pimoroni-Picodisplay-fuer-Raspi-Pico-5055596.html?seite=all&hg=1&hgi=8&hgf=false)

## Toolchain

1. [Pimorini MicroPython Firmware](https://github.com/pimoroni/pimoroni-pico/releases)
1. [Thonny](https://thonny.org/) (Arduino 2beta does not yet support it. Or I didn't find it)
1. Demofiles, see in PicoFolder
   * to run it automatically, just name it main.py. Done.
1. ...next step, Tetris on it: [TomsHardware](https://www.tomshardware.com/news/pico-tetris-display-pack-demo)
   * rather this one: [tetris micropython](https://github.com/nahog/pico-tetris)

## HW

1. re-solder board B

## Issues

1. tkinter not available on micropython. Maybe [this](https://github.com/MatthiasLienhard/micropython_mqtt_controller) helps? But has other dependencies again from outside of micropython.

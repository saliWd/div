# Project documentation

* Goal: have a running tetris on the pico

## Example

* See [heise article](https://www.heise.de/tests/Ausprobiert-Pimoroni-Picodisplay-fuer-Raspi-Pico-5055596.html?seite=all&hg=1&hgi=8&hgf=false) for micropython
* For the C/C++ Example see [this heise article](https://www.heise.de/developer/artikel/Raspberry-Pi-Pico-und-C-C-eine-gute-Kombination-5991042.html)

## Toolchain

1. [C/C++ on windows](https://www.element14.com/community/community/raspberry-pi/blog/2021/01/24/working-with-the-raspberry-pi-pico-with-windows): working fine
1. [Animations and stuff](http://www.penguintutor.com/programming/picodisplay)
1. ...next step, Tetris on it: [TomsHardware](https://www.tomshardware.com/news/pico-tetris-display-pack-demo)
   * rather this one: [tetris micropython](https://github.com/nahog/pico-tetris)

### Outdated

1. [Pimorini MicroPython Firmware](https://github.com/pimoroni/pimoroni-pico/releases)

## HW

1. two working boards, one pimoroni display with buttons. Some more picos and easy to connect buttons might help? Next order.

## Issues

1. tkinter not available on micropython. Maybe [this](https://github.com/MatthiasLienhard/micropython_mqtt_controller) helps? But has other dependencies again from outside of micropython.

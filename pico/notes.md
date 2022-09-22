# Project documentation

* Goal: document the build chain to get widaktiv-target on current build tools. One build running on the pimorini display (using animations and background picture), the other headless with just one button.


## build chain

1. Get pico sdk: https://github.com/raspberrypi/pico-sdk. Tested with repository from 2022-07-26 (more or less release 1.4.0)
1. Get pico examples: https://github.com/raspberrypi/pico-examples Tested with repository from 2022-07-26
1. install latest cmake: download cmake-3.23.2-windows-x86_64.msi from https://cmake.org/download/, run it
1. install gcc for arm: gcc-arm-11.2-2022.02-mingw-w64-i686-arm-none-eabi.exe from arm website: https://developer.arm.com/tools-and-software/open-source-software/developer-tools/gnu-toolchain/downloads. Set the tick to add the path to env. variable
1. install visual studio with C++ development workloads https://visualstudio.microsoft.com/de/downloads/
1. install visual studio code: https://code.visualstudio.com/download and install the extension CMake Tools


## build the targets

1. open developer power shell VS2022
1. cd widaktiv
1. python prepareNmake.py
1. (...follow further instructions given by script)


## pico_w adaptions

1. project folder wireless
1. start with micropython to check wireless configuration / server side first
   1. server code is in folder RX, needs to be copied to the webserver
   1. simple counter value monitoring
   1. main.py: working fine, db_update as expected 
1. TODO: check serial connection with IR header. Doable?
1. TODO: use an example from the sdk



## Sources

* Overview for the C/C++ example: [heise article](https://www.heise.de/developer/artikel/Raspberry-Pi-Pico-und-C-C-eine-gute-Kombination-5991042.html)
* [C/C++ on windows](https://www.element14.com/community/community/raspberry-pi/blog/2021/01/24/working-with-the-raspberry-pi-pico-with-windows)
* [Animations and stuff](http://www.penguintutor.com/programming/picodisplay)
* maybe future ...next step, Tetris on it: [TomsHardware](https://www.tomshardware.com/news/pico-tetris-display-pack-demo)

## Div
* [some possible display for raspi](https://www.heise.de/news/Transparentes-OLED-Display-fuer-Raspberry-und-Arduino-Bastelrechner-7269567.html)


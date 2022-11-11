# Wmeter TODOs

1. different name: StromMesser (cut your energy consumption)
   1. website / wordpress and stuff for it
   2. re-org of git repo
2. current consumption
   1. monitor: about 26 mA (10 Ah = 3 days)
   2. check solar panel setup
   3. try frequency reduction, deepsleep again, ir-disable / wlan disable
3. main.py
   1. stability?
      1. monitor
      2. remove sleeps on UART, replace with poll-commands
4. index.php
   1. device as variable
   1. design
      * full width for canvas
      * check on mobile
      * css clean-up
   1. range selection, disable non-available ones
   1. password
5. rx_v2.php
6. green light when 0W
   1. ~~display. mpy with pimoroni support: [pimoroni-picow-vXX.uf2](https://github.com/pimoroni/pimoroni-pico/releases/latest/)~~
      * works, have a nicer output...
7. db
   1. ~~data thinning for older time ranges:~~ 
      * 3 days plus: 1 value per 3 hours (180)
      * maybe archive db?: 1 value per day?


Next:  6 / 1.1.
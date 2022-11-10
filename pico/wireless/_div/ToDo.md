# Wmeter TODOs

1. different name
   1. website / wordpress and stuff for it
   2. re-org of git repo
2. current consumption
   1. monitor: about 26 mA (10 Ah = 3 days)
   2. check solar panel setup
3. main.py
   1. stability?
      1. monitor
      2. remove sleeps on UART, replace with poll-commands
   2. ~~TXVER2: transmit auth hash~~
4. index.php
   1. ~~user database~~
   2. device as variable
   3. ~~number of data points visible -> db_thinning~~
   4. ~~generation selection?~~
   5. ~~Positive numbers instead of negative ones~~
   6. design
   7. range selection, disable non-available ones
   8. password   
5. getRX.php
   1. ~~moving average at insert~~
6. green light when 0W
   1. ~~second main.py~~
   2. ~~raw-output~~
   3. display. mpy with pimoroni support: [pimoroni-picow-vXX.uf2](https://github.com/pimoroni/pimoroni-pico/releases/latest/)
7. db
   1. ~~data thinning for older time ranges:~~ 
      * ~~24h plus: only 4 per hour (from about 60)~~


Next:  6.3 / 
# Wmeter TODOs

1. different name
   1. website / wordpress and stuff for it
   2. re-org of git repo
2. current consumption
   1. monitor
   2. check solar panel setup
3. main.py
   1. stability?
      1. monitor
      2. remove sleeps on UART, replace with poll-commands
   2. ~~TXVER2: transmit auth hash~~
4. index.php
   1. ~~user database~~
   2. device as variable
   3. number of data points visible
   4. ~~generation selection?~~
   5. ~~Positive numbers instead of negative ones~~
   6. design
   7. range selection, disable non-available ones
   8. password
   9. raw-output
5. getRX.php
   1. ~~moving average at insert~~
6. green light when 0W
   1. second main.py
7. db
   1. data thinning for older time ranges: 
      * 24h plus: only 4 per hour (from about 60)
8. tools
   1. device simulator (post)

Next: 4.8 / 6.1 / 
import network
import urequests
from time import sleep
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(5)
wlan.connect("widmedia_mobile","publicPassword")
sleep(5)
print(wlan.isconnected())
counterValue = 0
while True:
    message = "https://widmedia.ch/pico/getRX.php?TX=pico&value0="+str(counterValue)
    print(message)  # debug purpose only
    urequests.post(message)
    counterValue = counterValue + 1 # I don't care about overflow
    sleep(15)  # in seconds. Do not set it below ~3 to limit the number of requests
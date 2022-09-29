import network
import urequests
from time import sleep
from machine import Pin, Timer

led_onboard = Pin("LED", Pin.OUT)
counterValue = 0
message = ""
wlan_ok = False
tim = Timer() # TODO: should I specify a number? I don't really care about precision etc.

# define the toggle as function. Overkill for now but might be expanded later
def blink(timer):
    led_onboard.toggle()

led_onboard.on()

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(3)

while not wlan_ok:
    wlan.connect("widmedia_mobile","publicPassword")
    sleep(3)
    wlan_ok = wlan.isconnected()
    print(wlan_ok) # debug output

# signals wlan connection is ok
tim.init(freq=2.5, mode=Timer.PERIODIC, callback=blink)
sleep(5) # let it blink for some seconds before going back to static values
tim.deinit()
led_onboard.off()

while True:
    message = "https://widmedia.ch/pico/getRX.php?TX=pico&device=home&value0="+str(counterValue)
    print(message)  # debug purpose only
    response = urequests.post(message)    
    counterValue = counterValue + 1 # I don't care about overflow
    response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    sleep(5)  # in seconds. Do not set it below ~3 to limit the number of requests    
    led_onboard.toggle()
# end while

import network # type: ignore (this is a pylance ignore warning directive)
import urequests # type: ignore
from time import sleep
from machine import Pin, Timer # type: ignore


# my own files
import my_config
from my_functions import debug_sleep, wlan_connect, urlencode, get_randNum_hash


def send_message_get_response(DEBUG_SETTINGS:dict, message:dict, wait_time:int, led_onboard):
    # about TXVER: integer (range 0 to 9), increases when there is a change on the transmitted value format 
    # 0 is doing GET-communication, 1 uses post to transmit an identifier, values as blob
    # 2 uses authentification with a hash when sending
    if(not DEBUG_SETTINGS["wlan_sim"]): # not sending anything in simulation
        URL = "https://widmedia.ch/wmeter/getRaw.php?TX=pico&TXVER=2"
        HEADERS = {'Content-Type':'application/x-www-form-urlencoded'}

        urlenc = urlencode(message)
        response = urequests.post(URL, data=urlenc, headers=HEADERS)
        print("Response: "+response.text)
        response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    debug_sleep(DEBUG_SETTINGS=DEBUG_SETTINGS,time=wait_time)
    led_onboard.toggle() # signal success


# debug stuff
DEBUG_SETTINGS = my_config.get_debug_settings()
LOOP_WAIT_TIME = 40

# pins
led_onboard = Pin("LED", Pin.OUT)

# machine specific stuff
tim = Timer() # no need to specify a number on pico, all SW timers

# normal variables
wlan_ok = False

## program starts here
led_onboard.off()

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(3)

device_config = my_config.get_device_config()

while True:
    randNum_hash = get_randNum_hash(device_config)
    
    message = dict([
        ('device', device_config['device_name']),
        ('randNum', randNum_hash['randNum']),
        ('hash', randNum_hash['hash'])
        ])
    # debug_print(DEBUG_SETTINGS=DEBUG_SETTINGS, text=str(message))
    
    wlan_connect(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan, tim=tim, led_onboard=led_onboard) # try to connect to the WLAN. Hangs there if no connection can be made
    send_message_get_response(DEBUG_SETTINGS=DEBUG_SETTINGS, message=message, wait_time=LOOP_WAIT_TIME, led_onboard=led_onboard) # does not send anything when in simulation
# end while

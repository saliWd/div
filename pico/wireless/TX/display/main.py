import network # type: ignore (this is a pylance ignore warning directive)
import urequests # type: ignore
from time import sleep
from machine import Pin, Timer # type: ignore
from hashlib import sha256
from binascii import hexlify
from random import randint

# my own files
import my_config


def debug_print(DEBUG_SETTINGS:dict, text:str):
    if(DEBUG_SETTINGS["print"]):
        print(text)
    # otherwise just return

def debug_sleep(DEBUG_SETTINGS:dict, time:int):
    if(DEBUG_SETTINGS["sleep"]): # minimize wait times by sleeping only one second instead of the normal amount
        sleep(1)
        return
    sleep(time)

# define the toggle as function. Overkill for now but might be expanded later
def blink(timer):
    led_onboard.toggle()

def get_wlan_ok(DEBUG_SETTINGS:dict, wlan):
    if(DEBUG_SETTINGS["wlan_sim"]):
        return(True)
    return(wlan.isconnected())

def wlan_connect(DEBUG_SETTINGS:dict, wlan, tim, led_onboard):
    wlan_ok = get_wlan_ok(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan)
    if(wlan_ok):
        return() # nothing to do
    else:
        tim.init(freq=4.0, mode=Timer.PERIODIC, callback=blink) # signals I'm searching for WLAN    
        while not wlan_ok:
            config_wlan = my_config.get_wlan_config() # stored in external file
            wlan.connect(config_wlan['ssid'], config_wlan['pw'])
            sleep(3)
            wlan_ok = get_wlan_ok(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan)
            print("WLAN connected? "+str(wlan_ok)) # debug output

        # signals wlan connection is ok
        tim.deinit()
        led_onboard.on()
    
def urlencode(dictionary:dict):
    urlenc = ""
    for key, val in dictionary.items():
        urlenc += "%s=%s&" %(key,val)
    urlenc = urlenc[:-1] # gets me something like 'val0=23&val1=bla space'
    return(urlenc)

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
    rand_num = randint(1, 10000)
    myhash = sha256(str(rand_num)+device_config['post_key'])
    hashString = hexlify(myhash.digest())
    
    message = dict([
        ('device', device_config['device_name']),
        ('randNum', rand_num),
        ('hash', hashString.decode())
        ])
    # debug_print(DEBUG_SETTINGS=DEBUG_SETTINGS, text=str(message))
    
    wlan_connect(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan, tim=tim, led_onboard=led_onboard) # try to connect to the WLAN. Hangs there if no connection can be made
    send_message_get_response(DEBUG_SETTINGS=DEBUG_SETTINGS, message=message, wait_time=LOOP_WAIT_TIME, led_onboard=led_onboard) # does not send anything when in simulation
# end while

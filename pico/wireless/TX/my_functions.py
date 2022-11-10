from time import sleep
from machine import Timer # type: ignore
from hashlib import sha256
from binascii import hexlify
from random import randint

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

def get_wlan_ok(DEBUG_SETTINGS:dict, wlan):
    if(DEBUG_SETTINGS["wlan_sim"]):
        return(True)
    return(wlan.isconnected())

def blink(led_onboard):
    led_onboard.toggle()

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

def get_randNum_hash(device_config):
    rand_num = randint(1, 10000)
    myhash = sha256(str(rand_num)+device_config['post_key'])
    hashString = hexlify(myhash.digest())
    returnVal = dict([
        ('randNum', rand_num),
        ('hash', hashString.decode())
    ])
    return(returnVal)

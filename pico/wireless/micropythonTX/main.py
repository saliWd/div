import network # type: ignore (this is a pylance ignore warning directive)
import urequests # type: ignore
from time import sleep
from machine import Pin, Timer, UART # type: ignore
# my own files
import my_config

# 0 is doing GET-communication
# 1 uses post. transmits a identifier, values as blob, sends to RX1.php
TX_INTERFACE_VERSION = 1 # integer (range 0 to 9), just increasing when there is a change on the transmitted value format 


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

def uart_ir_e350(DEBUG_SETTINGS:dict, uart_ir):
    if(DEBUG_SETTINGS["ir_sim"]):
        return('/LGZ4ZMF100AC.M26\r\n\x02F.F(00)\r\n0.0(          120858)\r\nC.1.0(13647123)\r\nC.1.1(        )\r\n1.8.1(042951.721*kWh)\r\n1.8.2(018609.568*kWh)\r\n2.8.1(000000.302*kWh)\r\n2.8.2(000010.188*kWh)\r\n1.8.0(061561.289*kWh)\r\n2.8.0(000010.490*kWh)\r\n15.8.0(061571.780*kWh)\r\nC.7.0(0008)\r\n32.7(241*V)\r\n52.7(243*V)\r\n72.7(242*V)\r\n31.7(000.35*A)\r\n51.7(000.52*A)\r\n71.7(000.47*A)\r\n82.8.1(0000)\r\n82.8.2(0000)\r\n0.2.0(M26)\r\nC.5.0(0401)\r\n!\r\n\x03\x01')
    if (uart_ir.any() != 0):
        uart_ir.read() # first clear everything. This should return None
        print('Warning: UART buffer was not empty at first read')
    uart_ir.write('\x2F\x3F\x21\x0D\x0A') # in characters: '/?!\r\n'
    sleep(1) # need to make sure it has been sent but not wait more than 2 secs. TODO: maybe use uart_ir.flush()
    uart_str_id = uart_ir.read() # should be b'/LGZ4ZMF100AC.M26\r\n'
    uart_ir.write('\x06\x30\x30\x30\x0D\x0A') # in characters: ACK000\r\n
    sleep(2) 
    uart_str_values_0 = uart_ir.read()
    sleep(2) 
    uart_str_values_1 = uart_ir.read()
    sleep(2) 
    if (uart_ir.any() != 0):
        print('Warning: UART buffer is not empty after two reads')
        
    if ((uart_str_id == None) or (uart_str_values_0 == None) or (uart_str_values_1 == None)):
        return('uart communication did not work')
    else:
        return(uart_str_id.decode()+uart_str_values_0.decode()+uart_str_values_1.decode())

def invalidUartStr(uart_received_str:str):
    return(len(uart_received_str) < 20) # TODO: expected length?

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

def send_message_and_wait_post(DEBUG_SETTINGS:dict, message:dict, wait_time:int, led_onboard, TX_INTERFACE_VERSION:int):
    if(not DEBUG_SETTINGS["wlan_sim"]): # not sending anything in simulation
        URL = "https://widmedia.ch/wmeter/getRX1.php?TX=pico&TXVER="+str(TX_INTERFACE_VERSION)
        HEADERS = {'Content-Type':'application/x-www-form-urlencoded'}

        urlenc = urlencode(message)
        response = urequests.post(URL, data=urlenc, headers=HEADERS)
        debug_print(DEBUG_SETTINGS=DEBUG_SETTINGS, text="Text:"+response.text)
        response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    debug_sleep(DEBUG_SETTINGS=DEBUG_SETTINGS,time=wait_time)
    led_onboard.toggle() # signal success


## constants
# debug stuff
DEBUG_SETTINGS = my_config.get_debug_settings()

# pins
led_onboard = Pin("LED", Pin.OUT)
enable3v3_pin = Pin(28, Pin.OUT) # solder pin GP28 to '3V3_EN'-pin

# machine specific stuff
tim = Timer() # no need to specify a number on pico, all SW timers
uart_ir = UART(0, baudrate=300, bits=7, parity=0, stop=1, tx=Pin(0), rx=Pin(1))

# normal variables
wlan_ok = False

## program starts here
led_onboard.off()
enable3v3_pin.off()

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(3)

while True:
    enable3v3_pin.on() # power on IR head
    debug_sleep(DEBUG_SETTINGS=DEBUG_SETTINGS, time=2) # make sure 3.3V power is stable
    uart_received_str = uart_ir_e350(DEBUG_SETTINGS=DEBUG_SETTINGS, uart_ir=uart_ir) # this takes some seconds
    # print(uart_received_str)
    enable3v3_pin.off() # power down IR head

    # find parameters
    if (invalidUartStr(uart_received_str=uart_received_str)):
        print('Warning: uart string not as expected')
        debug_sleep(DEBUG_SETTINGS=DEBUG_SETTINGS, time=10)
        continue

    
    message = dict([('device',my_config.get_device_name()),('ir_answer',uart_received_str)])
    debug_print(DEBUG_SETTINGS=DEBUG_SETTINGS, text=str(message))
    
    wlan_connect(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan, tim=tim, led_onboard=led_onboard) # try to connect to the WLAN. Hangs there if no connection can be made

    send_message_and_wait_post(DEBUG_SETTINGS=DEBUG_SETTINGS, message=message, wait_time=10, led_onboard=led_onboard, TX_INTERFACE_VERSION=TX_INTERFACE_VERSION) # does not send anything when in simulation
# end while

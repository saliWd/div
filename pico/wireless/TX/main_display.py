import network # type: ignore (this is a pylance ignore warning directive)
import urequests # type: ignore
from time import sleep
from pimoroni import RGBLED  # type: ignore
from picographics import PicoGraphics, DISPLAY_PICO_DISPLAY  # type: ignore

# my own files
import my_config
from my_functions import debug_print, debug_sleep, wlan_connect, urlencode, get_randNum_hash


def send_message_get_response(DEBUG_SETTINGS:dict, message:dict):    
    if (DEBUG_SETTINGS["wlan_sim"]):
        return("57 W heute um 11:04:59") # attention: `heute` may also be a date like `2022-11-10`
    
    URL = "https://widmedia.ch/wmeter/getRaw.php?TX=pico&TXVER=2"
    HEADERS = {'Content-Type':'application/x-www-form-urlencoded'}

    urlenc = urlencode(message)
    response = urequests.post(URL, data=urlenc, headers=HEADERS)
    debug_print(DEBUG_SETTINGS, response.text)
    returnText = response.text    
    response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    return(returnText)

# debug stuff
DEBUG_SETTINGS = my_config.get_debug_settings()
LOOP_WAIT_TIME = 60

# normal variables
wlan_ok = False

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(3)

device_config = my_config.get_device_config()

display = PicoGraphics(display=DISPLAY_PICO_DISPLAY, rotate=0)
led = RGBLED(6, 7, 8)
display.set_backlight(0.5)
display.set_font("sans")
WIDTH, HEIGHT = display.get_bounds() # 240x135
BLACK = display.create_pen(0, 0, 0)
WHITE = display.create_pen(255, 255, 255)
VALUE_MAX = 3 * HEIGHT # 405
bar_width = 5
wattValues = []
colors = [(0, 0, 255), (0, 255, 0), (255, 255, 0), (255, 0, 0)]

def value_to_color(value): # value must be between 0 and VALUE_MAX
    f_index = float(value) / float(VALUE_MAX)
    f_index *= len(colors) - 1
    index = int(f_index)

    if index == len(colors) - 1:
        return colors[index]

    blend_b = f_index - index
    blend_a = 1.0 - blend_b

    a = colors[index]
    b = colors[index + 1]

    return [int((a[i] * blend_a) + (b[i] * blend_b)) for i in range(3)]

def right_align(value):
    expand = ""
    if value < 1000:
        expand = " "
    if value < 100:
        expand = "  "
    if value < 10:
        expand = "   "
    return expand

def blink_led(led, red:bool): # TODO: this takes about seconds to run
    color = (0, 127, 0) # green, but not as bright as possible
    if (red):
        color = (255, 0, 0) # red, as bright as possible
    for i in range(19):
        led.set_rgb(*color)
        sleep(1)
        led.set_rgb(*(0, 0, 0))
        sleep(1)
    return -38

while True:
    randNum_hash = get_randNum_hash(device_config)
    waitTimeAdjust = 0
    
    message = dict([
        ('device', device_config['device_name']),
        ('randNum', randNum_hash['randNum']),
        ('hash', randNum_hash['hash'])
        ])
        
    wlan_connect(DEBUG_SETTINGS=DEBUG_SETTINGS, wlan=wlan, tim=False, led_onboard=False) # try to connect to the WLAN. Hangs there if no connection can be made
    wattValueString = send_message_get_response(DEBUG_SETTINGS=DEBUG_SETTINGS, message=message) # does not send anything when in simulation
    validValue = wattValueString.split("|")
    if (len(validValue) != 2 ):
        valid = 0
        wattValue = 999
    else:
        valid = int(validValue[0])
        wattValue = int(validValue[1])
    
    # normalize the value
    wattValueNonMaxed = wattValue
    wattValue = min(wattValue, VALUE_MAX)
    wattValue = max(wattValue, 0)

    debug_print(DEBUG_SETTINGS, "watt value: "+str(wattValue))

    # fills the screen with black
    display.set_pen(BLACK)
    display.clear()

    wattValues.append(wattValue)
    if len(wattValues) > WIDTH // bar_width: # shifts the wattValues history to the left by one sample
        wattValues.pop(0)

    i = 0
    for t in wattValues:        
        VALUE_COLOUR = display.create_pen(*value_to_color(t))
        display.set_pen(VALUE_COLOUR)
        display.rectangle(i, int(HEIGHT - (float(t) / 3.0)), bar_width, HEIGHT) # TODO: height-t needs to match with min/max scaling
        i += bar_width

    display.set_pen(WHITE)
    display.rectangle(1, 1, 137, 41) # draws a white background for the text

    expand = right_align(wattValueNonMaxed) # string formatting does not work correctly. Do it myself
    wattValueNonMaxed = min(wattValueNonMaxed, 9999) # limit it to 4 digits

    # writes the reading as text in the white rectangle
    display.set_pen(BLACK)
    display.text(expand+str(wattValueNonMaxed), 7, 23, wordwrap=100, scale=1.1) # format does not work correctly
    display.text(expand+str(wattValueNonMaxed), 8, 23, wordwrap=100, scale=1.1) # making it 'bold' by shifting it 1px right (not very nice hack)

    display.text("W", 104, 23, wordwrap=100, scale=1.1)
    display.text("W", 105, 23, wordwrap=100, scale=1.1)

    display.update()

    # lets also set the LED to match
    if (valid == 1):
        led.set_rgb(*value_to_color(wattValue))
        if (wattValueNonMaxed == 0):
            waitTimeAdjust = blink_led(led, red=False)
    else:
        waitTimeAdjust = blink_led(led, red=True)

    debug_sleep(DEBUG_SETTINGS=DEBUG_SETTINGS,time=LOOP_WAIT_TIME+waitTimeAdjust)
    
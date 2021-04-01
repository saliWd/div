import usb_hid
import time
from adafruit_hid.keyboard import Keyboard
from adafruit_hid.keycode import Keycode

# Set up a keyboard device
kbd = Keyboard(usb_hid.devices)

# Type lowercase 'a'. Presses the 'a' key and releases it.
kbd.send(Keycode.A)

# Type capital 'A'.
kbd.send(Keycode.SHIFT, Keycode.A)

i = 0
sleep_time_sec = 5
runtime_min = 60
repetitions = runtime_min * 60 / sleep_time_sec


while i < repetitions:
    time.sleep(sleep_time_sec)
    kbd.send(Keycode.A)
    i += 1
a
# Release all keys.
kbd.release_all()

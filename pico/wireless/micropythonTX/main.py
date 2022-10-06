import network
import urequests
from time import sleep
from machine import Pin, Timer, UART

# pins
led_onboard = Pin("LED", Pin.OUT)
enable3v3_pin = Pin(28, Pin.OUT) # solder pin GP28 to '3V3_EN'-pin

# machine specific stuff
tim = Timer() # no need to specify a number on pico, all SW timers
uart_ir = UART(0, baudrate=300, bits=7, parity=0, stop=1, tx=Pin(0), rx=Pin(1))

# normal variables
counterValue = 0
message = ""
wlan_ok = False

# define the toggle as function. Overkill for now but might be expanded later
def blink(timer):
    led_onboard.toggle()


## program starts here
led_onboard.off()
enable3v3_pin.off()

##########################################################################################
### temporary, UART trials (TODO: will be moved into while loop after WLAN connection)
enable3v3_pin.on() # power on IR head
sleep(2)

# try sending the init sequence
uart_ir.write('/?!\r\n')
ir_answer = uart_ir.read() # read all. should be /LGZ4ZMF100AC.M23 (without anything connected it is b'\x00')
print("UART answer is "+str(ir_answer)) # TODO with this read I get: UART answer is None

sleep(10)  # required?

uart_ir.write('000\r\n') # need to ack it. Then the values should appear...
# read all
# (without anything connected it is 'NONE')
ir_answer = uart_ir.read() # should be several lines like "/?!\\ /LGZ4ZMF100AC.M23 000 F.F(00) C.1.0(12314330) 0.0(00188123        )" and so on

print("UART second answer is "+str(ir_answer)) # TODO with this read I get: UART second answer is b'/LGZ4ZMF100AC.M26\r\n'

sleep(2)
enable3v3_pin.off() # power off IR head. Save some power
### /end of temporary, UART trials 
##########################################################################################




tim.init(freq=4.0, mode=Timer.PERIODIC, callback=blink) # signals I'm searching for WLAN
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
sleep(3)

while not wlan_ok:
    wlan.connect("widmedia_mobile","publicPassword")
    sleep(3)
    wlan_ok = wlan.isconnected()
    print("WLAN connected? "+str(wlan_ok)) # debug output

# signals wlan connection is ok
tim.deinit()
led_onboard.on()

while True:
    message = "https://widmedia.ch/pico/getRX.php?TX=pico&device=home&value0="+str(counterValue)
    print(message)  # debug purpose only
    response = urequests.post(message)    
    counterValue = counterValue + 1 # I don't care about overflow
    response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    sleep(5)  # in seconds. Do not set it below ~3 to limit the number of requests    
    led_onboard.toggle()
# end while

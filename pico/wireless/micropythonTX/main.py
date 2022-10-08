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

def uart_comm(uart_ir, text, iteration):
    uart_ir.write(text)
    sleep(2) # make sure it has been sent. Required?
    ir_answer = uart_ir.read() # read all
    sleep(2) # Required?
    print(str(iteration)+". UART answer is "+str(ir_answer))
    return(iteration+1)


## program starts here
led_onboard.off()
enable3v3_pin.off()

##########################################################################################
### temporary, UART trials (TODO: will be moved into while loop after WLAN connection)
enable3v3_pin.on() # power on IR head
sleep(2) # make sure 3.3V power is stable
iteration = 0

# send the init sequence
iteration = uart_comm(uart_ir, '/?!\r\n', iteration) # response: UART answer is b'/LGZ4ZMF100AC.M26\r\n'. as it should be (without anything connected it is b'\x00')

sleep(10)  # required?

# need to ack it
iteration = uart_comm(uart_ir, '000\r\n', iteration) # should be several lines like "/?!\\ /LGZ4ZMF100AC.M23 000 F.F(00) C.1.0(12314330) 0.0(00188123        )" and so on
# TODO with this read I get: 1. UART answer is None

#### trial. Just do another ir_communication
iteration = uart_comm(uart_ir, '000\r\n', iteration) # TODO, I get: 2. UART answer is None

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

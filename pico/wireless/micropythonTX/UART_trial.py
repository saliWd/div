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
iteration = 0


############# WOOOOORKS!
uart_ir.write('\x2F\x3F\x21\x0D\x0A')
sleep(1)
print("UART answer is "+str(uart_ir.read())) # output: UART answer is b'/LGZ4ZMF100AC.M26\r\n'
uart_ir.write('\x06\x30\x30\x30\x0D\x0A')
sleep(2) 
print("UART answer is "+str(uart_ir.read())) # first part of answer
sleep(2) 
print("UART answer is "+str(uart_ir.read())) # second part of answer
sleep(2) 
print("UART answer is "+str(uart_ir.read())) # none


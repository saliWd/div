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

# debug stuff
IR_SIMULATION = True

# normal variables
counterValue = 0
message = ""
wlan_ok = False
iteration = 0

# define the toggle as function. Overkill for now but might be expanded later
def blink(timer):
    led_onboard.toggle()

def uart_ir_e350(uart_ir, IR_SIMULATION):
    if(IR_SIMULATION):
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
    return(uart_str_id.decode()+uart_str_values_0.decode()+uart_str_values_1.decode())

def find_positions(uart_received_str, positions):
    positions.append(uart_received_str.find("1.8.1(")+6)
    positions.append(uart_received_str.find("1.8.2(")+6)

    positions.append(uart_received_str.find("32.7(")+5)
    positions.append(uart_received_str.find("31.7(")+5)
    positions.append(uart_received_str.find("52.7(")+5)
    positions.append(uart_received_str.find("51.7(")+5)
    positions.append(uart_received_str.find("72.7(")+5)
    positions.append(uart_received_str.find("71.7(")+5)

    return(positions)


## program starts here
led_onboard.off()
enable3v3_pin.off()

enable3v3_pin.on() # power on IR head
sleep(2) # make sure 3.3V power is stable

uart_received_str = uart_ir_e350(uart_ir,IR_SIMULATION)

# debug 
print ("UART_string:\n"+uart_received_str)

# find parameters
positions = list()
positions = find_positions(uart_received_str=uart_received_str,positions=positions)

values = list()
values.append(uart_received_str[positions[0]:positions[0]+10]) # HT
values.append(uart_received_str[positions[1]:positions[1]+10]) # NT

for i in range(2,8):
    values.append(uart_received_str[positions[i]:positions[i]+3])

print("1.8.1(NT) value: "+values[0])
print("1.8.2(HT) value: "+values[1])

print("Phase_1 value: "+values[2]+"*"+values[3])
print("Phase_2 value: "+values[4]+"*"+values[5])
print("Phase_3 value: "+values[6]+"*"+values[7])

val_watt_cons = str(float(val327)*float(317)+float(val527)*float(517)+float(val727)*float(717))

print("Watt consumption now: "+val_watt_cons)


transmit_str = val181+"|"+val182+"|"+val_watt_cons



##### end of IR trials

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

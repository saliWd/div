import network # type: ignore (this is a pylance ignore warning directive)
import urequests # type: ignore
from time import sleep
from machine import Pin, Timer, UART # type: ignore
# my own files
import my_config # type: ignore

# 0 is doing GET-communication
# 1 uses post. transmits a identifier, values as blob, sends to RX1.php
TX_INTERFACE_VERSION = 1 # integer (range 0 to 9), just increasing when there is a change on the transmitted value format 


def debug_print(DO_DEBUG_PRINT:bool, text:str):
    if(DO_DEBUG_PRINT):
        print(text)
    # otherwise just return

# define the toggle as function. Overkill for now but might be expanded later
def blink(timer):
    led_onboard.toggle()

def uart_ir_e350(uart_ir, IR_SIMULATION:bool):
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
        
    if ((uart_str_id == None) or (uart_str_values_0 == None) or (uart_str_values_1 == None)):
        return('uart communication did not work')
    else:
        return(uart_str_id.decode()+uart_str_values_0.decode()+uart_str_values_1.decode())

def find_positions(uart_received_str):
    positions = list()
    positions.append(uart_received_str.find("1.8.1(")+6) # returns -1 if not found
    positions.append(uart_received_str.find("1.8.2(")+6)    

    positions.append(uart_received_str.find("32.7(")+5)
    positions.append(uart_received_str.find("52.7(")+5)
    positions.append(uart_received_str.find("72.7(")+5)
    
    positions.append(uart_received_str.find("31.7(")+5)
    positions.append(uart_received_str.find("51.7(")+5)
    positions.append(uart_received_str.find("71.7(")+5)

    positions.append(min(positions) > 20) # all of them need to be bigger than 20. Otherwise returning false (find returns -1 but I add the length of the string)
    
    return(positions)

def print_values(DO_DEBUG_PRINT:bool, values:list, val_watt_cons:str):
    debug_print(DO_DEBUG_PRINT, "NT / HT values [kWh]: "+values[0]+", "+values[1])
    debug_print(DO_DEBUG_PRINT, "Phase1, Phase2, Phase3 values [V*A]: "+values[2]+"*"+values[5]+", "+values[3]+"*"+values[6]+", "+values[4]+"*"+values[7])
    debug_print(DO_DEBUG_PRINT, "Watt consumption now [W]: "+val_watt_cons)

def get_wlan_ok(WLAN_SIMULATION:bool, wlan):
    if(WLAN_SIMULATION):
        return(True)
    return(wlan.isconnected())

def wlan_connect(WLAN_SIMULATION:bool, wlan, tim, led_onboard):
    wlan_ok = get_wlan_ok(WLAN_SIMULATION=WLAN_SIMULATION, wlan=wlan)
    if(wlan_ok):
        return() # nothing to do
    else:
        tim.init(freq=4.0, mode=Timer.PERIODIC, callback=blink) # signals I'm searching for WLAN    
        while not wlan_ok:
            config_wlan = my_config.get_wlan_config() # stored in external file
            wlan.connect(config_wlan['ssid'], config_wlan['pw'])
            sleep(3)
            wlan_ok = get_wlan_ok(WLAN_SIMULATION=WLAN_SIMULATION, wlan=wlan)
            print("WLAN connected? "+str(wlan_ok)) # debug output

        # signals wlan connection is ok
        tim.deinit()
        led_onboard.on()

def send_message_and_wait(WLAN_SIMULATION:bool, message:str, wait_time:int, led_onboard):
    if(not WLAN_SIMULATION): # not sending anything in simulation
        response = urequests.post(message)    
        response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    sleep(wait_time)  # in seconds. Do not set it below ~3 to limit the number of requests
    led_onboard.toggle()
    
def urlencode(dictionary:dict):
    urlenc = ""
    for key, val in dictionary.items():
        urlenc += "%s=%s&" %(key,val)
    urlenc = urlenc[:-1] # gets me something like 'val0=23&val1=bla space'
    return(urlenc)

def send_message_and_wait_post(WLAN_SIMULATION:bool, message:dict, wait_time:int, led_onboard, TX_INTERFACE_VERSION:int):
    if(not WLAN_SIMULATION): # not sending anything in simulation
        URL = "https://widmedia.ch/wmeter/getRX1.php?TX=pico&TXVER="+str(TX_INTERFACE_VERSION)
        HEADERS = {'Content-Type':'application/x-www-form-urlencoded'}

        urlenc = urlencode(message)
        response = urequests.post(URL, data=urlenc, headers=HEADERS)
        debug_print(DO_DEBUG_PRINT, "Text:"+response.text)
        response.close() # this is needed, I'm getting outOfMemory exception otherwise after 4 loops
    sleep(wait_time)  # in seconds
    led_onboard.toggle() # signal success


# constants
LENGTHS = [10,10,3,3,3,6,6,6] # HT, NT, 3 x voltages, 3 x currents
# debug stuff
DO_DEBUG_PRINT = my_config.get_debug_print()
IR_SIMULATION = my_config.get_ir_simulation()
WLAN_SIMULATION = my_config.get_wlan_simulation()

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
    sleep(2) # make sure 3.3V power is stable
    uart_received_str = uart_ir_e350(uart_ir,IR_SIMULATION) # this takes some seconds
    # print(uart_received_str)
    enable3v3_pin.off() # power down IR head

    # find parameters
    positions = find_positions(uart_received_str=uart_received_str)
    if (not positions[8]): # one of the finds did not work. Doesn't make sense to continue in this while loop
        print('Warning: did not find the values in the IR answer')
        sleep(10)
        continue

    values = list()
    for i in range(0,8):        
        values.append(uart_received_str[positions[i]:positions[i]+LENGTHS[i]])

    # TODO: the calculation below is not correct. Not sure what the reported current value (in mA) relates to, simple P = U * I does not work (Scheinleistung/Wirkleistung)
    val_watt_cons = str(float(values[2])*float(values[5])+float(values[3])*float(values[6])+float(values[4])*float(values[7]))
    print_values(DO_DEBUG_PRINT=DO_DEBUG_PRINT, values=values, val_watt_cons=val_watt_cons)

    transmit_str = values[0]+"_"+values[1]+"_"+val_watt_cons # TODO: rather transmit the whole readout and have the string logic on the server
    message = dict([('device',my_config.get_device_name()),('val',transmit_str)])
    debug_print(DO_DEBUG_PRINT, str(message))
    
    wlan_connect(WLAN_SIMULATION=WLAN_SIMULATION, wlan=wlan, tim=tim, led_onboard=led_onboard) # try to connect to the WLAN. Hangs there if no connection can be made

    send_message_and_wait_post(WLAN_SIMULATION=WLAN_SIMULATION, message=message, wait_time=10, led_onboard=led_onboard, TX_INTERFACE_VERSION=TX_INTERFACE_VERSION) # does not send anything when in simulation
# end while

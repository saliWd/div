from machine import sleep, deepsleep # type: ignore

### does not work. Somehow there is no DEEPSLEEP_RESET constant in machine
# check if the device woke from a deep sleep
# if machine.reset_cause() == machine.DEEPSLEEP_RESET:
#    print('woke from a deep sleep')

# pins
# led_onboard = Pin("LED", Pin.OUT)

sleep(20) # to be able to stop the program before deepsleep destroys USB connection

## program starts here
# led_onboard.off()

while True:
    print('while loop, before deep sleep')
    sleep(1) # to receive above output
    
    # put the device to sleep for 10 seconds
    deepsleep(10000) # NB: connection to whatever device is getting lost. complicates debugging

    print('while loop, after deep sleep') # should never reach this point
    sleep(1) # to receive above output
# end while

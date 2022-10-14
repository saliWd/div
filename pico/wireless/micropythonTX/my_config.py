# returns the ssid and the password of the WLAN connection
def get_wlan_config():
    config_wlan = dict([('ssid','widmedia_mobile'),('pw','publicPassword')])
    return(config_wlan)

# device name must not be more than 8 characters (stored in db)
def get_device_name():
    return('austr10')

# debug settings
def get_debug_print():
    return(True)

def get_ir_simulation():
    return(True)

def get_wlan_simulation():
    return(True)

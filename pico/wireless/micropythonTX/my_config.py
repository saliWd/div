# returns the ssid and the password of the WLAN connection
def config_get_wlan():
    config_wlan = dict([('ssid','widmedia_mobile'),('pw','publicPassword')])
    return(config_wlan)

# device name must not be more than 8 characters (stored in db)
def config_get_device_name():
    return('austr10')

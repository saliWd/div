# returns the ssid and the password of the WLAN connection
def get_wlan_config():
    config_wlan = dict([
        ("ssid","widmedia_mobile"),
        ("pw","publicPassword")
        ])
    return(config_wlan)

# device name must not be more than 8 characters (stored in db)
def get_device_name():
    return("austr10")

# debug settings
def get_debug_settings():
    debug_settings = dict([
        ("print",True),
        ("ir_sim",True),
        ("wlan_sim",True),
        ("sleep",True)
    ])
    return(debug_settings)

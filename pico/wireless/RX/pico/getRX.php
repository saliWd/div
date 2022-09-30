<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/pico/getRX.php?TX=pico&device=home&value0=27"
    // setting then some db variable to the value 27. TX must be pico and value0 must be a number (smaller than some limit)

    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in pico_w.sql-file


    if (isset($_GET['TX'])) { // only do something if this is set
        $getTXsafe = safeStrFromExt('GET','TX', 4); // length-limited variable, HTML encoded
        if ($getTXsafe === 'pico') {

            $safeInt = safeIntFromExt('GET', 'value0', 11);
            if ($safeInt !== 0) {  // 0 value is considered invalid
                $deviceName = 'unknown';
                $unsafeDevice = safeStrFromExt('GET','device', 8); // maximum length of 8
                // TODO: have the known-device-names as a constant array or something similar
                if (($unsafeDevice ==='home') or ($unsafeDevice === 'work')) {
                    $deviceName = $unsafeDevice; // otherwise, just leave the device name (backwards compatibility)
                }

                if ($result = $dbConn->query('INSERT INTO `pico_w` (`device`, `value0`) VALUES ("'.$deviceName.'", "'.$safeInt.'")')) {
                    echo 'inserting ok'; // no real html, just for debugging
                } else { 
                    echo 'error when inserting'; // no real html, just for debugging
                }
            }
        }
    }
?>
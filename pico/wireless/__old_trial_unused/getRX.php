<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/wmeter/getRX.php?TX=pico&device=austr10&val=3456.0_46468465.84_348.28"
    // split up the val into 3 floats and store in the db. TX must be pico

    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in pico_w.sql-file


    if (isset($_GET['TX'])) { // only do something if this is set
        $getTXsafe = safeStrFromExt('GET','TX', 4); // length-limited variable, HTML encoded
        if ($getTXsafe === 'pico') {
            $deviceName = 'unknown';
            $unsafeDevice = safeStrFromExt('GET','device', 8); // maximum length of 8
            // TODO: have the known-device-names as a constant array or something similar
            if (($unsafeDevice ==='austr10') or ($unsafeDevice === 'austr8')) {
                $deviceName = $unsafeDevice; // otherwise, just leave the device name (backwards compatibility)
            }
            $safeStr = safeStrFromExt('GET', 'val', 63); // don't know how long exactly
            if (strlen($safeStr) > 0) {  // 0 value is considered invalid
                $values = explode("_", $safeStr, 3); // max of 3 values
                $nt = floatval($values[0]);
                $ht = floatval($values[1]);
                $watt = floatval($values[2]);

                if ($result = $dbConn->query('INSERT INTO `pico_w` (`device`, `nt`, `ht`, `watt`) VALUES ("'.$deviceName.'", "'.$nt.'", "'.$ht.'", "'.$watt.'")')) {
                    echo 'inserting ok'; // no real html, just for debugging
                } else { 
                    echo 'error when inserting'; // no real html, just for debugging
                }
            }
        }
    }
?>
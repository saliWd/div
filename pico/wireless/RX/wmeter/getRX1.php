<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/wmeter/getRX1.php?TX=pico&TXVER=1"
    // POST data, url encoded

    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in wmeter.sql-file
    if (isset($_GET['TX'])) { // only do something if this is set
        $getTXsafe = safeStrFromExt('GET','TX', 4); // length-limited variable, HTML encoded
        if ($getTXsafe === 'pico') {
            $getTXVERsafe = safeIntFromExt('GET','TXVER', 1);
            if ($getTXVERsafe === 1) { // don't accept other interface version numbers
                // now I can look the post variables
                $unsafeDevice = safeStrFromExt('POST','device', 8); // maximum length of 8
                // TODO: have the known-device-names from DB
                if (($unsafeDevice ==='austr10') or ($unsafeDevice === 'austr8')) {
                    $deviceName = $unsafeDevice;
                    $unsafe_ir_answer = safeStrFromExt('POST','ir_answer', 511); // that one is not really safe
                    
                    // I expect it to have about 406 bytes
                    // TODO: make it sql safe
                    if ($result = $dbConn->query('INSERT INTO `wmeter` (`device`, `ir_answer`) VALUES ("'.$deviceName.'", "'.$unsafe_ir_answer.'")')) {
                        echo 'inserting ok'; // no real html, just for debugging
                    } else { 
                        echo 'error when inserting'; // no real html, just for debugging
                    }
                }
            }
        }
    }
    
?>
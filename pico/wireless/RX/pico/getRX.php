<?php declare(strict_types=1);
// expecting a call like "https://widmedia.ch/pico/getRX.php?TX=pico&device=home&value0=27"
// setting then some db variable to the value 27. TX must be pico and value0 must be a number (smaller than some limit)

// no visible (=HTML) output is generated. Use index.php to monitor the value itself
// db structure is stored in pico_w.sql-file

require_once('dbConn.php'); // this will return the $dbConn variable as 'new mysqli'
if ($dbConn->connect_error) {
    die('Connection to the data base failed. Please try again later and/or send me an email: sali@widmedia.ch');
}
$dbConn->set_charset('utf8');


if (isset($_GET['TX'])) { // only do something if this is set
    $getTXsafe = htmlentities(substr($_GET['TX'], 0, 4)); // length-limited variable, HTML encoded
    if ($getTXsafe === 'pico') {
        $unsafeInt = filter_var(substr($_GET['value0'], 0, 11), FILTER_SANITIZE_NUMBER_INT); // sanitize a length-limited variable
        if (filter_var($unsafeInt, FILTER_VALIDATE_INT)) { 
            $safeInt = (int)$unsafeInt;
            // now I can do something

            $deviceName = 'unknown';
            $unsafeDevice = htmlentities(substr($_GET['device'], 0, 8)); // maximum length of 8
            // TODO: have the known-device-names as a constant array or something similar
            if (($unsafeDevice ==='home') or ($unsafeDevice === 'work')) {
                $deviceName = $unsafeDevice; // otherwise, just leave the device name
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
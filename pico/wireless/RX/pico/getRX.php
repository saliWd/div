<?php 
// expecting a call like "https://widmedia.ch/pico/getRX.php?TX=pico&value0=27"
// setting then some db variable to the value 27. TX must be pico and value0 must be a number (smaller than some limit)

// no visible (=HTML) output is generated. Use index.php to monitor the value itself

require_once('../start/php/dbConn.php'); // this will return the $dbConn variable as 'new mysqli'
if ($dbConn->connect_error) {
    printErrorAndDie('Connection to the data base failed', 'Please try again later and/or send me an email: sali@widmedia.ch');
}
$dbConn->set_charset('utf8');
  


?>
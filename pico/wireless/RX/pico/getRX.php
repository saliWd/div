<?php 
// expecting a call like "https://widmedia.ch/pico/getRX.php?TX=pico&value0=27"
// setting then some db variable to the value 27. TX must be pico and value0 must be a number (smaller than some limit)

// no visible (=HTML) output is generated. Use index.php to monitor the value itself
// db structure:
// INSERT INTO `pico_w` (`id`, `value0`, `date`) 
//               VALUES (NULL, '55',      current_timestamp()); 

require_once('dbConn.php'); // this will return the $dbConn variable as 'new mysqli'
if ($dbConn->connect_error) {
    die('Connection to the data base failed. Please try again later and/or send me an email: sali@widmedia.ch');
}
$dbConn->set_charset('utf8');


if (isset($_GET['TX'])) { // only do something if this is set
    $getTXsafe = htmlentities(substr($_GET['TX'], 0, 4)); // length-limited variable, HTML encoded
    if ($getTXsafe == 'pico') {
        $unsafeInt = filter_var(substr($_GET['value0'], 0, 11), FILTER_SANITIZE_NUMBER_INT); // sanitize a length-limited variable
        if (filter_var($unsafeInt, FILTER_VALIDATE_INT)) { 
            $safeInt = (int)$unsafeInt;
            // now I can do something

            $result = $dbConn->query('SELECT `id`, `value0` FROM `pico_w` WHERE 1 ORDER BY `id` DESC');
            $numRows = $result->num_rows;
            if ($numRows == 0) { // no content yet, insert one row
                if (!($result = $dbConn->query('INSERT INTO `pico_w` (`value0`) VALUES ("'.$safeInt.'")'))) {
                    echo 'error when inserting';
                } else { 
                    echo 'inserting ok'; 
                }
            } else { // already some content in the db, update it
                $row = $result->fetch_assoc();
                $id = (int)$row['id'];
                if (!($result = $dbConn->query('UPDATE `pico_w` SET `value0` = "'.$safeInt.'", `date` = current_timestamp() WHERE `id` = "'.$id.'"'))) { 
                    echo 'error when updating';
                } else { 
                    echo 'updating ok'; 
                }
            } // numrows == 0            
        }
    }
}
?>
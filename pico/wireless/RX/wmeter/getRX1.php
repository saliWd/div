<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/wmeter/getRX1.php?TX=pico&TXVER=1"
    // POST data, JSON encoded: data = {'val_0': [ 'this is a test string' ]}

    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in wmeter.sql-file

    echo 'received data
    '; 
    echo 'POST:
    ';
    print_r($_POST);
    echo 'GET:
    ';
    print_r($_GET);
    //echo 'JSON decoded:';
    //var_dump(json_decode($_POST[0]));

?>
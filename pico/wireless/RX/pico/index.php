<!DOCTYPE html><html><head>
<meta charset="utf-8" />
<title>pico_w counter status auto refresh</title>
<meta name="description" content="a page displaying the value of the pico_w counter" />  
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta http-equiv="refresh" content="10">
<link rel="stylesheet" href="css/font.css" type="text/css" />
<link rel="stylesheet" href="css/normalize.css" type="text/css" />
<link rel="stylesheet" href="css/skeleton.css" type="text/css" />
</head><body>
<div class="section noBottom">
<div class="container">    
<h3>pico wireless receiver</h3>
<p>&nbsp;</p>
<?php 
require_once('dbConn.php'); // this will return the $dbConn variable as 'new mysqli'
if ($dbConn->connect_error) {
    die('Connection to the data base failed. Please try again later and/or send me an email: sali@widmedia.ch');
}
$dbConn->set_charset('utf8');

// want the latest entry (usually, there is only one)
$result = $dbConn->query('SELECT `id`, `value0`, `date` FROM `pico_w` WHERE 1 ORDER BY `id` DESC LIMIT 1');
$row = $result->fetch_assoc();
$id = (int)$row['id'];
$value0 = (int)$row['value0'];
$date = $row['date'];

echo '<div class="row">
        <div class="four columns">id: '.$id.'</div>
        <div class="four columns">value: '.$value0.'</div>
        <div class="four columns">last change: '.$date.'</div>
      </div>';

echo '<div class="row twelve columns">&nbsp;</div>';
echo '<div class="row twelve columns">...refreshing every 10 seconds...</div>';
?>
</div></div></body></html>
<?php declare(strict_types=1); ?>
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
// TODO: add button to empty table: TRUNCATE `widmedia`.`pico_w`"
// truncate sets auto-increment back to 0

require_once('dbConn.php'); // this will return the $dbConn variable as 'new mysqli'
if ($dbConn->connect_error) {
    die('Connection to the data base failed. Please try again later and/or send me an email: sali@widmedia.ch');
}
$dbConn->set_charset('utf8');

// select all entries
$result = $dbConn->query('SELECT `id`, `device`, `value0`, `date` FROM `pico_w` WHERE 1 ORDER BY `id`');
while ($row = $result->fetch_assoc()) {
  $id = (int)$row['id'];  
  $value0 = (int)$row['value0'];  

  echo '<div class="row">
          <div class="three columns">id: '.$id.'</div>
          <div class="three columns">device: '.$row['device'].'</div>
          <div class="three columns">value: '.$value0.'</div>
          <div class="three columns">last change: '.$row['date'].'</div>
        </div>';
}
echo '<div class="row twelve columns">&nbsp;</div>';
echo '<div class="row twelve columns">...refreshing every 10 seconds...</div>';
?>
</div></div></body></html>
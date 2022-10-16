<?php declare(strict_types=1); 
require_once('functions.php');
$dbConn = initialize();

echo '<!DOCTYPE html><html><head>
<meta charset="utf-8" />
<title>Wmeter</title>
<meta name="description" content="a page displaying the smart meter value" />  
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta http-equiv="refresh" content="30; url=https://widmedia.ch/wmeter/">
<link rel="stylesheet" href="css/font.css" type="text/css" />
<link rel="stylesheet" href="css/normalize.css" type="text/css" />
<link rel="stylesheet" href="css/skeleton.css" type="text/css" />
</head><body>
<div class="section noBottom">
<div class="container">
<h3>Wmeter</h3>
<p>&nbsp;</p>';

$doSafe = safeIntFromExt('GET', 'do', 2); // this is an integer (range 1 to 99) or non-existing
if ($doSafe === 0) { // entry point of this site
  // select all entries
  $result = $dbConn->query('SELECT `id`, `device`, `nt`, `ht`, `watt`, `date` FROM `pico_w` WHERE 1 ORDER BY `id` DESC LIMIT 20');
  $resultCnt = $dbConn->query('SELECT COUNT(*) as `total` FROM `pico_w` WHERE 1'); // TODO: where device = something
  $rowTotal = $resultCnt->fetch_assoc(); // returns one row only
  
  echo '<div class="row">
          <div class="six columns">Insgesamt '.$rowTotal['total'].' Einträge</div>
          <div class="six columns"><div class="button"><a href="index.php?do=1">alle Einträge löschen</a></div></div>
        </div>';
  $onlyOnce = TRUE;
  while ($row = $result->fetch_assoc()) {
    $id = (int)$row['id'];  
    $nt = (float)$row['nt'];  
    $ht = (float)$row['ht'];  
    $watt = (float)$row['watt'];  

    if ($onlyOnce) {
      echo '<div class="row twelve columns"><hr>Letzte Wattmessung: '.$watt.' W<hr></div>';
      $onlyOnce = FALSE;
    }
    // TODO: mix of classes and style in the divs below is ugly
    echo '<div class="row">
            <div class="six columns" style="text-align: left;">id: '.$id.'<br>device: '.$row['device'].'<br>update: '.$row['date'].'</div>
            <div class="six columns" style="text-align: left;">Aktueller Verbrauch: '.$watt.' W<br>Niedertarif: '.$nt.' kWh<br>Hochtarif: '.$ht.' kWh</div>
          </div>';
  } // while
  echo '<div class="row twelve columns">...diese Seite wird alle 30 Sekunden neu geladen...</div>';
} elseif ($doSafe === 1) { // delete all entries, then go back to default page
  $result = $dbConn->query('DELETE FROM `pico_w` WHERE 1'); // `device` = "home"); // MAYBE: want to delete only some things
  if ($result) {
    echo '<div class="row twelve columns">...alle Einträge gelöscht. <a href="index.php">zurück</a>...</div>';
  } else {
    echo '<div class="row twelve columns">...something went wrong when deleting all entries...</div>';
  }
} else { // should never happen
  echo '<div class="row twelve columns">...something went wrong (undefined do-variable)...</div>';
}
?>
</div></div></body></html>

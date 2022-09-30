<?php declare(strict_types=1); 
require_once('functions.php');
$dbConn = initialize();

echo '<!DOCTYPE html><html><head>
<meta charset="utf-8" />
<title>pico_w Empfänger</title>
<meta name="description" content="a page displaying the value of the pico_w counter" />  
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta http-equiv="refresh" content="10; url=https://widmedia.ch/pico/">
<link rel="stylesheet" href="css/font.css" type="text/css" />
<link rel="stylesheet" href="css/normalize.css" type="text/css" />
<link rel="stylesheet" href="css/skeleton.css" type="text/css" />
</head><body>
<div class="section noBottom">
<div class="container">
<h3>pico wireless: Empfänger</h3>
<p>&nbsp;</p>';

$doSafe = safeIntFromExt('GET', 'do', 2); // this is an integer (range 1 to 99) or non-existing
if ($doSafe === 0) { // entry point of this site
  // select all entries
  $result = $dbConn->query('SELECT `id`, `device`, `value0`, `date` FROM `pico_w` WHERE 1 ORDER BY `id`');
  $rowCnt = $result->num_rows;
  
  echo '<div class="row">
          <div class="six columns">Insgesamt '.$rowCnt.' Einträge</div>
          <div class="six columns"><div class="button"><a href="index.php?do=1">alle Einträge löschen</a></div></div>
        </div>';
  while ($row = $result->fetch_assoc()) {
    $id = (int)$row['id'];  
    $value0 = (int)$row['value0'];  

    echo '<div class="row">
            <div class="three columns">id: '.$id.'</div>
            <div class="three columns">device: '.$row['device'].'</div>
            <div class="three columns">value: '.$value0.'</div>
            <div class="three columns">update: '.$row['date'].'</div>
          </div>';
  } // while
  echo '<div class="row twelve columns">...diese Seite wird alle 10 Sekunden neu geladen...</div>';
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

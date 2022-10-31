<?php declare(strict_types=1); 
require_once('functions.php');
$dbConn = initialize();

function printBeginOfPage_settings():void {
  echo '<!DOCTYPE html><html><head>
  <meta charset="utf-8" />
  <title>Wmeter Settings</title>
  <meta name="description" content="a page displaying the smart meter value" />  
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="css/font.css" type="text/css" />
  <link rel="stylesheet" href="css/skeleton.css" type="text/css" />
  </head><body>';
  printNavMenu(getCurrentSite());
  echo '
  <div class="section noBottom">
  <div class="container">
  <h3>Wmeter Settings</h3>
  <p>&nbsp;</p>
  <hr>';
  return;
}



$doSafe = safeIntFromExt('GET', 'do', 2); // this is an integer (range 1 to 99) or non-existing
// do = 0: entry point
// do = 1: delete all entries in DB
$device = 'austr10'; // TODO: device as variable

if ($doSafe === 0) { // entry point of this site
    printBeginOfPage_settings();
    echo '          
        <div class="row twelve columns">
            <div class="button"><a href="settings.php?do=1">alle Einträge löschen</a></div>
        </div>';
} elseif ($doSafe === 1) { // delete all entries, then go back to default page
  printBeginOfPage_index(FALSE);
  $result = $dbConn->query('DELETE FROM `wmeter` WHERE `device` = "'.$device.'"');
  if ($result) {
    echo '<div class="row twelve columns">...alle Einträge gelöscht. <a href="index.php">zurück</a>...</div>';
  } else {
    echo '<div class="row twelve columns">...something went wrong when deleting all entries...</div>';
  }
} else { // should never happen
  echo '<div class="row twelve columns">...something went wrong (undefined do-variable)...</div>';
}
?>
<div class="row twelve columns">&nbsp;</div>
</div></div></body></html>
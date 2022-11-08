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
// do = 2: data thinning for older entries (TODO: outsource this to a CRON-job like behaviour)
$device = 'austr10'; // TODO: device as variable

if ($doSafe === 0) { // entry point of this site
    printBeginOfPage_settings();
    echo '          
        <div class="row twelve columns">
            <div class="button"><a href="settings.php?do=1">alle Einträge löschen</a></div>
        </div>';
} elseif ($doSafe === 1) { // delete all entries, then go back to default page
  printBeginOfPage_settings();
  $result = $dbConn->query('DELETE FROM `wmeter` WHERE `device` = "'.$device.'"');
  if ($result) {
    echo '<div class="row twelve columns">...alle Einträge gelöscht. <a href="index.php">zurück</a>...</div>';
  } else {
    echo '<div class="row twelve columns">...something went wrong when deleting all entries...</div>';
  }
} elseif ($doSafe === 2) { // data thinning for older entries
  // TODO: do this in a loop
  
  printBeginOfPage_settings();
  $sqlWhereDeviceThin = 'device = "'.$device.'" AND thin = "0"';
  $sql = 'SELECT `date` FROM `wmeter` WHERE '.$sqlWhereDeviceThin.' ORDER BY `id` ASC LIMIT 1;';
  $result = $dbConn->query($sql);
  $row = $result->fetch_assoc();
  // get the time, add 15 minutes  
  $dateToThin = date_create($row['date']);
  $dateToThin->modify('+ 15 minutes');
  $dateToThinString = $dateToThin->format('Y-m-d H:i:s');
  
  $dateMinus24h = date_create("yesterday");
  if ($dateToThin < $dateMinus24h) {  // if this time is more then 24h old, proceed
     // get the last one where thinning was not yet applied
    $sql = 'SELECT `id` FROM `wmeter` WHERE '.$sqlWhereDeviceThin.' AND `date` < "'.$dateToThinString.'" ORDER BY `id` ASC LIMIT 1;';
    $result = $dbConn->query($sql);
    $row = $result->fetch_assoc();   // -> gets me the ID I want to update with the next commands
    $idToUpdate = $row['id'];
    
    $sql = 'SELECT SUM(`aveConsDiff`) as `sumAveConsDiff`, SUM(`aveDateDiff`) as `sumAveDateDiff` FROM `wmeter` WHERE '.$sqlWhereDeviceThin.' AND `date` < "'.$dateToThinString.'";';
    $result = $dbConn->query($sql);
    $row = $result->fetch_assoc(); 

    // now do the update and then delete the others
    $sql = 'UPDATE `wmeter` SET `aveConsDiff` = "'.$row['sumAveConsDiff'].'", `aveDateDiff` = "'.$row['sumAveDateDiff'].'", `thin` = 1 WHERE `id` = "'.$idToUpdate.'";';
    $result = $dbConn->query($sql);
    // echo '<br>'.$sql.'<br>';

    $sql = 'DELETE FROM `wmeter` WHERE '.$sqlWhereDeviceThin.' AND `date` < "'.$dateToThinString.'";';
    $result = $dbConn->query($sql);
    echo '<br>'.$dbConn->affected_rows.' Einträge wurden gelöscht<br>';
  }
 
} else { // should never happen
  echo '<div class="row twelve columns">...something went wrong (undefined do-variable)...</div>';
}
?>
<div class="row twelve columns">&nbsp;</div>
</div></div></body></html>

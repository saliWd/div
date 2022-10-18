<?php declare(strict_types=1); 
require_once('functions.php');
$dbConn = initialize();

function printBeginOfPage_index(bool $enableAutoload):void {
  echo '<!DOCTYPE html><html><head>
  <meta charset="utf-8" />
  <title>Wmeter</title>
  <meta name="description" content="a page displaying the smart meter value" />  
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="css/font.css" type="text/css" />
  <link rel="stylesheet" href="css/normalize.css" type="text/css" />
  <link rel="stylesheet" href="css/skeleton.css" type="text/css" />
  <script src="script/chart.min.js"></script>';
  if ($enableAutoload) {
    echo '<meta http-equiv="refresh" content="40; url=https://widmedia.ch/wmeter/index.php?do=2">';
  }
  echo '
  </head><body>
  <div class="section noBottom">
  <div class="container">
  <h3>Wmeter</h3>
  <p>&nbsp;</p>';
  return;
}

$doSafe = safeIntFromExt('GET', 'do', 2); // this is an integer (range 1 to 99) or non-existing
// do = 0: entry point, display graph and stuff
// do = 1: delete all entries in DB
// do = 2: enable autoload
if (($doSafe === 0) or ($doSafe === 2)) { // entry point of this site
  printBeginOfPage_index(($doSafe === 2)); // autoload is enabled when do=2
  // TODO: SQL queries: use where device = something
  $QUERY_LIMIT = 5000; // TODO: check js-performance for a meaningful value
  $GRAPH_LIMIT = 3; // does not make sense to display a graph otherwise

  $result = $dbConn->query('SELECT `nt`, `ht`, `watt`, `date` FROM `pico_w` WHERE 1 ORDER BY `date` DESC LIMIT '.$QUERY_LIMIT);
  $queryCount = $result->num_rows; // this may be 0 ( = need to exclude from logic), or < graph-limit ( = display at least the newest) or >= graph-limit ( = all good)

  $resultCnt = $dbConn->query('SELECT COUNT(*) as `total` FROM `pico_w` WHERE 1');
  $rowTotal = $resultCnt->fetch_assoc(); // returns one row only
  
  echo '<div class="row">
          <div class="six columns">Insgesamt '.$rowTotal['total'].' Einträge</div>
          <div class="six columns"><div class="button"><a href="index.php">neu laden</a></div></div>
        </div>'; 

  if ($queryCount > 0) { // have at least one. Can display the newest
    $row_newest = $result->fetch_assoc();
    $nt_new = (float)$row_newest['nt'];  
    $ht_new = (float)$row_newest['ht'];  
    $watt_new = (float)$row_newest['watt'];
    $date_new = date_create($row_newest['date']);

    echo '<div class="row twelve columns"><hr>Letzte Wattmessung: '.$watt_new.' W um '.$date_new->format('Y-m-d H:i:s').'<hr></div>';

    if ($queryCount >= $GRAPH_LIMIT) {
      $axis_x = ''; // rightmost value comes first. Remove something again after the while loop
      $val_y0_nt = '';
      $val_y1_ht = '';
      $val_y2_wa = '';
      
      while ($row = $result->fetch_assoc()) { // did already fetch the newest one. At least 2 remaining
        $dateDiff = date_diff($date_new, date_create($row['date'])); // will be negative
        $dateMinutes = ($dateDiff->d * 24 * -60) - ($dateDiff->h * 60) - ($dateDiff->i); // values are negative

        $nt = (float)$row['nt'] - $nt_new; // will be 0 or negative
        $ht = (float)$row['ht'] - $ht_new;
        $watt = (float)$row['watt'];
        
        // revert the ordering
        $axis_x = $dateMinutes.', '.$axis_x; 
        $val_y0_nt = $nt.', '.$val_y0_nt;
        $val_y1_ht = $ht.', '.$val_y1_ht;
        $val_y2_wa = $watt.', '.$val_y2_wa;
      } // while 
      // remove the last two caracters (a comma-space) and add the brackets before and after
      $axis_x = '[ '.substr($axis_x, 0, -2).' ]';
      $val_y0_nt = '[ '.substr($val_y0_nt, 0, -2).' ]';
      $val_y1_ht = '[ '.substr($val_y1_ht, 0, -2).' ]';
      $val_y2_wa = '[ '.substr($val_y2_wa, 0, -2).' ]';
      
      // TODO: add some text about the absolute value (of kWh and date)

      echo '<div class="row twelve columns"><canvas id="myChart" width="600" height="300"></canvas></div>      
      <script>
      const ctx = document.getElementById("myChart");
      const labels = '.$axis_x.';
      const data = {
        labels: labels,
        datasets: [{
          label: "Niedertarif [kWh]",
          data: '.$val_y0_nt.',
          yAxisID: "yleft",
          backgroundColor: "rgb(255, 99, 132)",
          showLine: false
        },
        {
          label: "Hochtarif [kWh]",
          data: '.$val_y1_ht.',
          yAxisID: "yleft",
          backgroundColor: "rgb(132, 255, 99)",
          showLine: false
        },
        {
          label: "Aktueller Verbrauch [W]",
          data: '.$val_y2_wa.',
          yAxisID: "yright",
          backgroundColor: "rgb(25, 99, 132)",
          showLine: false
        }
      ],
      };
      const config = {
        type: "line",
        data: data,
        options: {
          scales: {
            x: { type: "linear", position: "bottom", title: { display: true, text: "Minuten" } },
            yleft: { type: "linear", position: "left", ticks: {color: "rgb(255, 99, 132)"} },
            yright: { type: "linear",  position: "right", ticks: {color: "rgb(25, 99, 132)"}, grid: {drawOnChartArea: false} }
          }
        }
      };
      const myChart = new Chart( document.getElementById("myChart"), config );
      </script>';
    } else {
      echo '<div class="row twelve columns"> - weniger als '.$GRAPH_LIMIT.' Einträge - </div>';
    }    
  } else {
    echo '<div class="row twelve columns"> - noch keine Einträge - </div>';
  }
  echo '<div class="row twelve columns"><hr><div class="button"><a href="index.php?do=1">alle Einträge löschen</a></div></div>';
} elseif ($doSafe === 1) { // delete all entries, then go back to default page
  printBeginOfPage_index(FALSE);
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

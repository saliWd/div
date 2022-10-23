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

  $result = $dbConn->query('SELECT `consumption`, `generation`, `date` FROM `wmeter` WHERE 1 ORDER BY `date` DESC LIMIT '.$QUERY_LIMIT);
  $queryCount = $result->num_rows; // this may be 0 ( = need to exclude from logic), or < graph-limit ( = display at least the newest) or >= graph-limit ( = all good)

  $resultCnt = $dbConn->query('SELECT COUNT(*) as `total` FROM `wmeter` WHERE 1');
  $rowTotal = $resultCnt->fetch_assoc(); // returns one row only
  
  echo '<div class="row">
          <div class="six columns">Insgesamt '.$rowTotal['total'].' Einträge</div>
          <div class="six columns"><div class="button"><a href="index.php">neu laden</a></div></div>
        </div>'; 

  if ($queryCount > 0) { // have at least one. Can display the newest
    $row_newest = $result->fetch_assoc();
    $consumption_new = $row_newest['consumption'];  
    $generation_new = $row_newest['generation'];
    $date_new = date_create($row_newest['date']);

    $date_minus_10mins = date_create($row_newest['date']);
    date_modify($date_minus_10mins, '- 10 minutes');
    $date_minus_60mins = date_create($row_newest['date']);
    date_modify($date_minus_60mins, '- 60 minutes');
    $FORMAT_STRING = 'Y-m-d H:i:s';

    $result_10mins = $dbConn->query('SELECT `consumption`, `generation`, `date` FROM `wmeter` WHERE `date` < \''.date_format($date_minus_10mins, $FORMAT_STRING).'\' ORDER BY `date` DESC LIMIT 1');
    $queryCount10mins = $result_10mins->num_rows; // this may be 0 or 1
    $result_60mins = $dbConn->query('SELECT `consumption`, `generation`, `date` FROM `wmeter` WHERE `date` < \''.date_format($date_minus_60mins, $FORMAT_STRING).'\' ORDER BY `date` DESC LIMIT 1');
    $queryCount60mins = $result_60mins->num_rows; // this may be 0 or 1

    
    $text_10mins = '(noch keine Messung)';
    if ($queryCount10mins === 1) {
      $row_10mins = $result_10mins->fetch_assoc(); // returns one row only
      $dateDiff_10min = date_diff(date_create($row_10mins['date']), $date_new); // it's not exactly 10 mins
      $dateSecs = ($dateDiff_10min->d * 24 * 3600) + ($dateDiff_10min->h*3600) + ($dateDiff_10min->i * 60) + ($dateDiff_10min->s);

      $valueDiff_10min = 3600 * 1000 * ($consumption_new - $row_10mins['consumption']); // difference is in kWh = 1000 * 3600 W
      $averageConsumption10mins = round($valueDiff_10min / $dateSecs, 2);
      $text_10mins = $averageConsumption10mins.' W (letzte 10 Minuten)';
    }
    
    $text_60mins = '';
    if ($queryCount60mins === 1) {
      $row_60mins = $result_60mins->fetch_assoc(); // returns one row only
      $dateDiff_60min = date_diff(date_create($row_60mins['date']), $date_new); // it's not exactly 60 mins
      $dateSecs = ($dateDiff_60min->d * 24 * 3600) + ($dateDiff_60min->h*3600) + ($dateDiff_60min->i * 60) + ($dateDiff_60min->s);

      $valueDiff_60min = 3600 * 1000 * ($consumption_new - $row_60mins['consumption']); // difference is in kWh = 1000 * 3600 W
      $averageConsumption60mins = round($valueDiff_60min / $dateSecs, 2);
      $text_60mins = ', '.$averageConsumption60mins.' W (letzte Stunde)';
    }


    echo '<div class="row twelve columns"><hr>&Oslash; Verbrauch: '.$text_10mins.$text_60mins.' Letzte Messung: '.$date_new->format('Y-m-d H:i:s').'<hr></div>';

    if ($queryCount >= $GRAPH_LIMIT) {
      $axis_x = ''; // rightmost value comes first. Remove something again after the while loop
      $val_y0_consumption = '';
      $val_y1_generation = '';
      
      while ($row = $result->fetch_assoc()) { // did already fetch the newest one. At least 2 remaining
        $dateDiff = date_diff($date_new, date_create($row['date'])); // will be negative
        $dateHours = ($dateDiff->d * -24) - ($dateDiff->h) - ($dateDiff->i / 60) - ($dateDiff->s / 3600); // values are negative

        $consumption = $row['consumption'] - $consumption_new; // will be 0 or negative
        $generation = $row['generation'] -  $generation_new;
        
        // revert the ordering
        $axis_x = $dateHours.', '.$axis_x; 
        $val_y0_consumption = $consumption.', '.$val_y0_consumption;
        $val_y1_generation = $generation.', '.$val_y1_generation;
      } // while 
      // remove the last two caracters (a comma-space) and add the brackets before and after
      $axis_x = '[ '.substr($axis_x, 0, -2).' ]';
      $val_y0_consumption = '[ '.substr($val_y0_consumption, 0, -2).' ]';
      $val_y1_generation = '[ '.substr($val_y1_generation, 0, -2).' ]';
      
      // TODO: add some text about the absolute value (of kWh and date)

      echo '<div class="row twelve columns"><canvas id="myChart" width="600" height="300"></canvas></div>      
      <script>
      const ctx = document.getElementById("myChart");
      const labels = '.$axis_x.';
      const data = {
        labels: labels,
        datasets: [{
          label: "Verbrauch [kWh]",
          data: '.$val_y0_consumption.',
          yAxisID: "yleft",
          backgroundColor: "rgb(255, 99, 132)",
          showLine: false
        },
        {
          label: "Einspeisung [kWh]",
          data: '.$val_y1_generation.',
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
            x: { type: "linear", position: "bottom", title: { display: true, text: "Stunden" } },
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
  $result = $dbConn->query('DELETE FROM `wmeter` WHERE 1'); // `device` = "home"); // MAYBE: want to delete only some things
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

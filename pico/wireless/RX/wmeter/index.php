<?php declare(strict_types=1); 
require_once('functions.php');
$dbConn = initialize();

function printBeginOfPage_index(bool $enableAutoReload):void {
  echo '<!DOCTYPE html><html><head>
  <meta charset="utf-8" />
  <title>Wmeter</title>
  <meta name="description" content="a page displaying the smart meter value" />  
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="css/font.css" type="text/css" />
  <link rel="stylesheet" href="css/normalize.css" type="text/css" />
  <link rel="stylesheet" href="css/skeleton.css" type="text/css" />
  <script src="script/chart.min.js"></script>
  <script src="script/moment.min.mine.js"></script>
  <script src="script/chartjs-adapter-moment.mine.js"></script>';
  if ($enableAutoReload) {
    echo '<meta http-equiv="refresh" content="40; url=https://widmedia.ch/wmeter/index.php?autoreload=1">';
  }
  echo '
  </head><body>
  <div class="section noBottom">
  <div class="container">
  <h3>Wmeter</h3>';
  return;
}

// returns the time range to be displayed as int. Possible values are: 1 (for last 1 hour), 6, 24, 25. 25 means: all data. Default is 24
function getTimeRange():int {
  $returnVal = 24;
  if (isset($_GET['submit_1h'])) { $returnVal = 1; }
  if (isset($_GET['submit_6h'])) { $returnVal = 6; }
  if (isset($_GET['submit_24h'])) { $returnVal = 24; }
  if (isset($_GET['submit_25h'])) { $returnVal = 25; } // this one has a special meaning: display all data
  return $returnVal;
}

$doSafe = safeIntFromExt('GET', 'do', 2); // this is an integer (range 1 to 99) or non-existing
// do = 0: entry point, display graph and stuff
// do = 1: delete all entries in DB
// do = 2: 
$autoreload = safeIntFromExt('GET', 'autoreload', 1);
$timeSelected = getTimeRange();
$enableAutoReload = ($autoreload === 1);
if ($doSafe === 0) { // entry point of this site
  printBeginOfPage_index($enableAutoReload); // refreshes the page every x seconds
  $QUERY_LIMIT = 5000; // TODO: check js-performance for a meaningful value
  $GRAPH_LIMIT = 3; // does not make sense to display a graph otherwise

  $sql = 'SELECT `consumption`, `date`, '; // TODO: maybe add generation
  $sql = $sql.'avg(`consDiff`) OVER(ORDER BY `date` DESC ROWS BETWEEN 5 PRECEDING AND CURRENT ROW ) as `movAveConsDiff`, ';
  $sql = $sql.'avg(`dateDiff`) OVER(ORDER BY `date` DESC ROWS BETWEEN 5 PRECEDING AND CURRENT ROW ) as `movAveDateDiff` ';
  $sql = $sql.'from `wmeter` WHERE `device` = "austr10" '; // TODO: device as variable
  $sql = $sql.'ORDER BY `date` DESC LIMIT '.$QUERY_LIMIT.';';
  
  $result = $dbConn->query($sql);
  $queryCount = $result->num_rows; // this may be 0 ( = need to exclude from logic), or < graph-limit ( = display at least the newest) or >= graph-limit ( = all good)

  if ($queryCount > 0) { // have at least one. Can display the newest
    $row_newest = $result->fetch_assoc();
    $consumption_new = $row_newest['consumption'];
    if ($row_newest['movAveDateDiff'] > 0) { // divide by 0 exception
        $newestConsumption = round($row_newest['movAveConsDiff']*3600*1000 / $row_newest['movAveDateDiff']); // kWh compared to seconds
    } else {$newestConsumption = 0.0; }
    $date_new = date_create($row_newest['date']);

    $dateString = 'um '.$date_new->format('Y-m-d H:i:s');
    if (date('Y-m-d') === $date_new->format('Y-m-d')) { // same day
      $dateString = 'heute um '.$date_new->format('H:i:s');  
    }
    echo '<div class="row twelve columns"><hr>Verbrauch: <b>'.$newestConsumption.'W</b> '.$dateString.'<hr></div>';

    if ($queryCount >= $GRAPH_LIMIT) {
      $axis_x = ''; // rightmost value comes first. Remove something again after the while loop
      $val_y0_consumption = '';
      $val_y1_watt = '';
      
      while ($row = $result->fetch_assoc()) { // did already fetch the newest one. At least 2 remaining  
        $consumption = $row['consumption'] - $consumption_new; // will be 0 or negative
        if ($row['movAveDateDiff'] > 0) { // divide by 0 exception
          $watt = max(round($row['movAveConsDiff']*3600*1000 / $row['movAveDateDiff']), 1.0); // max(val,1.0) because 0 in log will not be displayed correctly
        } else { $watt = 0; }
        
        // revert the ordering
        $axis_x = 'new Date("'.$row['date'].'"), '.$axis_x; // new Date("2020-03-01 12:00:12")
        $val_y0_consumption = $consumption.', '.$val_y0_consumption;
        $val_y1_watt = $watt.', '.$val_y1_watt;
      } // while 
      // remove the last two caracters (a comma-space) and add the brackets before and after
      $axis_x = '[ '.substr($axis_x, 0, -2).' ]';
      $val_y0_consumption = '[ '.substr($val_y0_consumption, 0, -2).' ]';
      $val_y1_watt = '[ '.substr($val_y1_watt, 0, -2).' ]';
      
      // TODO: add some text about the absolute value (of kWh)

      echo '<div class="row twelve columns"><canvas id="myChart" width="600" height="300"></canvas></div>      
      <script>
      const ctx = document.getElementById("myChart");
      const labels = '.$axis_x.';
      const data = {
        labels: labels,
        datasets: [{
          label: "Verbrauch [W]",
          data: '.$val_y1_watt.',
          yAxisID: "yleft",
          backgroundColor: "rgb(25, 99, 132)",
          showLine: false
        },
        {
          label: "Verbrauch total [kWh]",
          data: '.$val_y0_consumption.',
          yAxisID: "yright",
          backgroundColor: "rgba(255, 99, 132, 0.4)",
          showLine: false
        }
      ],
      };
      const config = {
        type: "line",
        data: data,
        options: {
          scales: {
            '; 
            // TODO: depending on the range, I need to specify the unit
            echo 'x: { type: "time", 
              time: {
                unit: "hour"
              }
            },';
            echo '
            yleft: { type: "logarithmic", position: "left", ticks: {color: "rgb(25, 99, 132)"} },
            yright: { type: "linear",  position: "right", ticks: {color: "rgba(255, 99, 132, 0.8)"}, grid: {drawOnChartArea: false} }
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

  $resultCnt = $dbConn->query('SELECT COUNT(*) as `total` FROM `wmeter` WHERE `device` = "austr10";');
  $rowTotal = $resultCnt->fetch_assoc(); // returns one row only
  $checkedText = '';
  if($enableAutoReload) {
    $checkedText = ' checked';
  }
  
  // TODO: add icons, change design
  $submitTexts = array (
    '1' => array('1h','1 h',''),
    '6' => array('6h','6 h',''),
    '24' => array('24h','24 h',''),
    '25' => array('25h','alles','')
  );
  $submitTexts[$timeSelected][2]  = ' class="active"'; // highlight the selected one
  
  echo '<div class="row twelve columns"><form action="index.php" method="get"><input type="checkbox" id="autoreload" name="autoreload" value="1"'.$checkedText.'> auto reload ';
          foreach ($submitTexts as $submitText) {
            echo '<input name="submit_'.$submitText[0].'" type="submit" id="submit_'.$submitText[0].'" value="'.$submitText[1].'"'.$submitText[2].'> ';
          }
          echo '</form></div>
          <div class="row">
            <div class="six columns">Insgesamt '.$rowTotal['total'].' Einträge</div>
            <div class="six columns"><div class="button"><a href="index.php?do=1">alle Einträge löschen</a></div></div>
          </div>';
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
<div class="row twelve columns">&nbsp;</div>
</div></div></body></html>

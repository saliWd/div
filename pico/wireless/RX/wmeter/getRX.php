<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/wmeter/getRX.php?TX=pico&TXVER=1"
    // with POST data (url encoded)

    // TODO: add an access key feature

    // checks the params retrieved over get and returns TRUE if they are ok
    function verifyGetParams (): bool {  
        if (isset($_GET['TX'])) { // only do something if this is set            
            if (safeStrFromExt('GET','TX', 4) === 'pico') {                
                if (safeIntFromExt('GET','TXVER', 1) === 1) { // don't accept other interface version numbers
                    return TRUE;
                }
            }
        }
        return FALSE;
    }
    
    // sql sanitation and length limitation
    function sqlSafeStrFromPost ($dbConn, string $varName, int $length): string {
        if (isset($_POST[$varName])) {
           return mysqli_real_escape_string($dbConn, (substr($_POST[$varName], 0, $length))); // length-limited variable           
        } else {
           return '';
        }
    }

    // I want to readout: total_consumption and total_generation. NB: phases do not help that much without cosphi                
    function get_interesting_values (string $haystack, string $param_consumption, string $param_generation): array {
        $return_array = array(FALSE, '',  ''); // valid/consumptio/generation
            $consumption_pos = strpos($haystack,$param_consumption);
            if ($consumption_pos) {
                $return_array[1] = substr($haystack,$consumption_pos+6,10); // I know it's 10 characters long and starts after the bracket
                $generation_pos = strpos($haystack,$param_generation); // search only if the first param has been found already
                if ($consumption_pos) {
                    $return_array[2] = substr($haystack,$generation_pos+6,10); // I know it's 10 characters long and starts after the bracket
                    $return_array[0] = TRUE; // only true if both values have been found
                }
            }
        return $return_array;
    }
    
    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in wmeter.sql-file
    if (verifyGetParams()) { // now I can look the post variables        
        $unsafeDevice = safeStrFromExt('POST','device', 8); // maximum length of 8        
        if (($unsafeDevice ==='austr10') or ($unsafeDevice === 'austr8')) { // TODO: have the known-device-names from DB
            $deviceName = $unsafeDevice;
            $sqlSafe_ir_answer = sqlSafeStrFromPost($dbConn, 'ir_answer', 511); // safe to insert into sql (not to output on html)
            // process the whole IR string
            // \x02F.F(00)                     
            // 0.0(          120858)
            // C.1.0(13647123)
            // C.1.1(        )
            // 1.8.1(042951.721*kWh)       total_nt_consumption
            // 1.8.2(018609.568*kWh)       total_ht_consumption
            // 2.8.1(000000.302*kWh)       total_nt_generation
            // 2.8.2(000010.188*kWh)       total_ht_generation
            // 1.8.0(061561.289*kWh)       total_consumption
            // 2.8.0(000010.490*kWh)       total_generation
            // 15.8.0(061571.780*kWh)      total_energy
            // C.7.0(0008)                 power_off_counter
            // 32.7(241*V)                 phase_0_volt
            // 52.7(243*V)                 phase_1_volt
            // 72.7(242*V)                 phase_2_volt
            // 31.7(000.35*A)              phase_0_amp
            // 51.7(000.52*A)              phase_1_amp
            // 71.7(000.47*A)              phase_2_amp
            // 82.8.1(0000)
            // 82.8.2(0000)
            // 0.2.0(M26)
            // C.5.0(0401)
            // !
            // x03\x01'

            // interested in those two params (unfortunately no 16.7 and no cosPhi param. So phase-values are just indicative)
            $values = get_interesting_values($sqlSafe_ir_answer, "1.8.0(", "2.8.0(");
            if ($values[0]) {
                $total_consumption = $values[1];
                $total_generation = $values[2];
                if ($result = $dbConn->query('INSERT INTO `wmeter` (`device`, `consumption`, `generation`) VALUES ("'.$deviceName.'", "'.$total_consumption.'", "'.$total_generation.'")')) {
                    echo 'inserting ok'; // no real html, just for debugging
                    //NB: not using last inserted ID as other inserts may have happened in the meantime
                    $result = $dbConn->query('SELECT * FROM `wmeter` WHERE `device` = "'.$deviceName.'" ORDER BY `id` DESC LIMIT 2');
                    $queryCount = $result->num_rows; // this may be 1 or 2
                    if ($queryCount === 2) {
                        $row_now = $result->fetch_assoc();
                        $row_before = $result->fetch_assoc();
                        $consDiff = $row_now['consumption'] - $row_before['consumption']; // 0 or positive
                        $genDiff = $row_now['generation'] - $row_before['generation']; // 0 or positive
                        $dateDiff = date_diff(date_create($row_before['date']), date_create($row_now['date']));
                        $dateSecs = ($dateDiff->d * 24 * 3600) + ($dateDiff->h*3600) + ($dateDiff->i * 60) + ($dateDiff->s);
                        if ($dateSecs < 9000) { // doesn't make sense otherwise, too long between measurements
                            if ($result = $dbConn->query('UPDATE `wmeter` SET `consDiff` = "'.$consDiff.'", `genDiff` = "'.$genDiff.'", `dateDiff` = "'.$dateSecs.'" WHERE `id` = "'.$row_now['id'].'"')) {
                                echo 'diff update ok';
                            } else {
                                echo 'error: diff upated nok';
                            }
                        } else {
                            echo 'previous data too old'; // not an error
                        } 
                    } else {
                        echo 'no previous data'; // not an error
                    }
                } else { 
                    echo 'error: when inserting';
                }
            } else {
                echo 'error: values not found';
            }
        } else { 
            echo 'error: device not supported'; 
        }
    }
    
?>
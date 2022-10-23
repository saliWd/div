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
            $consumption_pos = strpos($haystack,$param_consumption."(");
            if ($consumption_pos) {
                $return_array[1] = substr($haystack,$consumption_pos+6,10); // I know it's 10 characters long and starts after the bracket
                $generation_pos = strpos($haystack,$param_generation."("); // search only if the first param has been found already
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
            
            // interested in roughly those params (unfortunately no 16.7 and no cosPhi param. So phase-values are just indicative)
            $PARAM_CODES = array(
                "total_nt_consumption" => "1.8.1",
                "total_ht_consumption" => "1.8.2",
                "total_nt_generation" => "2.8.1",
                "total_ht_generation" => "2.8.2",
                "total_consumption" => "1.8.0",
                "total_generation" => "2.8.0",
                "total_energy" => "15.8.0",
                "power_off_counter" => "C.7.0",
                "phase_0_volt" => "32.7",
                "phase_1_volt" => "52.7",
                "phase_2_volt" => "72.7",
                "phase_0_amp" => "31.7",
                "phase_1_amp" => "51.7",
                "phase_2_amp" => "71.7"
            );
            $values = get_interesting_values($sqlSafe_ir_answer, $PARAM_CODES["total_consumption"], $PARAM_CODES["total_generation"]);
            if ($values[0]) {
                $total_consumption = $values[1];
                $total_generation = $values[2];
                if ($result = $dbConn->query('INSERT INTO `wmeter` (`device`, `consumption`, `generation`) VALUES ("'.$deviceName.'", "'.$total_consumption.'", "'.$total_generation.'")')) {
                    echo 'inserting ok'; // no real html, just for debugging
                } else { 
                    echo 'error: when inserting';
                }
            } else {
                echo 'error: values not found';
            }
            
            /* process the whole IR string
            \x02F.F(00)
            0.0(          120858)
            C.1.0(13647123)
            C.1.1(        )
            1.8.1(042951.721*kWh)
            1.8.2(018609.568*kWh)
            2.8.1(000000.302*kWh)
            2.8.2(000010.188*kWh)
            1.8.0(061561.289*kWh)
            2.8.0(000010.490*kWh)
            15.8.0(061571.780*kWh)
            C.7.0(0008)
            32.7(241*V)
            52.7(243*V)
            72.7(242*V)
            31.7(000.35*A)
            51.7(000.52*A)
            71.7(000.47*A)
            82.8.1(0000)
            82.8.2(0000)
            0.2.0(M26)
            C.5.0(0401)
            !
            x03\x01'
            */
        } else { 
            echo 'error: device not supported'; 
        }
    }
    
?>
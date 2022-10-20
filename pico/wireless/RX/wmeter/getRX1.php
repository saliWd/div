<?php declare(strict_types=1); 
    require_once('functions.php');
    $dbConn = initialize();
    // expecting a call like "https://widmedia.ch/wmeter/getRX1.php?TX=pico&TXVER=1"
    // with POST data (url encoded)

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
    
    function sqlSafeStrFromPost ($dbConn, string $varName, int $length): string {
        if (isset($_POST[$varName])) {
           return mysqli_real_escape_string($dbConn, (substr($_POST[$varName], 0, $length))); // length-limited variable           
        } else {
           return '';
        }
    }

    // no visible (=HTML) output is generated. Use index.php to monitor the value itself
    // db structure is stored in wmeter.sql-file
    if (verifyGetParams()) { // now I can look the post variables        
        $unsafeDevice = safeStrFromExt('POST','device', 8); // maximum length of 8        
        if (($unsafeDevice ==='austr10') or ($unsafeDevice === 'austr8')) { // TODO: have the known-device-names from DB
            $deviceName = $unsafeDevice;
            $sqlSafe_ir_answer = sqlSafeStrFromPost($dbConn, 'ir_answer', 511); // safe to insert into sql (not to output on html)
            
            // I expect it to have about 406 bytes (TODO: cut off some stuff in the TX already)           
            if ($result = $dbConn->query('INSERT INTO `wmeter` (`device`, `ir_answer`) VALUES ("'.$deviceName.'", "'.$sqlSafe_ir_answer.'")')) {
                echo 'inserting ok'; // no real html, just for debugging

                // process the whole IR string
                /*
                /LGZ4ZMF100AC.M26
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
                    "phase_2_amp" => "71.7");
                /*
                def find_positions(uart_received_str):
                    positions = list()
                    positions.append(uart_received_str.find("1.8.1(")+6) # returns -1 if not found
                    positions.append(uart_received_str.find("1.8.2(")+6)    

                    positions.append(uart_received_str.find("32.7(")+5)
                    positions.append(uart_received_str.find("52.7(")+5)
                    positions.append(uart_received_str.find("72.7(")+5)
                    
                    positions.append(uart_received_str.find("31.7(")+5)
                    positions.append(uart_received_str.find("51.7(")+5)
                    positions.append(uart_received_str.find("71.7(")+5)

                    positions.append(min(positions) > 20) # all of them need to be bigger than 20. Otherwise returning false (find returns -1 but I add the length of the string)
                    
                    return(positions)

                def print_values(DO_DEBUG_PRINT:bool, values:list, val_watt_cons:str):
                    debug_print(DO_DEBUG_PRINT, "NT / HT values [kWh]: "+values[0]+", "+values[1])
                    debug_print(DO_DEBUG_PRINT, "Phase1, Phase2, Phase3 values [V*A]: "+values[2]+"*"+values[5]+", "+values[3]+"*"+values[6]+", "+values[4]+"*"+values[7])
                    debug_print(DO_DEBUG_PRINT, "Watt consumption now [W]: "+val_watt_cons)
    
                LENGTHS = [10,10,3,3,3,6,6,6] # HT, NT, 3 x voltages, 3 x currents
                
                positions = find_positions(uart_received_str=uart_received_str)
                if (not positions[8]): # one of the finds did not work. Doesn't make sense to continue in this while loop
                    print('Warning: did not find the values in the IR answer')
                    sleep(10)
                    continue

                values = list()
                for i in range(0,8):        
                    values.append(uart_received_str[positions[i]:positions[i]+LENGTHS[i]])

                # TODO: the calculation below is not correct. Not sure what the reported current value (in mA) relates to, simple P = U * I does not work (Scheinleistung/Wirkleistung)
                val_watt_cons = str(float(values[2])*float(values[5])+float(values[3])*float(values[6])+float(values[4])*float(values[7]))
                print_values(DO_DEBUG_PRINT=DO_DEBUG_PRINT, values=values, val_watt_cons=val_watt_cons)

                transmit_str = values[0]+"_"+values[1]+"_"+val_watt_cons # TODO: rather transmit the whole readout and have the string logic on the server
                */

            } else { 
                echo 'error when inserting';
            }
        } echo 'error device not supported';
    }
    
?>
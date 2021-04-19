/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Ha Thach (tinyusb.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "bsp/board.h"
#include "tusb.h"

#include "usb_descriptors.hpp"
#include "icons.hpp"


#include "pico_display.hpp"
#include "font8_data.hpp"

//--------------------------------------------------------------------+
// MACRO CONSTANT TYPEDEF PROTYPES
//--------------------------------------------------------------------+

/* Blink pattern
 * - 250 ms  : device not mounted
 * - 1000 ms : device mounted
 * - 2500 ms : device is suspended
 */
enum {
    BLINK_NOT_MOUNTED = 250,
    BLINK_MOUNTED = 1000,
    BLINK_SUSPENDED = 2500,
};

static uint32_t blink_interval_ms = BLINK_NOT_MOUNTED;

using namespace pimoroni;

uint16_t buffer[PicoDisplay::WIDTH * PicoDisplay::HEIGHT];
PicoDisplay pico_display(buffer);
bool running = false;
bool move_mouse = false;
bool type_character = false;
bool mouse_enabled = true;
bool keyboard_enabled = false;

const uint16_t color_orange = pico_display.create_pen(255, 165, 0);
const uint16_t color_white = pico_display.create_pen(200, 200, 200);
const uint16_t color_darkwhite = pico_display.create_pen(100, 100, 100);
const uint16_t color_darkblue = pico_display.create_pen(10, 20, 30);
const uint16_t color_red = pico_display.create_pen(220, 30, 30);

void led_blinking_task(void);
void hid_task(bool move_mouse, bool type_character);
void update_gui(Rect rectBtnA, Rect rectBtnX, Rect rectBtnY, bool running, bool mouse_enabled, bool keyboard_enabled, PicoDisplay pico_display);
void initial_gui(PicoDisplay pico_display);
void icon_draw(icon_t icon, bool flip, uint16_t c, PicoDisplay pico_display);

/*------------- MAIN -------------*/
int main(void) {

    icon_mouse.bitmap = icon_mouse_bmp;
    icon_mouse.pos_x = 190;
    icon_mouse.pos_y = 18;
    icon_mouse.height = 34;// TODO does not work sizeof(icon_mouse.bitmap)/sizeof(icon_mouse.bitmap[0]);
    icon_kbd.bitmap = icon_kbd_bmp;
    icon_kbd.pos_x = 187;
    icon_kbd.pos_y = 98;
    icon_kbd.height = 20; // sizeof(icon_kbd.bitmap)/sizeof(icon_kbd.bitmap[0]);
    icon_x.bitmap = icon_x_bmp;
    // pos x and y vary
    icon_x.height = 20;
    

    board_init();
    tusb_init();
    srand(time(0));

    // setup the display
    pico_display.init(); // 240 x 135 pixel
    pico_display.set_backlight(150);
    pico_display.set_font(&font8);
    initial_gui(pico_display);
    
    Rect rectBtnA(20, 20, 170, 60); // on/off
    Rect rectBtnB(20, 97, 100, 30); // character select
    Rect rectBtnX(icon_mouse.pos_x, icon_mouse.pos_y, 32, icon_mouse.height);
    Rect rectBtnY(icon_kbd.pos_x, icon_kbd.pos_y, 32, icon_kbd.height);
    
    update_gui(rectBtnA, rectBtnX, rectBtnY, running, mouse_enabled, keyboard_enabled, pico_display);
    
    uint8_t which_button = 0;
    uint16_t debounce_cnt = 0;

    while (1) {
        if (pico_display.is_pressed(pico_display.A)) which_button = 1; // make sure I react only onto one button    
            else if (pico_display.is_pressed(pico_display.B)) which_button = 2;
            else if (pico_display.is_pressed(pico_display.X)) which_button = 3;
            else if (pico_display.is_pressed(pico_display.Y)) which_button = 4;
            else which_button = 0;

        if ((debounce_cnt == 0) && (which_button > 0)) { // do something. If not 0, just ignore the pressed button                
            if (which_button == 1) running = !running;
            if (which_button == 2) ; // TODO: functionality button B
            if (which_button == 3) mouse_enabled = !mouse_enabled; // button X
            if (which_button == 4) keyboard_enabled = !keyboard_enabled; // button Y

            update_gui(rectBtnA, rectBtnX, rectBtnY, running, mouse_enabled, keyboard_enabled, pico_display);
            move_mouse = running && mouse_enabled;
            type_character = running && keyboard_enabled;

            debounce_cnt = 500;
        }
        if (debounce_cnt > 0) {
            debounce_cnt--;
            busy_wait_us_32(1000);
        }
        tud_task(); // tinyusb device task
        hid_task(move_mouse, type_character);
    }
    return 0;
}

//--------------------------------------------------------------------+
// Device callbacks
//--------------------------------------------------------------------+

// Invoked when device is mounted
void tud_mount_cb(void) {
    blink_interval_ms = BLINK_MOUNTED;
}

// Invoked when device is unmounted
void tud_umount_cb(void) {
    blink_interval_ms = BLINK_NOT_MOUNTED;
}

// Invoked when usb bus is suspended
// remote_wakeup_en : if host allow us  to perform remote wakeup
// Within 7ms, device must draw an average of current less than 2.5 mA from bus
void tud_suspend_cb(bool remote_wakeup_en) {
    (void) remote_wakeup_en;
    blink_interval_ms = BLINK_SUSPENDED;
}

// Invoked when usb bus is resumed
void tud_resume_cb(void) {
    blink_interval_ms = BLINK_MOUNTED;
}

//--------------------------------------------------------------------+
// various helper functions
//--------------------------------------------------------------------+
void icon_draw(icon_t icon, bool flip, uint16_t c, PicoDisplay pico_display) {
   for(int ay = 0; ay < icon.height; ay++) {
        uint32_t sl = icon.bitmap[ay];
        for(int ax = 0; ax < 32; ax++) {
            if(flip) {
                if((0b10000000000000000000000000000000 >> ax) & sl) pico_display.frame_buffer[(ax + icon.pos_x) + (ay + icon.pos_y) * 240] = c;
            } else {
                if((0b1 << ax) & sl) pico_display.frame_buffer[(ax + icon.pos_x) + (ay + icon.pos_y) * 240] = c;
            }
        }
    }
};


void update_gui(Rect rectBtnA, Rect rectBtnX, Rect rectBtnY, bool running, bool mouse_enabled, bool keyboard_enabled, PicoDisplay pico_display) {
    // clear all buttons
    pico_display.set_pen(color_darkblue);
    pico_display.rectangle(rectBtnA);
    pico_display.rectangle(rectBtnX);
    pico_display.rectangle(rectBtnY);
    pico_display.set_pen(color_white);

    if (running) {                        
        if (! (mouse_enabled || keyboard_enabled)) {
            pico_display.text("nothing enabled...", Point(rectBtnA.x, rectBtnA.y), rectBtnA.w);
            pico_display.set_led(15,15,150);
        } else {
            pico_display.text("stop", Point(rectBtnA.x, rectBtnA.y), rectBtnA.w);
            pico_display.set_led(15,150,15); // green
        }
    } else {
        pico_display.text("START", Point(rectBtnA.x, rectBtnA.y), rectBtnA.w);
        pico_display.set_led(15,15,150);
    }                        

    if (mouse_enabled) icon_draw(icon_mouse, true, color_white, pico_display);
    else {
        icon_draw(icon_mouse, true, color_darkwhite, pico_display);
        icon_x.pos_x = icon_mouse.pos_x;
        icon_x.pos_y = icon_mouse.pos_y + (icon_mouse.height - icon_x.height) / 2; // different heights
        icon_draw(icon_x, false, color_red, pico_display);        
    }

    if (keyboard_enabled) icon_draw(icon_kbd, false, color_white, pico_display);
    else {
        icon_draw(icon_kbd, false, color_darkwhite, pico_display);
        icon_x.pos_x = icon_kbd.pos_x;
        icon_x.pos_y = icon_kbd.pos_y; 
        icon_draw(icon_x, false, color_red, pico_display);
    } 
    pico_display.update(); // now we've done our drawing let's update the screen
}
void initial_gui(PicoDisplay pico_display) {
    pico_display.set_pen(color_darkblue); // orange
    pico_display.clear(); // fill the screen with the current pen colour
    pico_display.set_pen(color_orange); 
    Rect rect_border(4, 4, PicoDisplay::WIDTH-8, PicoDisplay::HEIGHT-8);
    pico_display.rectangle(rect_border);
    pico_display.set_pen(color_darkblue); 
    Rect rect_border_inner(6, 6, PicoDisplay::WIDTH-12, PicoDisplay::HEIGHT-12);
    pico_display.rectangle(rect_border_inner);
}

//--------------------------------------------------------------------+
// USB HID
//--------------------------------------------------------------------+

void hid_task(bool move_mouse, bool type_character) {
    // Poll every 10ms
    const uint32_t interval_ms = 10;
    static uint32_t start_ms = 0;
    
    if (board_millis() - start_ms < interval_ms) return; // not enough time
    start_ms += interval_ms;
    
    // Remote wakeup
    if (tud_suspended()) {
        // Wake up host if we are in suspend mode
        // and REMOTE_WAKEUP feature is enabled by host
        tud_remote_wakeup();
    }

    /*------------- Mouse -------------*/
    uint32_t const  mouse_move_every_x = 19;
    static uint32_t mouse_move_every_x_counter = mouse_move_every_x;
    static uint32_t mouse_sequence = 0;
    static uint32_t substepCounter = 0;    
    int8_t const substep = 40 / 10; // 40 px in total for every arc of the lying-8

    // define a 'lying-8 type' of structure
    int8_t const xseq[16] = {1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, -1, -1};
    int8_t const yseq[16] = {1, 0, 0, -1, -1, 0, 0, 1, 1, 0, 0, -1, -1, 0, 0, 1};
    

    if (tud_hid_ready()) {
        if (move_mouse) {
            if (mouse_move_every_x_counter < mouse_move_every_x) {
                mouse_move_every_x_counter++;
            } else {
                mouse_move_every_x_counter = 0;
                int plus_minus_onex = rand() % 3 - 1;    // in the range -1 to 1
                int plus_minus_oney = rand() % 3 - 1;
                int8_t deltax = xseq[mouse_sequence] * substep + plus_minus_onex;
                int8_t deltay = yseq[mouse_sequence] * substep + plus_minus_oney;
                if (substepCounter < 9) {
                    substepCounter++;
                } else {
                    substepCounter = 0;                
                    if (mouse_sequence < 15) {
                        mouse_sequence++;
                    } else {
                        mouse_sequence = 0;
                    }
                }

                // no button, right + down, no scroll pan
                tud_hid_mouse_report(REPORT_ID_MOUSE, 0x00, deltax, deltay, 0, 0);

                // delay a bit before attempt to send keyboard report
                board_delay(10);
            }
        }
    }

    /*------------- Keyboard -------------*/
    if (tud_hid_ready()) {
        uint32_t const  kbd_print_every_x = 499;
        static uint32_t kbd_print_every_x_counter = kbd_print_every_x; // make sure it prints a character first and then waits
        

        // use to avoid send multiple consecutive zero report for keyboard
        static bool has_key = false;        

        if (type_character) {
            if (kbd_print_every_x_counter < kbd_print_every_x) {
                kbd_print_every_x_counter++;
                // send empty key report if previously has key pressed
                if (has_key) tud_hid_keyboard_report(REPORT_ID_KEYBOARD, 0, NULL);
                has_key = false;
            } else {
                kbd_print_every_x_counter = 0;            
                
                uint8_t keycode[6] = {0};
                keycode[0] = HID_KEY_A; // 0x04 to 0x27 are valid characters (a to 0)

                tud_hid_keyboard_report(REPORT_ID_KEYBOARD, 0, keycode);
                has_key = true;                        
            }            
        }
    }
}


// Invoked when received GET_REPORT control request
// Application must fill buffer report's content and return its length.
// Return zero will cause the stack to STALL request
uint16_t tud_hid_get_report_cb(uint8_t report_id, hid_report_type_t report_type, uint8_t *buffer, uint16_t reqlen) {
    // TODO not Implemented
    (void) report_id;
    (void) report_type;
    (void) buffer;
    (void) reqlen;

    return 0;
}

// Invoked when received SET_REPORT control request or
// received data on OUT endpoint ( Report ID = 0, Type = 0 )
void tud_hid_set_report_cb(uint8_t report_id, hid_report_type_t report_type, uint8_t const *buffer, uint16_t bufsize) {
    // TODO set LED based on CAPLOCK, NUMLOCK etc...
    (void) report_id;
    (void) report_type;
    (void) buffer;
    (void) bufsize;
}

//--------------------------------------------------------------------+
// BLINKING TASK
//--------------------------------------------------------------------+
void led_blinking_task(void) {
    static uint32_t start_ms = 0;
    static bool led_state = false;

    // Blink every interval ms
    if (board_millis() - start_ms < blink_interval_ms) return; // not enough time
    start_ms += blink_interval_ms;

    board_led_write(led_state);
    led_state = 1 - led_state; // toggle
}

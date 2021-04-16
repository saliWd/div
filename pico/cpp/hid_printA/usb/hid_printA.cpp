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

// icons, 32 x 20px, flipped along vertical axis
uint32_t icon_keyboard[] = {
    0b00000110000000000000000000000000,
    0b00000011000000000000000000000000,
    0b00000001110000000000000000000000,
    0b00000000001100000000000000000000,
    0b00000000000110000000000000000000,
    0b00000000000011000000000000000000,
    0b11111111111111111111111111111111,
    0b10000000000000000000000000000001,
    0b10011001100110011001100110011001,
    0b10011001100110011001100110011001,
    0b10000000000000000000000000000001,
    0b10000000000000000000000000000001,
    0b10011001100110011001100110011001,
    0b10011001100110011001100110011001,
    0b10000000000000000000000000000001,
    0b10000000000000000000000000000001,
    0b10011001111111111111111110011001,
    0b10011001111111111111111110011001,
    0b10000000000000000000000000000001,
    0b11111111111111111111111111111111
  };
static uint32_t blink_interval_ms = BLINK_NOT_MOUNTED;

using namespace pimoroni;

uint16_t buffer[PicoDisplay::WIDTH * PicoDisplay::HEIGHT];
PicoDisplay pico_display(buffer);
const uint16_t color_font = pico_display.create_pen(200, 200, 200); // almost white
const uint16_t color_bg = pico_display.create_pen(10, 20, 30); // some dark, slightly blue color

void led_blinking_task(void);
void hid_task(bool move_mouse, bool type_character);
void clearRect(PicoDisplay pico_display, Rect rectangle);
void icon_32_20(uint32_t *p, int x, int y, uint16_t c, PicoDisplay pico_display);

/*------------- MAIN -------------*/
int main(void) {
    board_init();
    tusb_init();
    srand(time(0));

    // setup the display
    pico_display.init(); // 240 x 135 pixel

    pico_display.set_backlight(150);
    // set the colour of the pen
    // parameters are red, green, blue all between 0 and 255
    pico_display.set_pen(255, 165, 0); // orange

    // fill the screen with the current pen colour
    pico_display.clear();

    // draw a box to put some text in
    const int outer_margin = 10;
    pico_display.set_pen(color_bg); 
    Rect text_rect_inner_box(outer_margin, outer_margin, PicoDisplay::WIDTH-2*outer_margin, PicoDisplay::HEIGHT-2*outer_margin);
    Rect text_rectBtnA(outer_margin, outer_margin, 180, 30); // on/off
    Rect text_rectBtnB(outer_margin, 94, 140, 30); // character select
    Rect text_rectBtnX(191, outer_margin, 39, 30); // mouse
    Rect text_rectBtnY(151, 94, 80, 30); // keyboard
    pico_display.rectangle(text_rect_inner_box); // generates an orange 10px border around the full box
    
    // write some text inside the box with 10 pixels of margin
    text_rectBtnA.deflate(10);
    text_rectBtnB.deflate(10);
    pico_display.set_pen(color_font);
    pico_display.set_font(&font8);
    pico_display.text("Button A to start", Point(text_rectBtnA.x, text_rectBtnA.y), text_rectBtnA.w);
    pico_display.text("abc...", Point(text_rectBtnB.x, text_rectBtnB.y), text_rectBtnB.w);
    pico_display.text("mouse", Point(text_rectBtnX.x, text_rectBtnX.y), text_rectBtnX.w);
    // pico_display.text("keyboard", Point(text_rectBtnY.x, text_rectBtnY.y), text_rectBtnY.w);
    icon_32_20(icon_keyboard, 187, 90, color_font, pico_display);
    pico_display.set_led(15,15,150); // blueish, only at the beginning it's blue

    pico_display.update(); // now we've done our drawing let's update the screen

    bool running = false;
    bool move_mouse = false;
    bool type_character = false;
    bool mouse_enabled = true;
    bool keyboard_enabled = false;

    uint8_t which_button = 0;
    Rect text_rect(10, 10, 10, 10); // values here are arbitrary

    uint16_t debounce_cnt = 0;

    while (1) {
        if (pico_display.is_pressed(pico_display.A)) { // make sure I react only onto one button
                which_button = 1;
                text_rect = text_rectBtnA;
            } else if (pico_display.is_pressed(pico_display.B)) {
                which_button = 2;
                text_rect = text_rectBtnB;
            } else if (pico_display.is_pressed(pico_display.X)) {
                which_button = 3;
                text_rect = text_rectBtnX;
            } else if (pico_display.is_pressed(pico_display.Y)) {
                which_button = 4;
                text_rect = text_rectBtnY;
            } else which_button = 0;

        if ((debounce_cnt == 0) && (which_button > 0)) { // do something. If not 0, just ignore the pressed button                
            clearRect(pico_display, text_rect);

            if (which_button == 1) { // button A switches between active and waiting.
                if (running) { // currently running, afterwards not anymore                        
                    pico_display.text("Button A to start", Point(text_rect.x, text_rect.y), text_rect.w);
                    pico_display.set_led(150,15,15); //reddish
                } else { // currently not running, afterwards it is
                    if (! (mouse_enabled || keyboard_enabled)) {
                        pico_display.text("Error: nothing enabled", Point(text_rect.x, text_rect.y), text_rect.w);
                        pico_display.set_led(15,15,150);
                    } else {
                        pico_display.text("Button A to stop", Point(text_rect.x, text_rect.y), text_rect.w);
                        pico_display.set_led(15,150,15); // green
                    }
                }                        
                running = !running;
            } // button a
            if (which_button == 2) { // TODO: functionality button B
                pico_display.text("abc...", Point(text_rect.x, text_rect.y), text_rect.w);
            }
            if (which_button == 3) {
                if (mouse_enabled) pico_display.text("no mouse", Point(text_rect.x, text_rect.y), text_rect.w);
                else pico_display.text("mouse", Point(text_rect.x, text_rect.y), text_rect.w);            
                                    
                mouse_enabled = !mouse_enabled;
            } // button X
            if (which_button == 4) {
                if (keyboard_enabled) pico_display.text("X", Point(text_rect.x, text_rect.y), text_rect.w); // TODO
                else icon_32_20(icon_keyboard, 187, 90, color_font, pico_display);
                
                keyboard_enabled = !keyboard_enabled;
            } // button Y

            pico_display.update();
            move_mouse = running && mouse_enabled;
            type_character = running && keyboard_enabled;

            debounce_cnt = 1000;
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
// clear previous content in a certain rectangle
void clearRect(PicoDisplay pico_display, Rect rectangle) {
    pico_display.set_pen(color_bg);
    pico_display.rectangle(rectangle); // fill it with that
    pico_display.set_pen(color_font);
}
void icon_32_20(uint32_t *p, int x, int y, uint16_t c, PicoDisplay pico_display) {
    for(int ay = 0; ay < 20; ay++) {
        uint32_t sl = p[ay];
        for(int ax = 0; ax < 32; ax++) {
            // does not work as expected if((0b10000000000000000000000000000000 >> ax) & sl) pico_display.frame_buffer[(ax + x) + (ay + y) * 240] = c; 
            if((0b1 << ax) & sl) pico_display.frame_buffer[(ax + x) + (ay + y) * 240] = c;
        }
    }
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
    static uint32_t mouse_move_every_x_counter = 0;
    uint32_t const  mouse_move_every_x = 9;
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
        static uint32_t kbd_print_every_x_counter = 0;
        uint32_t const  kbd_print_every_x = 499;

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

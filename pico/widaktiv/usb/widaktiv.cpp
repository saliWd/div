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
#include <math.h>

#include <iostream>
#include <fstream>

#include "bsp/board.h"
#include "tusb.h"

#include "usb_descriptors.hpp"
#include "imgdata.hpp"
#include "pico_display.hpp"
#include "font8_data.hpp"
#include "pico/multicore.h"

// required for the bootsel button check in headless mode
#include "pico/stdlib.h"
#include "hardware/clocks.h"
#include "hardware/gpio.h"
#include "hardware/sync.h"
#include "hardware/pwm.h"
#include "hardware/structs/ioqspi.h"
#include "hardware/structs/sio.h"

typedef uint16_t u16;
typedef uint32_t u32;

//--------------------------------------------------------------------+
// MACRO CONSTANT TYPEDEF PROTYPES
//--------------------------------------------------------------------+
enum {
    BLINK_NOT_MOUNTED = 250,
    BLINK_MOUNTED = 1000,
    BLINK_SUSPENDED = 2500,
};
struct Rectangle {
    uint8_t x = 0, y = 0, w = 0, h = 0;

    Rectangle() = default;
    Rectangle(uint8_t x, uint8_t y, uint8_t w, uint8_t h) : x(x), y(y), w(w), h(h) {}
};

#define PI 3.14159265
#define MULTICORE_FLAG 16384 // arbitrary value, just not using the lower bits
#ifdef headlessMode // when headless, the mouse is enabled and starts right away. Keyboard is disabled
    const bool HEADLESS = true; 
#else
    const bool HEADLESS = false;
#endif
static u32 blink_interval_ms = BLINK_NOT_MOUNTED;

using namespace pimoroni;

u16 buffer[PicoDisplay::WIDTH * PicoDisplay::HEIGHT];
PicoDisplay pico_display(buffer);

const u16 color_white = pico_display.create_pen(255, 255, 255);
const u16 color_black = pico_display.create_pen(0, 0, 0);
const u16 color_red = pico_display.create_pen(240, 20, 20);

void hid_task(bool move_mouse, bool type_character);
void update_gui(bool running, bool mouse_enabled, bool keyboard_enabled, PicoDisplay pico_display);
void draw_x(Point center, int half_len);
void replace_img(u16 *new_img, Rectangle rec);
void animate_arrow(bool running);
void animate_active(bool running);
u32 get_status(bool running, bool mouse_enabled, bool keyboard_enabled);
bool get_status_bit(u16 bit, u32 status);
bool __no_inline_not_in_flash_func(get_bootsel_button)();

// core1 is responsible for all the display/buffer accesses
void core1_entry() {
    multicore_fifo_push_blocking(MULTICORE_FLAG); // signal core 0 the startup is done
    bool running_core1 = false;
    bool mouse_enabled_core1 = true;
    bool keyboard_enabled_core1 = false;

    // setup the display
    pico_display.init(); // 240 x 135 pixel
    pico_display.set_backlight(150);
    pico_display.set_led(15,15,150); // blue
    pico_display.set_font(&font8);
    memcpy(buffer, background_bmp, 240*135*2); // copy the background image from the .hpp file into the display buffer
    update_gui(running_core1, mouse_enabled_core1, keyboard_enabled_core1, pico_display);
    
    u32 g = multicore_fifo_pop_blocking(); // check communication from core 0 to core 1
    if (g != MULTICORE_FLAG) pico_display.set_led(150,15,15); // red
    
    u32 status_core1 = 0;
    
    while (1) {
        if (multicore_fifo_rvalid()) {
            status_core1 = multicore_fifo_pop_blocking();
            running_core1 = get_status_bit(0, status_core1);
            mouse_enabled_core1 = get_status_bit(1, status_core1);
            keyboard_enabled_core1 = get_status_bit(2, status_core1);
            update_gui(running_core1, mouse_enabled_core1, keyboard_enabled_core1, pico_display);
        } 
        animate_active(running_core1);
        animate_arrow(running_core1);
    }
}


// core 0 handles all the button inputs and the USB keyboard/mouse stuff
int main(void) {
    board_init();
    tusb_init();
    srand(time(0));

    uint8_t which_button = 0;
    u16 debounce_cnt = 500; // make sure there is not button press at the beginning
    u16 pulseLedCnt = 18000; // initial brightness
    bool incPolarity = false; // polarity of increment, either + 1 or - 1; 

    bool running = false;
    bool move_mouse = false;
    bool type_character = false;
    bool mouse_enabled = true;
    bool keyboard_enabled = false;
    uint slice_num = 0;
    u16 top = 0;

    // initialization stuff
    if (HEADLESS) { // use the board LED and move the mouse from beginning
        running = true; // mouse is enabled by default in headless
        move_mouse = running && mouse_enabled; // move_mouse = true at the start

	    gpio_set_function(PICO_DEFAULT_LED_PIN, GPIO_FUNC_PWM); // Tell GPIO 0 it is allocated to the PWM
	    slice_num = pwm_gpio_to_slice_num(PICO_DEFAULT_LED_PIN); // get PWM slice for GPIO

	    // set frequency
	    float divider = clock_get_hz(clk_sys) / 1'000'000UL;  // typically 125 MHz (free running). let's arbitrarily choose to run pwm clock at 1MHz
	    pwm_set_clkdiv(slice_num, divider); // pwm clock should now be running at 1MHz
        top =  1'000'000UL/440 -1; // 440 frequency we want to generate in Hz, TOP is u16 has a max of 65535, being 65536 cycles
	    pwm_set_wrap(slice_num, top);

	    pwm_set_chan_level(slice_num, PICO_DEFAULT_LED_PIN, (top+1) * pulseLedCnt/32000 - 1); // start with 60% duty cycle
	    pwm_set_enabled(slice_num, true); // start
    } else { // multicore init. Core1 does the display stuff
        multicore_launch_core1(core1_entry);    
        u32 g = multicore_fifo_pop_blocking(); // Blocks until core1 is done starting up
    
        if (g == MULTICORE_FLAG) multicore_fifo_push_blocking(MULTICORE_FLAG); // check communication to core1
        else pico_display.set_led(150,15,15); // red
    }

    while (1) {
        if (HEADLESS) {
            if(get_bootsel_button()) which_button = 1; // function returns true when pressed, this is the start/stop button in non-headless
            else which_button = 0;
        } else { // not headless, have several buttons on the display
            if (pico_display.is_pressed(pico_display.A)) which_button = 1; // make sure I react only onto one button. Button2 = B is not used
            else if (pico_display.is_pressed(pico_display.X)) which_button = 3;
            else if (pico_display.is_pressed(pico_display.Y)) which_button = 4;
            else which_button = 0;
        } // end non-headless
        if ((debounce_cnt == 0) && (which_button > 0)) { // do something. If debounce is not 0, just ignore the pressed button                
            if (which_button == 1) running = !running;
            if (which_button == 3) mouse_enabled = !mouse_enabled; // button X
            if (which_button == 4) keyboard_enabled = !keyboard_enabled; // button Y

            move_mouse = running && mouse_enabled;
            type_character = running && keyboard_enabled;

            debounce_cnt = 500;
            if(HEADLESS) {
                if (running)  {
                    gpio_set_function(PICO_DEFAULT_LED_PIN, GPIO_FUNC_PWM); // Tell GPIO 0 it is allocated to the PWM
                    pwm_set_enabled(slice_num, true); // enable pwm again
                } else  { // need to use the GPIO function to make sure it's 0 when not running
                    pwm_set_enabled(slice_num, false); // disable pwm first, then switch to GPIO
                    gpio_init(PICO_DEFAULT_LED_PIN);
                    gpio_set_dir(PICO_DEFAULT_LED_PIN, GPIO_OUT);
                    gpio_put(PICO_DEFAULT_LED_PIN, 0); 
                }
            } else {
                if (multicore_fifo_wready()) multicore_fifo_push_blocking(get_status(running,mouse_enabled,keyboard_enabled));  // update core1 running variable
                else pico_display.set_led(150,15,15); // red // we have an issue, can't write into FIFO because it's full 
            }
        }
        if (debounce_cnt > 0) {
            debounce_cnt--;
            busy_wait_us_32(1000);
        }

        tud_task(); // tinyusb device task
        hid_task(move_mouse, type_character);
        if(HEADLESS && running) {
            if (incPolarity) { // counting up
                if (pulseLedCnt < 22000) pulseLedCnt++; // do not use the max value, limit brightness
                else incPolarity = !incPolarity;
            } else { // counting down
                if (pulseLedCnt > 1000) pulseLedCnt--; // 0 brightness is bad, can be mistaken for 'not running'
                else incPolarity = !incPolarity;
            }

            u16 pwm_level = (top+1) * pulseLedCnt / 32000 - 1; // pwm_level
	        pwm_set_chan_level(slice_num, PICO_DEFAULT_LED_PIN, pwm_level);
        }
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
// clears the whole display and draws the icons in correct state (active/inactive)
void update_gui(bool running, bool mouse_enabled, bool keyboard_enabled, PicoDisplay pico_display) {
    // this is a bit overkill, need to replace only the 'active area'
    memcpy(buffer, background_bmp, 240*135*2); // copy the whole background image from the .hpp file into the display buffer
    
    if (running) {                        
        if (! (mouse_enabled || keyboard_enabled)) {
            pico_display.set_pen(color_white);
            pico_display.text("nothing enabled...", Point(20, 100), 170);
        }
        replace_img(stop_bmp, Rectangle(50, 13, 43, 29));        
    }                        
    
    if (! mouse_enabled) draw_x(Point(213, 24), 20);
    if (! keyboard_enabled) draw_x(Point(213, 111), 20);
    pico_display.update(); // now we've done our drawing let's update the screen
}

// replace part of the buffer with new image
void replace_img(u16* new_img, Rectangle rec) {    
    u16* startPixel;
    u16* startPixNewImg;
    for (int ay = 0; ay < rec.h; ay++) {
        startPixel = buffer + 240 * (rec.y+ay) + rec.x;
        startPixNewImg = new_img + ay * rec.w;
        memcpy(startPixel, startPixNewImg, 2*rec.w);
    }
}

// draws a red X over the center point
void draw_x(Point center, int half_len) {
    pico_display.set_pen(color_red);
    for (int xvar = -1; xvar < 2; xvar++) {
        for (int yvar = -1; yvar < 2; yvar++) {
            pico_display.line(Point(center.x-half_len+xvar, center.y-half_len+yvar), Point(center.x+half_len+xvar, center.y+half_len+yvar));
            pico_display.line(Point(center.x-half_len+xvar, center.y+half_len+yvar), Point(center.x+half_len+xvar, center.y-half_len+yvar));
        }
    } 
}

// plays a small animation by replacing part of the buffer
void animate_arrow(bool running) {
    const u32 interval_ms = 400; // poll every x ms
    static u32 start_ms = 0;
    
    if (board_millis() - start_ms < interval_ms) return; // not enough time
    start_ms += interval_ms;

    if (running) return; // nothing to animate if we're running already
    
    static uint8_t sequence = 0;
    
    if (sequence < 5) sequence++; // 0 to 5
    else sequence = 0;

    u16* new_img = arr0_bmp;
    if (sequence == 1) new_img = arr1_bmp;
    else if (sequence == 2) new_img = arr2_bmp;
    else if (sequence == 3) new_img = arr3_bmp;
    else if (sequence == 4) new_img = arr4_bmp;
    else if (sequence == 5) new_img = arr5_bmp;
    replace_img(new_img, Rectangle(0, 14, 23, 19));
    pico_display.update();
}

// plays a circle-around animation
void animate_active(bool running) {
    const u32 interval_ms = 30; // poll every x ms
    static u32 start_ms = 0;
    
    if (board_millis() - start_ms < interval_ms) return;
    start_ms += interval_ms;

    if (!running) return; // nothing to animate if we're not running
    
    const uint8_t NUM_STEPS = 180;
    static uint8_t sequence = 0;
    uint8_t old_sequence = sequence;
    if (sequence < NUM_STEPS - 1) sequence++;
    else sequence = 0;
    
    // center_pix: x = 120, y = 67. have a 25px radius
    const double PI2NUMSTEPS = 2 * PI / NUM_STEPS;
    u16 old_centerpix_x = 120 + round(sin(double(old_sequence) * PI2NUMSTEPS) * 25.0);
    u16 old_centerpix_y = 67 + round(cos(double(old_sequence) * PI2NUMSTEPS) * 25.0);
    u16 centerpix_x = 120 + round(sin(double(sequence) * PI2NUMSTEPS) * 25.0);
    u16 centerpix_y = 67 + round(cos(double(sequence) * PI2NUMSTEPS) * 25.0);

    const int RADIUS = 5; // radius/size of the point which moves
    const int SIZE = 2*RADIUS+1;
    
    // restore the old content
    int offset;
    for (int ay = 0; ay < SIZE; ay++) {
        offset = 240 * (old_centerpix_y-RADIUS+ay) + old_centerpix_x-RADIUS;
        memcpy(buffer + offset, background_bmp + offset, 2*SIZE);
    }
    // draw the new one
    pico_display.set_pen(color_black);
    pico_display.circle(Point(centerpix_x,centerpix_y),RADIUS);
    pico_display.set_pen(color_white);
    pico_display.circle(Point(centerpix_x,centerpix_y),RADIUS-2);

    pico_display.update();
}

u32 get_status(bool running, bool mouse_enabled, bool keyboard_enabled) {
    u32 status = 0;
    if (running) status += 1; // bit0
    if (mouse_enabled) status += 2; // bit1
    if (keyboard_enabled) status += 4; // bit2
    return status;
}

bool get_status_bit(u16 bit, u32 status) {
    u32 bitmask = 1 << bit;
    if (bit < 3)  return status & bitmask;
    else return false; 
}

//--------------------------------------------------------------------+
// USB HID
//--------------------------------------------------------------------+
void hid_task(bool move_mouse, bool type_character) {
    // Poll every 10ms
    const u32 interval_ms = 10;
    static u32 start_ms = 0;
    static u32 mouse_sequence = 0;
    static u32 substepCounter = 0;    
    
    if (board_millis() - start_ms < interval_ms) return; // not enough time
    start_ms += interval_ms;
    
    // Remote wakeup
    if (tud_suspended()) {
        // Wake up host if we are in suspend mode
        // and REMOTE_WAKEUP feature is enabled by host
        tud_remote_wakeup();
    }

    /*------------- Mouse -------------*/
    u32 const  mouse_move_every_x = 79;
    static u32 mouse_move_every_x_counter = mouse_move_every_x;
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
        u32 const  kbd_print_every_x = 499;
        static u32 kbd_print_every_x_counter = kbd_print_every_x; // make sure it prints a character first and then waits
        

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
    return;
}


// Invoked when received GET_REPORT control request
// Application must fill buffer report's content and return its length.
// Return zero will cause the stack to STALL request
uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id, hid_report_type_t report_type, uint8_t* buffer, uint16_t reqlen)
{
  // TODO not Implemented
  (void) instance;
  (void) report_id;
  (void) report_type;
  (void) buffer;
  (void) reqlen;

  return 0;
}

// Invoked when received SET_REPORT control request or
// received data on OUT endpoint ( Report ID = 0, Type = 0 )
void tud_hid_set_report_cb(uint8_t instance, uint8_t report_id, hid_report_type_t report_type, uint8_t const* buffer, uint16_t bufsize)
{
  (void) instance;

  if (report_type == HID_REPORT_TYPE_OUTPUT)
  {
    // Set keyboard LED e.g Capslock, Numlock etc...
    if (report_id == REPORT_ID_KEYBOARD)
    {
      // bufsize should be (at least) 1
      if ( bufsize < 1 ) return;

      uint8_t const kbd_leds = buffer[0];

      if (kbd_leds & KEYBOARD_LED_CAPSLOCK)
      {
        // Capslock On: disable blink, turn led on
        blink_interval_ms = 0;
        board_led_write(true);
      }else
      {
        // Caplocks Off: back to normal blink
        board_led_write(false);
        blink_interval_ms = BLINK_MOUNTED;
      }
    }
  }
}



// bootsel button: use it in headless mode to start/stop the mouse
// Picoboard has a button attached to the flash CS pin, which the bootrom checks, and jumps straight to the USB bootcode if the button is pressed (pulling flash CS low). 
// We can check this pin in by jumping to some code in SRAM (so that the XIP interface is not required), floating the flash CS pin, and observing whether it is pulled low.
// This doesn't work if others are trying to access flash at the same time, e.g. the other core.
bool __no_inline_not_in_flash_func(get_bootsel_button)() {
    const uint CS_PIN_INDEX = 1;

    // Must disable interrupts, as interrupt handlers may be in flash
    u32 flags = save_and_disable_interrupts();

    // Set chip select to Hi-Z
    hw_write_masked(&ioqspi_hw->io[CS_PIN_INDEX].ctrl,
                    GPIO_OVERRIDE_LOW << IO_QSPI_GPIO_QSPI_SS_CTRL_OEOVER_LSB,
                    IO_QSPI_GPIO_QSPI_SS_CTRL_OEOVER_BITS);

    // Note we can't call into any sleep functions in flash right now
    for (volatile int i = 0; i < 1000; ++i);

    // The HI GPIO registers in SIO can observe and control the 6 QSPI pins.
    // Note the button pulls the pin *low* when pressed.
    bool button_state = !(sio_hw->gpio_hi_in & (1u << CS_PIN_INDEX));

    // Need to restore the state of chip select to return to code in flash
    hw_write_masked(&ioqspi_hw->io[CS_PIN_INDEX].ctrl,
                    GPIO_OVERRIDE_NORMAL << IO_QSPI_GPIO_QSPI_SS_CTRL_OEOVER_LSB,
                    IO_QSPI_GPIO_QSPI_SS_CTRL_OEOVER_BITS);

    restore_interrupts(flags);

    return button_state;
}



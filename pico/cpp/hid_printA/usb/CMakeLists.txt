add_executable(hid_printA
        hid_printA.c
        usb_descriptors.c
        )

target_include_directories(hid_printA PRIVATE ${CMAKE_CURRENT_LIST_DIR})

target_link_libraries(hid_printA PRIVATE pico_stdlib tinyusb_device tinyusb_board)
pico_add_extra_outputs(hid_printA)

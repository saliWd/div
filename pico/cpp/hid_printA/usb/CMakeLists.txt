add_executable(hid_printA
        hid_printA.cpp
        usb_descriptors.cpp
        )

target_include_directories(hid_printA PRIVATE ${CMAKE_CURRENT_LIST_DIR})

target_link_libraries(hid_printA PRIVATE pico_stdlib tinyusb_device tinyusb_board pico_display)
pico_add_extra_outputs(hid_printA)

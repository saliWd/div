add_executable(widaktiv widaktiv.cpp usb_descriptors.cpp)

target_include_directories(widaktiv PRIVATE ${CMAKE_CURRENT_LIST_DIR})

target_link_libraries(widaktiv PRIVATE pico_multicore pico_stdlib tinyusb_device tinyusb_board pico_display)

# pico_enable_stdio_uart(widaktiv 1)
# pico_enable_stdio_uart(widaktiv 0)

pico_add_extra_outputs(widaktiv)







# I'd like two compiler runs, one with headlessMode set and the other without it
add_executable(widaktivHeadless widaktiv.cpp usb_descriptors.cpp)
target_include_directories(widaktivHeadless PRIVATE ${CMAKE_CURRENT_LIST_DIR})
target_link_libraries(widaktivHeadless PRIVATE pico_multicore pico_stdlib tinyusb_device tinyusb_board pico_display)
pico_add_extra_outputs(widaktivHeadless)
add_definitions(-DheadlessMode)
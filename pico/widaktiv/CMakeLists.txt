cmake_minimum_required(VERSION 3.15)

# Pull in SDK (must be before project)
# assuming it's in the normal 'git-repo' location
set(PICO_SDK_PATH "../../../../pico-sdk")
# set(PICO_BOARD "pico_w")
set(PICO_BOARD "pico")
include(pico_sdk_import.cmake)

project(pico_examples C CXX ASM)
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

set(PICO_EXAMPLES_PATH ${PROJECT_SOURCE_DIR})

# Initialize the SDK
pico_sdk_init()

add_subdirectory(st7789)
add_subdirectory(libraries)
add_subdirectory(usb)

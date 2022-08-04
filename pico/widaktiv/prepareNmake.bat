@echo off
setlocal

if not exist "build/" mkdir build

:: Holzhammermethode, delete some dirs
:: does not work at the moment...
if exist "build/CMakeFiles" del /S /Q "build/CMakeFiles" >nul 2>&1
if exist "build/usb" del /S /Q "build/usb" >nul 2>&1
if exist "build/st7789" del /S /Q "build/st7789" >nul 2>&1
if exist "build/libraries" del /S /Q "build/libraries" >nul 2>&1
:: do not change the pico-sdk / elf2uf2 / pioasm directories (shouldn't change that often and they are big)

:: this one is working
cd build
:: source one directory up, build in this directory
cmake -G "NMake Makefiles" -S .. -B .
cd ../
dir build

:: now use nmake (in build directory) with the Developer command console for VS2022

endlocal
@echo on
@echo off
setlocal

if not exist "build/" mkdir build

:: Holzhammermethode, delete some dirs
if exist "build/CMakeFiles" del /S /Q "build/CMakeFiles" >nul 2>&1
if exist "build/usb" del /S /Q "build/usb" >nul 2>&1
if exist "build/st7789" del /S /Q "build/st7789" >nul 2>&1
if exist "build/libraries" del /S /Q "build/libraries" >nul 2>&1


cd build
:: source one directory up, build in this directory
cmake -G "NMake Makefiles" -S .. -B .
cd ../
dir build

:: now use nmake (in build directory) with the Developer command console for VS2022

endlocal
@echo on
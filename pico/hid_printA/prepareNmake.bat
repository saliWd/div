@echo off
setlocal

:: Holzhammermethode, delete some dirs
if exist "build/CMakeFiles" del /S /Q "build/CMakeFiles" >nul 2>&1
if exist "build/usb" del /S /Q "build/usb" >nul 2>&1
if exist "build/st7789" del /S /Q "build/st7789" >nul 2>&1
if exist "build/libraries" del /S /Q "build/libraries" >nul 2>&1


cd build
cmake -G "NMake Makefiles" .. 
cd ../
dir build

:: now use nmake (in build directory) with the Developer command console for VS2019 (%comspec% /k "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\Common7\Tools\VsDevCmd.bat")

endlocal
@echo on
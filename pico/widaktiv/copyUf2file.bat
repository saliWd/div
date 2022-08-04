@echo off
setlocal

:: copy it from the build directory as this may be deleted
copy build\usb\widaktiv.uf2 widaktiv.uf2
copy build\usb\widaktivHeadless.uf2 widaktivHeadless.uf2

:: copy it to the pico itself but check first whether it's attached
if exist "D:\INFO_UF2.TXT" copy widaktiv.uf2 "D:\widaktiv.uf2"


endlocal
@echo on
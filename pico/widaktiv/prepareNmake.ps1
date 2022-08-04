pwd
pause

Remove-Item -Path .\build\CMakeFiles -Recurse
Remove-Item -Path .\build\usb -Recurse
Remove-Item -Path .\build\st7789 -Recurse
Remove-Item -Path .\build\libraries -Recurse

cd build
# source one directory up, build in this directory
cmake -G "NMake Makefiles" -S .. -B .
cd ../
dir build

# now use nmake (in build directory) with the Developer command console for VS2022

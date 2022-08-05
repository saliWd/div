# tested with power shell on windows 10

import os
from pathlib import Path

def rmdir(directory):
    directory = Path(directory)
    for item in directory.iterdir():
        if item.is_dir():
            rmdir(item)
        else:
            item.unlink()
    directory.rmdir()

current_dir = os.getcwd()
build_dir = current_dir + "\\build" # escape the backslash

build_dir_exist = os.path.exists(build_dir)

if (build_dir_exist) :
  cmakefiles_dir = build_dir + "\\CMakeFiles"
  cmakefiles_dir_exist  = os.path.exists(cmakefiles_dir)
  if (cmakefiles_dir_exist) :
    # rmdir(Path(cmakefiles_dir))
    print("need to delete the build, cmake directory")  
  
  
else : # build directory does not exist
  os.mkdir(build)

exit()


## if not exist "build/" mkdir build
## 
## :: Holzhammermethode, delete some dirs
## :: does not work at the moment...
## if exist "build/CMakeFiles" del /S /Q "build/CMakeFiles" >nul 2>&1
## if exist "build/usb" del /S /Q "build/usb" >nul 2>&1
## if exist "build/st7789" del /S /Q "build/st7789" >nul 2>&1
## if exist "build/libraries" del /S /Q "build/libraries" >nul 2>&1
## :: do not change the pico-sdk / elf2uf2 / pioasm directories (shouldn't change that often and they are big)
## 
## :: this one is working
## cd build
## :: source one directory up, build in this directory
## cmake -G "NMake Makefiles" -S .. -B .
## cd ../
## dir build
## 
## :: now use nmake (in build directory) with the Developer command console for VS2022
## 
## endlocal
## @echo on
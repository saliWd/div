# tested with power shell on windows 10

import os
from pathlib import Path
import subprocess

def rmdir(directory):
    directory = Path(directory)
    for item in directory.iterdir():
        if item.is_dir():
            rmdir(item)
        else:
            item.unlink()
    directory.rmdir()

current_dir = os.getcwd()
build_dir = current_dir + '\\build' # escape the backslash

build_dir_exist = os.path.exists(build_dir)

if (build_dir_exist) :
  cmakefiles_dir = build_dir + '\\CMakeFiles'
  usb_dir = build_dir + '\\usb'
  st7789_dir = build_dir + '\\st7789'
  libraries_dir = build_dir + '\\libraries'
  
  # TODO: move into a loop over a list
  if (os.path.exists(cmakefiles_dir)) :
    rmdir(Path(cmakefiles_dir))
    print('did delete the build, cmake directory')  
  if (os.path.exists(usb_dir)) :
    rmdir(Path(usb_dir))
    print('did delete the build, usb directory')  
  if (os.path.exists(st7789_dir)) :
    rmdir(Path(st7789_dir))
    print('did delete the build, st7789 directory')  
  if (os.path.exists(libraries_dir)) :
    rmdir(Path(libraries_dir))
    print('did delete the build, libraries directory')   
  ## do not change the pico-sdk / elf2uf2 / pioasm directories (shouldn't change that often and they are big)
  
else : # build directory does not exist. create it
  os.mkdir(build)

# now I am sure the build directory exists and the (user code) outputs are deleted
os.chdir('build')
p0 = subprocess.run('cmake -G "NMake Makefiles" -S .. -B .')
os.chdir('..\\')

# doesn't work at the moment. Don't know why though...
# p1 = subprocess.run('dir build') # TODO: could use python functionality for that

print('Done. Now use nmake (in build directory) with the Developer command console for VS2022')
exit()

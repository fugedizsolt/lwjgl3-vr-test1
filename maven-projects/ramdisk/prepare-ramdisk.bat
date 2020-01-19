SET ORIG_PATH=D:\w0rk\GitHub\lwjgl3-vr-test1\maven-projects\
SET RAMDISK_PATH=R:\lwjgl3-vr-test1

mkdir %RAMDISK_PATH%

call mklink-folder.bat %RAMDISK_PATH%\src                          %ORIG_PATH%\src
call mklink-folder.bat %RAMDISK_PATH%\.settings                    %ORIG_PATH%\.settings
call mklink-file.bat   %RAMDISK_PATH%\log4j-config.xml             %ORIG_PATH%\log4j-config.xml
call mklink-file.bat   %RAMDISK_PATH%\pom.xml                      %ORIG_PATH%\pom.xml
call mklink-file.bat   %RAMDISK_PATH%\bild.bat                     %ORIG_PATH%\bild.bat
call mklink-file.bat   %RAMDISK_PATH%\run1.bat                     %ORIG_PATH%\run1.bat
call mklink-file.bat   %RAMDISK_PATH%\.classpath                   %ORIG_PATH%\.classpath
call mklink-file.bat   %RAMDISK_PATH%\.project                     %ORIG_PATH%\.project

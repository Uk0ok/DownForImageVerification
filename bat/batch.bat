@echo off
set JAVA_HOME="C:\Program Files\Java\jdk-11.0.17"
set CLASSPATH=.;./lib/xvarmapi_20181220.jar;./lib/log4j-1.2.15.jar;./lib/ojdbc8.jar;./bin;./conf

echo %CLASSPATH%

cd C:/dev/JAVA/first_prj

%JAVA_HOME%\bin\java.exe -cp %CLASSPATH% com.ecm.download.Down_main

pause
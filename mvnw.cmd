@echo off
setlocal

set JAVA_HOME=C:\PROGRA~1\Java\jdk-22
set WRAPPER_JAR=C:\Users\PABERR~1\IDEAPR~1\PROYEC~1\.mvn\wrapper\maven-wrapper.jar
set PROJECT_DIR=C:\Users\PABERR~1\IDEAPR~1\PROYEC~1

%JAVA_HOME%\bin\java.exe -Dmaven.multiModuleProjectDirectory=%PROJECT_DIR% -jar %WRAPPER_JAR% %*

endlocal

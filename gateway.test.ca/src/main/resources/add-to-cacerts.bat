REM
REM
REM   Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
REM
REM

REM ###########################################
REM # Add democa.crt to cacerts (JDK 1.6,1.7) #
REM ###########################################

echo @off

REM set this to install it on a different java (jdk or jre) installation 
REM than what's specified in environment variables
SET JAVA_HOME=C:\Program Files\Java\jre7

REM enable either this if you have specified a JDK
REM SET CACERTS=%JAVA_HOME%/jre/lib/security/cacerts

REM or this if you have specified a JRE
SET CACERTS=%JAVA_HOME%/lib/security/cacerts

echo @on

REM Deleting kaazing-democa alias
cmd /c keytool -delete -alias kaazing-democa -keystore "%CACERTS%" -storepass changeit -noprompt

REM Adding Demo CA as kaazing-democa alias
cmd /c keytool -importcert -trustcacerts -alias kaazing-democa -file democa.crt -keystore "%CACERTS%" -storepass changeit -noprompt

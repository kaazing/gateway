REM Note: Requires Java 8 Keytool

REM ###########################################
REM # Add democa.crt to cacerts (JDK 1.8) #
REM ###########################################

SET exeDir=%~dp0
SET CACERTS=%JAVA_HOME%/jre/lib/security/cacerts

REM Deleting kaazing-democa alias
REM cmd /c keytool -delete -alias kaazing-democa -keystore "%CACERTS%" -storepass changeit -noprompt

REM Adding Demo CA as kaazing-democa alias
cmd /c keytool -importcert -trustcacerts -alias kaazing-democa -file %exeDir%democa.crt -keystore "%CACERTS%" -storepass changeit -noprompt

@REM
@REM Copyright 2007-2016, Kaazing Corporation. All rights reserved.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off

if "%OS%" == "Windows_NT" SETLOCAL EnableDelayedExpansion
rem ---------------------------------------------------------------------------
rem Windows start script for Kaazing Gateway
rem ---------------------------------------------------------------------------

cd %~dp0


rem A temporary variable for the location of the gateway installation,
rem to allow determining the conf and lib subdirectories (assumed to 
rem be siblings to this script's 'bin' directory).
set GW_HOME=..

rem You can define various Java system properties by setting the value
rem of the GATEWAY_OPTS environment variable before calling this script.
rem The script itself should not be changed. For example, the setting
rem below sets the Java maximum memory to 512MB.
if "%GATEWAY_OPTS%" == "" (
    set GATEWAY_OPTS=-Xmx512m
)

rem You can opt into using early access features by setting the value 
rem of the GATEWAY_FEATURES environment variable to a comma separated 
rem list of features to enable before calling this script.
rem The script itself should not be changed.
set FEATURE_OPTS= 
if not "%GATEWAY_FEATURES%" == "" (
   echo Enabling early access features: %GATEWAY_FEATURES%
   set FEATURE_OPTS=-Dfeature.%GATEWAY_FEATURES:,= -Dfeature.%
)

rem Create the classpath.

rem Set the gateway identifier (required by multiple gateway instances)
set GW_ID=
if "%GATEWAY_IDENTIFIER%" NEQ "" (
    set GW_ID="-Dorg.kaazing.gateway.server.GATEWAY_IDENTIFIER=%GATEWAY_IDENTIFIER%"
)

rem Checking java version
java -version 1>nul 2>nul || (
    echo "Java is not installed. Cannot start the Gateway."
    exit /b 2
)
for /f eol^=J^ tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j%%k"

if %jver% LSS 18 (
  echo "Java 8 or higher must be installed to start the Gateway."
  exit /b 1
)

rem Startup the gateway
java %FEATURE_OPTS% %GATEWAY_OPTS% %GW_ID% -Djava.library.path="%JAVA_LIBRARY_PATH%" -XX:+HeapDumpOnOutOfMemoryError -cp "%GW_HOME%\lib\*" org.kaazing.gateway.server.WindowsMain %*

@REM
@REM Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
@REM 
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM 
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM 
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
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

rem Create the classpath.

rem Add a directory for management support
set JAVA_LIBRARY_PATH=%GW_HOME%\lib\sigar

rem Verify if the gateway identifier was provided (required by Agorna multiple gateway instances)

set GW_ID=

if NOT "%GATEWAY_OPTS%"=="%GATEWAY_OPTS:org.kaazing.gateway.management.AGRONA_ENABLED=true=%" (
    if "%GATEWAY_IDENTIFIER%" NEQ "" (
        set GW_ID="-Dorg.kaazing.gateway.server.GATEWAY_IDENTIFIER=%GATEWAY_IDENTIFIER%"
        
        rem Startup the StatsD publisher
        START CMD /C CALL "scripts/metrics.statsD.start.bat" %GATEWAY_IDENTIFIER%
    )
)

rem Startup the gateway
java %GATEWAY_OPTS% %GW_ID% -Djava.library.path="%JAVA_LIBRARY_PATH%" -XX:+HeapDumpOnOutOfMemoryError -cp "%GW_HOME%\lib\*" org.kaazing.gateway.server.WindowsMain %*

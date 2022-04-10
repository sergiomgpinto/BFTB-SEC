@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
for %%i in ("%~dp0..") do set "BASEDIR=%%~fi"

:repoSetup
set REPO=


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\repo

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\pt\tecnico\bftb\bftb-contract\1.0.0-SNAPSHOT\bftb-contract-1.0.0-SNAPSHOT.jar;"%REPO%"\io\grpc\grpc-protobuf\1.28.0\grpc-protobuf-1.28.0.jar;"%REPO%"\io\grpc\grpc-api\1.28.0\grpc-api-1.28.0.jar;"%REPO%"\io\grpc\grpc-context\1.28.0\grpc-context-1.28.0.jar;"%REPO%"\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar;"%REPO%"\org\codehaus\mojo\animal-sniffer-annotations\1.18\animal-sniffer-annotations-1.18.jar;"%REPO%"\com\google\guava\guava\28.1-android\guava-28.1-android.jar;"%REPO%"\com\google\guava\failureaccess\1.0.1\failureaccess-1.0.1.jar;"%REPO%"\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflict-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;"%REPO%"\org\checkerframework\checker-compat-qual\2.5.5\checker-compat-qual-2.5.5.jar;"%REPO%"\com\google\j2objc\j2objc-annotations\1.3\j2objc-annotations-1.3.jar;"%REPO%"\com\google\api\grpc\proto-google-common-protos\1.17.0\proto-google-common-protos-1.17.0.jar;"%REPO%"\io\grpc\grpc-protobuf-lite\1.28.0\grpc-protobuf-lite-1.28.0.jar;"%REPO%"\io\grpc\grpc-stub\1.28.0\grpc-stub-1.28.0.jar;"%REPO%"\io\grpc\grpc-netty-shaded\1.28.0\grpc-netty-shaded-1.28.0.jar;"%REPO%"\io\grpc\grpc-core\1.28.0\grpc-core-1.28.0.jar;"%REPO%"\com\google\android\annotations\4.1.1.4\annotations-4.1.1.4.jar;"%REPO%"\io\perfmark\perfmark-api\0.19.0\perfmark-api-0.19.0.jar;"%REPO%"\io\grpc\grpc-testing\1.28.0\grpc-testing-1.28.0.jar;"%REPO%"\junit\junit\4.12\junit-4.12.jar;"%REPO%"\org\hamcrest\hamcrest-core\1.3\hamcrest-core-1.3.jar;"%REPO%"\io\opencensus\opencensus-api\0.24.0\opencensus-api-0.24.0.jar;"%REPO%"\com\google\protobuf\protobuf-java-util\3.11.4\protobuf-java-util-3.11.4.jar;"%REPO%"\com\google\errorprone\error_prone_annotations\2.3.4\error_prone_annotations-2.3.4.jar;"%REPO%"\com\google\code\gson\gson\2.8.6\gson-2.8.6.jar;"%REPO%"\javax\annotation\javax.annotation-api\1.3.2\javax.annotation-api-1.3.2.jar;"%REPO%"\pt\ulisboa\tecnico\sdis\zk-naming\1.0.3\zk-naming-1.0.3.jar;"%REPO%"\org\apache\zookeeper\zookeeper\3.6.0\zookeeper-3.6.0.jar;"%REPO%"\commons-lang\commons-lang\2.6\commons-lang-2.6.jar;"%REPO%"\org\apache\zookeeper\zookeeper-jute\3.6.0\zookeeper-jute-3.6.0.jar;"%REPO%"\org\apache\yetus\audience-annotations\0.5.0\audience-annotations-0.5.0.jar;"%REPO%"\io\netty\netty-handler\4.1.45.Final\netty-handler-4.1.45.Final.jar;"%REPO%"\io\netty\netty-common\4.1.45.Final\netty-common-4.1.45.Final.jar;"%REPO%"\io\netty\netty-buffer\4.1.45.Final\netty-buffer-4.1.45.Final.jar;"%REPO%"\io\netty\netty-transport\4.1.45.Final\netty-transport-4.1.45.Final.jar;"%REPO%"\io\netty\netty-resolver\4.1.45.Final\netty-resolver-4.1.45.Final.jar;"%REPO%"\io\netty\netty-codec\4.1.45.Final\netty-codec-4.1.45.Final.jar;"%REPO%"\io\netty\netty-transport-native-epoll\4.1.45.Final\netty-transport-native-epoll-4.1.45.Final.jar;"%REPO%"\io\netty\netty-transport-native-unix-common\4.1.45.Final\netty-transport-native-unix-common-4.1.45.Final.jar;"%REPO%"\org\slf4j\slf4j-api\1.7.25\slf4j-api-1.7.25.jar;"%REPO%"\org\slf4j\slf4j-log4j12\1.7.25\slf4j-log4j12-1.7.25.jar;"%REPO%"\log4j\log4j\1.2.17\log4j-1.2.17.jar;"%REPO%"\mysql\mysql-connector-java\8.0.28\mysql-connector-java-8.0.28.jar;"%REPO%"\com\google\protobuf\protobuf-java\3.11.4\protobuf-java-3.11.4.jar;"%REPO%"\pt\tecnico\bftb\bftb-server\1.0.0-SNAPSHOT\bftb-server-1.0.0-SNAPSHOT.jar

set ENDORSED_DIR=
if NOT "%ENDORSED_DIR%" == "" set CLASSPATH="%BASEDIR%"\%ENDORSED_DIR%\*;%CLASSPATH%

if NOT "%CLASSPATH_PREFIX%" == "" set CLASSPATH=%CLASSPATH_PREFIX%;%CLASSPATH%

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS%  -classpath %CLASSPATH% -Dapp.name="bftb-server" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" pt.tecnico.bftb.server.BFTBServerApp %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%

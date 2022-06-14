@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem ---------------------------------------------------------------------------
rem Start script for the CATALINA Server
rem ---------------------------------------------------------------------------

setlocal

rem Guess CATALINA_HOME if not defined
:: 把当前目录设置到一个名为CURRENT_DIR的变量中
set "CURRENT_DIR=%cd%"
:: 如果系统中配置过CATALINA_HOME则跳到gotHome代码段, 正常情况下我们的电脑都没有配置CATALINA_HOME
if not "%CATALINA_HOME%" == "" goto gotHome
:: 把当前目录设置为CATALINA_HOME.
set "CATALINA_HOME=%CURRENT_DIR%"
:: 判断CATALINA_HOME目录下是否存在catalina.bat文件, 如果存在就跳到okHome代码块.(是存在的)
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
cd ..
set "CATALINA_HOME=%cd%"
cd "%CURRENT_DIR%"
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

:: 把catalina.bat文件的的路径赋给一个叫EXECUTABLE的变量, 然后会进一步判断这个路径是否存在, 存在则跳转到okExec代码块, 不存在的话会在控制台输出一些错误信息.
set "EXECUTABLE=%CATALINA_HOME%\bin\catalina.bat"

rem Check that target executable exists
:: 判断这个路径是否存在, 存在则跳转到okExec代码块
if exist "%EXECUTABLE%" goto okExec
:: 不存在的话会在控制台输出一些错误信息.
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec

rem Get remaining unshifted command line arguments and save them in the
:: 把setArgs代码块的返回结果赋值给CMD_LINE_ARGS变量, 这个变量用于存储启动参数.
set CMD_LINE_ARGS=
:setArgs
:: 首先会判断是否有参数, (if ""%1""==""""判断第一个参数是否为空), 如果没有参数则相当于参数项为空.
if ""%1""=="""" goto doneSetArgs
:: 如果有参数则循环遍历所有的参数(每次拼接第一个参数).
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

:: 执行catalina.bat文件, 如果有参数则带上参数.
call "%EXECUTABLE%" start %CMD_LINE_ARGS%

:end

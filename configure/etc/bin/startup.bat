@echo off
setlocal

set "HOME=%CD%"
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk"
set "APPLICATION_NAME=org.devlive.metaforge.server.MetaForge"
set "APPLICATION_PID="

call :check_java_version
call :job_before_echo_basic
call :job_runner_checker_server
call :job_runner_start_server
exit /b

:check_java_version
for /f "tokens=3" %%i in ('"%JAVA_HOME%\bin\java" -version 2^>^&1 ^| findstr "version"') do (
    set java_version=%%i
)
set java_version=%java_version:"=%
for /f "tokens=1 delims=." %%a in ("%java_version%") do set major_version=%%a
if not "%major_version%"=="1" if not "%major_version%"=="11" (
    echo Error: Java version [ %java_version% ] is not supported. Please use Java 1.8 or 11.
    exit /b 1
)
exit /b 0

:get_jvm_conf
set "JVM_CONF="
for /f "tokens=* delims=" %%a in ('findstr /v "^#" "%HOME%\configure\jvm.conf"') do set "JVM_CONF=%JVM_CONF% %%a"
exit /b

:job_before_echo_basic
echo.
echo     Job before echo basic
echo ============================================
echo Runtime home                           | %HOME%
echo Runtime java home                      | %JAVA_HOME%
echo Runtime application name               | %APPLICATION_NAME%
echo ============================================
echo.
exit /b

:job_before_apply_server
for /f "tokens=1" %%a in ('tasklist /fi "imagename eq java.exe" /v ^| findstr /i "%APPLICATION_NAME%"') do (
    set "APPLICATION_PID=%%a"
)
exit /b

:job_runner_checker_server
echo.
echo     Job runner check server
echo ============================================
call :job_before_apply_server
echo Runtime process                        | %APPLICATION_PID%
if "%APPLICATION_PID%"=="" (
    echo Server status                          | stopped
    echo ============================================
    echo.
) else (
    echo Server status                          | running
    echo ============================================
    echo.
    exit 1
)
exit /b

:job_runner_start_server
echo.
echo     Job runner server
echo ============================================
echo Server starting                        | %APPLICATION_NAME%
cd /d "%HOME%"
call :get_jvm_conf
start /B "" "%JAVA_HOME%\bin\javaw" -cp "%HOME%\lib\*" %JVM_CONF% "%APPLICATION_NAME%" --spring.config.location="%HOME%\configure\" > nul 2>&1
timeout /t 5 > nul
call :job_before_apply_server
if "%APPLICATION_PID%"=="" (
    echo Server start failed                    |
) else (
    echo %APPLICATION_PID% > pid
    echo Server start successful                |
)
echo ============================================
echo.
exit /b

@echo off
set "HOME=%CD%"
set "APPLICATION_NAME=org.devlive.metaforge.server.MetaForge"
set "APPLICATION_PID="

call :job_before_echo_basic
call :job_runner_stop_server
exit /b

:job_before_echo_basic
echo.
echo     Job before echo basic
echo ============================================
echo Runtime home                           | %HOME%
echo Runtime application name               | %APPLICATION_NAME%
echo ============================================
echo.
exit /b

:job_before_apply_server
for /f "tokens=1" %%a in ('tasklist /fi "imagename eq java.exe" /v ^| findstr /i "%APPLICATION_NAME%"') do (
    set "APPLICATION_PID=%%a"
)
exit /b

:job_runner_stop_server
echo.
echo     Job runner check server
echo ============================================
call :job_before_apply_server
echo Runtime process                        | %APPLICATION_PID%
if "%APPLICATION_PID%"=="" (
    echo Server status                          | stopped
    echo ============================================
    echo.
    exit 1
) else (
    echo Server stopping                        | %APPLICATION_NAME%
    taskkill /F /PID %APPLICATION_PID%
    if exist "%HOME%\pid" rd /s /q "%HOME%\pid"
    echo Server stopped successfully            |
    echo ============================================
    echo.
)
exit /b

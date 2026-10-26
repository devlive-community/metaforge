@echo off
setlocal EnableDelayedExpansion
set "HOME=%cd%"
set "VERSION=2026.0.0
set "CDN_CENTER=https://repo1.maven.org/maven2/org/devlive/metaforge"

:: 检查并创建临时目录
:: Check and create temporary directory
set "TEMP_DIR=%HOME%\temp"
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
echo ========== Starting installation ==========
echo Version: %VERSION%
echo CDN Center: %CDN_CENTER%
if not exist "%HOME%\plugins" (
    mkdir "%HOME%\plugins"
    echo Created plugins directory
)

:: 使用更通用的下载方法
:: Use more universal download method
set "DOWNLOAD_COMMAND="
where powershell >nul 2>nul
if %errorlevel% equ 0 (
    set "DOWNLOAD_COMMAND=powershell"
) else (
    where curl >nul 2>nul
    if %errorlevel% equ 0 (
        set "DOWNLOAD_COMMAND=curl -L -o"
    ) else (
        where wget >nul 2>nul
        if %errorlevel% equ 0 (
            set "DOWNLOAD_COMMAND=wget -O"
        ) else (
            echo Error: Neither PowerShell, curl, nor wget found.
            exit /b 1
        )
    )
)

:: 使用更通用的解压方法
:: Use more universal unzip method
set "UNZIP_COMMAND="
where tar >nul 2>nul
if %errorlevel% equ 0 (
    set "UNZIP_COMMAND=tar -xzf"
) else (
    where 7z >nul 2>nul
    if %errorlevel% equ 0 (
        set "UNZIP_COMMAND=7z x"
    ) else (
        echo Error: Neither tar nor 7z found.
        exit /b 1
    )
)

for /f "usebackq tokens=*" %%A in (`type "%HOME%\configure\plugin.conf"`) do (
    set "line=%%A"
    if not "!line!"=="" if not "!line:~0,2!"=="--" (
        set "DOWNLOAD_URL=%CDN_CENTER%/!line!/%VERSION%/!line!-%VERSION%-bin.tar.gz"
        set "TEMP_FILE=%TEMP_DIR%\!line!-%VERSION%-bin.tar.gz"

        echo Downloading: !line!
        if "!DOWNLOAD_COMMAND!"=="powershell" (
            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '!DOWNLOAD_URL!' -OutFile '!TEMP_FILE!'"
        ) else (
            %DOWNLOAD_COMMAND% "!TEMP_FILE!" "!DOWNLOAD_URL!"
        )
        if !errorlevel! equ 0 (
            echo Extracting: !line!
            %UNZIP_COMMAND% "!TEMP_FILE!" -C "%HOME%\plugins"
            del "!TEMP_FILE!"
        )
    )
)

:: 清理临时目录
:: Clean up temporary directory
rd /s /q "%TEMP_DIR%" 2>nul
echo ========== Installation complete ==========
endlocal

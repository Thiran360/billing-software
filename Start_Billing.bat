@echo off
cd /d "%~dp0"
if not exist "StoreBillingApp.jar" (
    for %%f in ("StoreBillingApp*.jar") do (
        if not exist "StoreBillingApp.jar" (
            ren "%%f" "StoreBillingApp.jar" >nul 2>nul
        )
    )
)
set "TARGET_JAR=StoreBillingApp.jar"
if not exist "%TARGET_JAR%" (
    for %%f in (*billing*.jar *.jar) do (
        set "TARGET_JAR=%%f"
        goto :found
    )
)
:found
where javaw >nul 2>nul
if %ERRORLEVEL% equ 0 (
    start javaw -jar "%TARGET_JAR%"
) else (
    echo javaw not found, using java...
    start java -jar "%TARGET_JAR%"
)
exit

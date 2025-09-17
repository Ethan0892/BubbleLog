@echo off
echo Building BubbleLog plugin...
call gradlew.bat clean shadowJar

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo Plugin JAR: build\libs\BubbleLog-1.0.0.jar
    echo.
    echo Copy this file to your Velocity plugins folder.
) else (
    echo.
    echo Build failed! Check the output above for errors.
)

pause

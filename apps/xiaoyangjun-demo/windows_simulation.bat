@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\..\scripts\windows_simulation.ps1" -App xiaoyangjun-demo %*
exit /b %ERRORLEVEL%

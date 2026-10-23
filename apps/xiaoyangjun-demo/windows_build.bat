@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\..\scripts\windows_build.ps1" -App xiaoyangjun-demo %*
exit /b %ERRORLEVEL%

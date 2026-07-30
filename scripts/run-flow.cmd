@echo off
REM Wrapper: UTF-8 + bypass ExecutionPolicy (không đổi cấu hình Windows)
chcp 65001 >nul
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-flow.ps1" %*
exit /b %ERRORLEVEL%

@echo off
REM Wrapper: UTF-8 + bypass ExecutionPolicy
REM Chạy từ IntelliJ: tự mở cửa sổ CMD mới nếu cần (menu ↑↓)
chcp 65001 >nul
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0chay.ps1" %*
exit /b %ERRORLEVEL%

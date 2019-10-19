@echo off
setlocal

set "BITTRANSFER_URL=https://dist.nuget.org/win-x86-commandline/latest/nuget.exe"
set "BITTRANSFER_SAVEAS=nuget.exe"

if not exist nuget.exe  powershell " Import-Module BitsTransfer; Start-BitsTransfer '%BITTRANSFER_URL%' '%BITTRANSFER_SAVEAS%' "
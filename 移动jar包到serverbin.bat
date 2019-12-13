@echo off
chcp 65001
set currentDir=%~dp0
cd %~dp0

del %currentDir%..\serverbin\*.jar

for /R %%s in (*) do (
	if %%~xs == .jar (
		echo 复制协议:%%~nxs
		move %%s %currentDir%..\serverbin
	)
)

pause

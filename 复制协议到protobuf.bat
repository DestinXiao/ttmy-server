@echo off
chcp 65001
set currentDir=%~dp0
cd %~dp0

del %currentDir%..\ServerProtocol\protos\*.proto

cd src
for /R %%s in (*) do (
	if %%~xs == .proto (
		echo 复制协议:%%~nxs
		copy %%s %currentDir%..\ServerProtocol\protos\
	)
)

pause

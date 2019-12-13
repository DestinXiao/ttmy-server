@echo off
chcp 65001
set proto_dir=%~dp0
cd %~dp0

cd ..\src\main\resources\protos

for /R %%s in (*.proto) do (
	echo 编译协议:%%~nxs
	%proto_dir%protoc -I=. --java_out=..\..\java %%~nxs
)

pause

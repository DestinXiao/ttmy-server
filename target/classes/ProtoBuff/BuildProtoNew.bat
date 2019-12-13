@echo off

rem 删除当前目录的proto文件夹中所有文件
del *.proto /q
rem 拷贝protos到当前目录的中
copy ..\protos\*.proto .\ /y

rem 查找文件
for /f "delims=" %%i in ('dir /b "*.proto"') do echo proto\%%~ni
rem 转
for /f "delims=" %%i in ('dir /b/a "*.proto"') do   "tool/ProtoGen/ProtoGen" -i:%%i -o:ProtoOut\%%~ni.cs -t:tool/ProtoGen/csharp.xslt

pause
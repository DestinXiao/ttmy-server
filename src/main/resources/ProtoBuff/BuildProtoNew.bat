@echo off

rem ɾ����ǰĿ¼��proto�ļ����������ļ�
del *.proto /q
rem ����protos����ǰĿ¼����
copy ..\protos\*.proto .\ /y

rem �����ļ�
for /f "delims=" %%i in ('dir /b "*.proto"') do echo proto\%%~ni
rem ת
for /f "delims=" %%i in ('dir /b/a "*.proto"') do   "tool/ProtoGen/ProtoGen" -i:%%i -o:ProtoOut\%%~ni.cs -t:tool/ProtoGen/csharp.xslt

pause
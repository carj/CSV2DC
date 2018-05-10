@ECHO OFF
:: ------------------------------------------------------------------
::  JavaHome.bat - search for and set JAVA_HOME
::  1. If JAVA_HOME is set in system environment, do nothing else.
::  2. If javahome.txt already exists, use that value
::  3. If those fail, search parent directories for java.exe
::  4. Finally, try looking in the registry for other Java installations
::  Note- Script does not add trailing slash to JAVA_HOME variable
::  Note- JBINARY var can be set to JRE or JDK detection
:: ------------------------------------------------------------------
SET TITLE=JavaHome.bat
TITLE=%TITLE%
SETLOCAL ENABLEDELAYEDEXPANSION
SET JDKBIN=\bin
SET JREBIN=\jre\bin
SET JBINARY=%JREBIN%\java.exe
SET SCRIPTDIR=%~dp0
SET SCRIPTDIR=%SCRIPTDIR:~0,-1%
IF EXIST "%SCRIPTDIR%\javahome.txt" (
  ECHO.
  GOTO :LOCKFILE
)
:: search environment section
IF DEFINED JAVA_HOME (
  ECHO.
  CALL :STRIP "!JAVA_HOME!">"!SCRIPTDIR!\javahome.txt"
  GOTO :END
)
SET "dir=%~f0"
:DIRLOOP
CALL :FGETDIR "%dir%"
IF EXIST "%dir%\%JBINARY%" (
  ECHO Parent directory search found JAVA_HOME at %dir%
  GOTO :SEARCHSET
)
IF "%dir:~-1%" == ":" (
  ECHO Parent directory search reached root and "%JBINARY%" was not found.
  GOTO :REGISTRY
)
GOTO :DIRLOOP
:SEARCHSET
SET JAVA_HOME=%dir%
ECHO %JAVA_HOME%>javahome.txt
GOTO :END
:REGISTRY
:: registry search section
:: runs only when JAVA_HOME not set, file search fails, and javahome.txt doesn't exist
ECHO Searching registry for JAVA_HOME...
ECHO. 2>merged.txt
ECHO. 2>list.txt
ECHO. 2>uniquelist.txt
IF NOT EXIST reg32.txt ECHO. 2>reg32.txt
IF NOT EXIST reg64.txt ECHO. 2>reg64.txt
START /w REGEDIT /e reg32.txt "HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\JavaSoft\Java Development Kit"
TYPE reg32.txt | FIND "JavaHome" > merged.txt
START /w REGEDIT /e reg64.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit"
TYPE reg64.txt | FIND "JavaHome" >> merged.txt
FOR /f "tokens=2 delims==" %%x IN (merged.txt) DO (
  CALL :STRIP "%%~x" >>list.txt
)
FOR /F "tokens=* delims= " %%a IN (list.txt) DO (
  SET str=%%a
  FIND /I ^"!str!^" list.txt>nul
  FIND /I ^"!str!^" uniquelist.txt>nul
  IF ERRORLEVEL 1 ECHO !str!>>uniquelist.txt
)
:PROMPT
ECHO Select a JDK from the list:
SET /A COUNT=0
FOR /f "tokens=1,* delims=" %%y IN (uniquelist.txt) DO (
  SET /A COUNT += 1
  ECHO    !COUNT!: %%~y
)
SET /P NUMBER=Type a number here: 
IF "%NUMBER%" GTR "%COUNT%" GOTO :PROMPT
SET /A COUNT=0
FOR /f "tokens=1,* delims=" %%z IN (uniquelist.txt) DO (
  SET /A COUNT += 1
  IF !COUNT!==!NUMBER! (
    SET JAVA_HOME=%%~z
  )
)
ECHO %JAVA_HOME%>javahome.txt
GOTO CLEANUP

:: batch functions section
:FGETDIR
SET "dir=%~dp1"
SET "dir=%dir:~0,-1%"
EXIT /B 0
:STRIP
REM Strip quotes and extra backslash from string
SET n=%~1
SET n=%n:\\=\%
SET n=%n:"=%
IF NOT "%n%"=="" ECHO %n%
GOTO :EOF
:: cleanup and end
:CLEANUP
REM cleanup of registry search
DEL /Q merged.txt
DEL /Q list.txt
DEL /Q uniquelist.txt
DEL /Q reg32.txt
DEL /Q reg64.txt
GOTO :LOCKFILE
:: if all fails
:FAILED
IF NOT DEFINED JAVA_HOME (
  ECHO Error: JAVA_HOME not set in system vars, file search failed, && javahome.txt didn't exist.
  GOTO :END
)
:LOCKFILE
ECHO.
SET /P JAVA_HOME=<"%SCRIPTDIR%\javahome.txt"
:END

"%JAVA_HOME%\bin\java" -cp .;lib/* CSV2Metadata %*




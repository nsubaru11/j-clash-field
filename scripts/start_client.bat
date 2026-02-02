@echo off
chcp 65001 > nul
echo Starting Othello Client...

rem JDKの設定
set "CORRETTO_HOME=C:\Program Files\Amazon Corretto\jdk1.8.0_472"

rem Corretto の存在チェック
if exist "%CORRETTO_HOME%\bin\javac.exe" (
    echo Using bundled JDK: %CORRETTO_HOME%
    set "JAVA_HOME=%CORRETTO_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac"
    set "JAVA_CMD=%JAVA_HOME%\bin\java"
) else (
    echo Bundled JDK not found. Using system default Java.
    set "JAVAC_CMD=javac"
    set "JAVA_CMD=java"
)

rem ディレクトリ設定と移動
for %%i in ("%~dp0..") do set "REPO_DIR=%%~fi"
cd /d "%REPO_DIR%"
set "SRC_DIR=%REPO_DIR%\src"
set "OUT_DIR=%REPO_DIR%\out\production\online-action-game-netprog"

rem コンパイル
echo Compiling...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

rem クライアントに必要なソースファイルをリストアップ (model, network, client)
dir /s /b "%SRC_DIR%\model\*.java" "%SRC_DIR%\network\*.java" "%SRC_DIR%\client\*.java" > "%REPO_DIR%\sources.txt"

"%JAVAC_CMD%" -encoding UTF-8 -d "%OUT_DIR%" @"%REPO_DIR%\sources.txt"

if %errorlevel% neq 0 (
    echo [Error] Compilation failed.
    pause
    exit /b %errorlevel%
)

rem 一時ファイルの削除
del "%REPO_DIR%\sources.txt"

rem 画像ファイル(assets)のコピー
echo Copying resources...
xcopy /E /I /Y "%REPO_DIR%\resources" "%OUT_DIR%\resources" > nul

if %errorlevel% neq 0 (
    echo [Warning] Failed to copy resources.
)

rem 実行
echo Starting Client Application...
"%JAVA_CMD%" -cp "%OUT_DIR%" client.ClientMain

pause

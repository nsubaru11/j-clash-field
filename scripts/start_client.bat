@echo off
chcp 65001 > nul
echo Starting Othello Client...

rem ---------------------------------------------------------
rem JDKの設定
rem ---------------------------------------------------------
rem パスをクォート付きで保持
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

rem ---------------------------------------------------------
rem ディレクトリ設定と移動
rem ---------------------------------------------------------
rem プロジェクトルートディレクトリを取得
for %%i in ("%~dp0..") do set "REPO_DIR=%%~fi"

rem 作業ディレクトリをプロジェクトルートに移動
cd /d "%REPO_DIR%"

set "SRC_DIR=%REPO_DIR%\src"
set "OUT_DIR=%REPO_DIR%\out\production\online-action-game-netprog"

rem ---------------------------------------------------------
rem コンパイル
rem ---------------------------------------------------------
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

rem ---------------------------------------------------------
rem ★ここが修正点: 画像ファイル(assets)のコピー
rem ---------------------------------------------------------
echo Copying resources...
rem /E: ディレクトリ構造ごとコピー
rem /I: 送り側がディレクトリなら受け側もディレクトリとみなす
rem /Y: 上書き確認なし
xcopy /E /I /Y "%SRC_DIR%\resorces" "%OUT_DIR%\resorces" > nul

if %errorlevel% neq 0 (
    echo [Warning] Failed to copy resources.
)

rem ---------------------------------------------------------
rem 実行
rem ---------------------------------------------------------
echo Starting Client Application...
"%JAVA_CMD%" -cp "%OUT_DIR%" client.controller.GameClient

pause

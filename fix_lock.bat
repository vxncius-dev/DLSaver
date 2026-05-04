@echo off
setlocal
cd /d "%~dp0"

set "R_DIR=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources"
set "R_JAR=%R_DIR%\R.jar"

echo Parando daemons do Gradle...
call gradle --stop

echo Encerrando processos que podem travar o R.jar...
taskkill /F /IM gradle.exe /T >nul 2>&1
taskkill /F /IM java.exe /T >nul 2>&1

echo Limpando intermediario travado...
if exist "%R_JAR%" attrib -R "%R_JAR%" >nul 2>&1
if exist "%R_DIR%" rmdir /S /Q "%R_DIR%"

if exist "%R_JAR%" (
    echo Nao foi possivel remover %R_JAR%.
    echo Feche Android Studio, Preview/Live Edit e tente novamente.
    exit /b 1
)

echo Recompilando...
call gradle assembleDebug
exit /b %errorlevel%

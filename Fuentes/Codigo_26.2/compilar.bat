@echo off
cd /d "%~dp0"
echo Compilando Cornbread Finder para Minecraft 26.2. Se necesita JDK 25.
call gradlew.bat build
if errorlevel 1 (
  echo No se pudo compilar. Revisa el error de arriba y JAVA_HOME.
) else (
  echo Listo: build\libs\cornbread-finder-0.6.1.jar
)
pause

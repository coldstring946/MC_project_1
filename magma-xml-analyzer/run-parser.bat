@echo off
echo Starting XML to HQDM Parser...
echo.

REM Compile the Java files
echo Compiling...
mvn clean compile

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running parser on 333 sampled XML files...
echo This may take a few minutes...
echo.

REM Run the parser
java -cp "target\classes;%USERPROFILE%\.m2\repository\uk\gov\gchq\magma-core\core\4.0.1-SNAPSHOT\core-4.0.1-SNAPSHOT.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-simple\2.0.9\slf4j-simple-2.0.9.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar;%USERPROFILE%\.m2\repository\org\apache\jena\jena-core\4.10.0\jena-core-4.10.0.jar;%USERPROFILE%\.m2\repository\org\apache\jena\jena-arq\4.10.0\jena-arq-4.10.0.jar" XMLToHQDMParser

echo.
echo Parser completed! Check the output files:
echo   - parsing-summary.txt
echo   - articles-basic.ttl
echo.
pause
@echo off
echo Simple Test Runner
echo ==================

echo Compiling...
mvn clean compile -q

echo.
echo Running application...
mvn exec:java -Dexec.mainClass="uk.gov.gchq.magma.xml.test.Main" -q

pause
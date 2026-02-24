@echo off
echo Starting API Gateway with reactive configuration...
cd /d "C:\Users\dodoo\OneDrive\Documents\gigs\primedealer-backend"
echo Current directory: %cd%
echo.
echo Building project...
call mvnw.cmd clean compile -pl api-gateway -q
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b %errorlevel%
)
echo Build successful!
echo.
echo Starting application...
call mvnw.cmd spring-boot:run -pl api-gateway
pause

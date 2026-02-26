# Quick Start Script for Expense Tracker Backend
# This script sets up JAVA_HOME and starts the Spring Boot application

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Expense Tracker Backend Startup" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Set JAVA_HOME
$javaHome = "C:\Program Files\Java\jdk-25"
if (Test-Path $javaHome) {
    $env:JAVA_HOME = $javaHome
    Write-Host "✓ JAVA_HOME set to: $javaHome" -ForegroundColor Green
} else {
    Write-Host "✗ Java not found at: $javaHome" -ForegroundColor Red
    Write-Host "  Please update the path in this script or install Java" -ForegroundColor Yellow
    exit 1
}

# Check if PostgreSQL is running
Write-Host ""
Write-Host "Checking PostgreSQL connection..." -ForegroundColor Yellow
$pgTest = Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue

if ($pgTest.TcpTestSucceeded) {
    Write-Host "✓ PostgreSQL is running on port 5432" -ForegroundColor Green
} else {
    Write-Host "✗ PostgreSQL is NOT running on port 5432" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please start PostgreSQL:" -ForegroundColor Yellow
    Write-Host "  Option 1: Start PostgreSQL service in Windows Services" -ForegroundColor White
    Write-Host "  Option 2: Run Docker: docker start expense-tracker-db" -ForegroundColor White
    Write-Host "  Option 3: Create new Docker container:" -ForegroundColor White
    Write-Host "    docker run --name expense-tracker-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=expense_tracker -p 5432:5432 -d postgres:15" -ForegroundColor Gray
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
}

# Start the application
Write-Host ""
Write-Host "Starting Spring Boot application..." -ForegroundColor Yellow
Write-Host "Server will be available at: http://localhost:8080" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Gray
Write-Host ""

./mvnw spring-boot:run -DskipTests

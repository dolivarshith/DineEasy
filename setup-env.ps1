$url = "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
$output = "d:\projects\DineEasy\maven.zip"
$target = "d:\projects\DineEasy\.maven"

# 1. Download and setup Maven
if (-not (Test-Path "$target\apache-maven-3.9.9\bin\mvn.cmd")) {
    Write-Host "Creating target folder..."
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    Write-Host "Downloading Maven 3.9.9 from Apache Archives..."
    Invoke-WebRequest -Uri $url -OutFile $output
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $output -DestinationPath $target
    Write-Host "Cleaning up ZIP file..."
    Remove-Item $output
    Write-Host "Maven installed successfully at $target\apache-maven-3.9.9\bin\mvn.cmd"
} else {
    Write-Host "Maven is already configured at $target\apache-maven-3.9.9\bin\mvn.cmd"
}

# 2. Start MySQL Service from XAMPP if not already running
$mysqlRunning = Get-Process -Name "mysqld" -ErrorAction SilentlyContinue
if ($mysqlRunning) {
    Write-Host "MySQL is already running."
} else {
    Write-Host "Starting XAMPP MySQL daemon in background..."
    Start-Process -FilePath "C:\xampp\mysql\bin\mysqld.exe" -ArgumentList "--defaults-file=C:\xampp\mysql\bin\my.ini", "--standalone" -WindowStyle Hidden
    Start-Sleep -Seconds 3
    
    # Verify connection
    $mysqlRunningAfter = Get-Process -Name "mysqld" -ErrorAction SilentlyContinue
    if ($mysqlRunningAfter) {
        Write-Host "MySQL started successfully."
    } else {
        Write-Warning "Could not start MySQL daemon. Please verify XAMPP configurations manually."
    }
}

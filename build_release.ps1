$ErrorActionPreference = 'Stop'

Write-Host "=== Building Client Release ===" -ForegroundColor Cyan

$releaseDir = "MustafaBilling_Client_Release"

# Clean old release if exists
if (Test-Path $releaseDir) {
    Remove-Item -Path $releaseDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
New-Item -ItemType Directory -Force -Path "$releaseDir\lib" | Out-Null
New-Item -ItemType Directory -Force -Path "$releaseDir\bin" | Out-Null

# 1. Download/Copy Dependencies
Write-Host "Setting up dependencies..."
$libFiles = @(
    @{ Url="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar"; Name="sqlite-jdbc-3.41.2.2.jar" },
    @{ Url="https://repo1.maven.org/maven2/com/formdev/flatlaf/3.2.1/flatlaf-3.2.1.jar"; Name="flatlaf-3.2.1.jar" },
    @{ Url="https://repo1.maven.org/maven2/com/google/zxing/core/3.5.3/core-3.5.3.jar"; Name="core-3.5.3.jar" },
    @{ Url="https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.3/javase-3.5.3.jar"; Name="javase-3.5.3.jar" }
)

$classPathArray = @()
foreach ($lib in $libFiles) {
    $targetPath = "$releaseDir\lib\$($lib.Name)"
    $localLibPath = "lib\$($lib.Name)"
    
    if (Test-Path $localLibPath) {
        Copy-Item -Path $localLibPath -Destination $targetPath
    } else {
        Write-Host "Downloading $($lib.Name)..."
        Invoke-WebRequest -Uri $lib.Url -OutFile $targetPath
    }
    $classPathArray += "lib/$($lib.Name)"
}

# 2. Compile Java Source Code
Write-Host "Compiling source code..."
$javaFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$compileClassPath = ($classPathArray | ForEach-Object { "$releaseDir\$_" }) -join ";"

javac -encoding UTF-8 -d "$releaseDir\bin" -cp $compileClassPath $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# 3. Create the JAR File
Write-Host "Packaging into JAR..."
$manifestPath = "$releaseDir\Manifest.txt"
$classPathString = $classPathArray -join " "
"Main-Class: com.store.billing.Main`nClass-Path: $classPathString`n" | Out-File -FilePath $manifestPath -Encoding ASCII

Set-Location "$releaseDir\bin"

# Extract all libraries to make a FAT JAR
Get-ChildItem "..\lib\*.jar" | ForEach-Object {
    jar xf $_.FullName
}
# Remove signature files to prevent security exceptions in the Fat JAR
Remove-Item -Path "META-INF\*.SF" -ErrorAction SilentlyContinue
Remove-Item -Path "META-INF\*.DSA" -ErrorAction SilentlyContinue
Remove-Item -Path "META-INF\*.RSA" -ErrorAction SilentlyContinue

jar cvfm ..\StoreBillingApp.jar ..\Manifest.txt . | Out-Null
Set-Location "..\.."

# Clean up bin and manifest as they are now in the JAR
Remove-Item -Path "$releaseDir\bin" -Recurse -Force
Remove-Item -Path $manifestPath -Force

# 4. Create Launch Script
Write-Host "Creating startup scripts..."
$batContent = "@echo off`ncd /d `"%~dp0`"`nif not exist `"StoreBillingApp.jar`" (`n    for %%f in (`"StoreBillingApp*.jar`") do (`n        if not exist `"StoreBillingApp.jar`" (`n            ren `"%%f`" `"StoreBillingApp.jar`" >nul 2>nul`n        )`n    )`n)`nset `"TARGET_JAR=StoreBillingApp.jar`"`nif not exist `"%TARGET_JAR%`" (`n    for %%f in (*billing*.jar *.jar) do (`n        set `"TARGET_JAR=%%f`"`n        goto :found`n    )`n)`n:found`nwhere javaw >nul 2>nul`nif %ERRORLEVEL% equ 0 (`n    start javaw -jar `"%TARGET_JAR%`"`n) else (`n    echo javaw not found, using java...`n    start java -jar `"%TARGET_JAR%`"`n)`nexit"
$batContent | Out-File -FilePath "$releaseDir\Start_Billing.bat" -Encoding ASCII

# 5. Copy Database
if (Test-Path "store_billing.db") {
    Copy-Item -Path "store_billing.db" -Destination $releaseDir -Force
}
Copy-Item -Path "db\*" -Destination $releaseDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "=== Build Complete! ===" -ForegroundColor Green
Write-Host "Your client package is ready in the '$releaseDir' folder." -ForegroundColor Yellow
Write-Host "Just zip this folder and send it to your client." -ForegroundColor Yellow

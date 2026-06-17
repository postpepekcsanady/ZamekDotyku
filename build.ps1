$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$SdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($SdkRoot)) {
    $SdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}

$JavaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $DefaultJavaHome = 'C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot'
    if (Test-Path -LiteralPath $DefaultJavaHome) {
        $JavaHome = $DefaultJavaHome
    }
}

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw 'JAVA_HOME není nastavené a nepodařilo se najít JDK.'
}

$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome 'bin') + ';' + $env:Path

$BuildTools = Join-Path $SdkRoot 'build-tools\35.0.0'
$AndroidJar = Join-Path $SdkRoot 'platforms\android-35\android.jar'
$Aapt2 = Join-Path $BuildTools 'aapt2.exe'
$D8 = Join-Path $BuildTools 'd8.bat'
$Zipalign = Join-Path $BuildTools 'zipalign.exe'
$Apksigner = Join-Path $BuildTools 'apksigner.bat'
$Javac = Join-Path $JavaHome 'bin\javac.exe'
$Jar = Join-Path $JavaHome 'bin\jar.exe'
$Keytool = Join-Path $JavaHome 'bin\keytool.exe'

foreach ($Tool in @($AndroidJar, $Aapt2, $D8, $Zipalign, $Apksigner, $Javac, $Jar, $Keytool)) {
    if (!(Test-Path -LiteralPath $Tool)) {
        throw "Chybí nástroj: $Tool"
    }
}

$BuildDir = Join-Path $ProjectRoot 'build'
$CompiledRes = Join-Path $BuildDir 'compiled-res.zip'
$GeneratedDir = Join-Path $BuildDir 'generated'
$ClassesDir = Join-Path $BuildDir 'classes'
$DexDir = Join-Path $BuildDir 'dex'
$OutputsDir = Join-Path $BuildDir 'outputs'
$LinkedApk = Join-Path $BuildDir 'linked.apk'
$AlignedApk = Join-Path $OutputsDir 'ZamekDotyku-unsigned-aligned.apk'
$FinalApk = Join-Path $OutputsDir 'ZamekDotyku-debug.apk'
$Keystore = Join-Path $ProjectRoot 'debug.keystore'

$ResolvedBuildDir = [System.IO.Path]::GetFullPath($BuildDir)
$ResolvedProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
if ($ResolvedBuildDir.StartsWith($ResolvedProjectRoot, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $BuildDir)) {
    Remove-Item -LiteralPath $BuildDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $GeneratedDir, $ClassesDir, $DexDir, $OutputsDir | Out-Null

& $Aapt2 compile --dir (Join-Path $ProjectRoot 'res') -o $CompiledRes
& $Aapt2 link `
    -I $AndroidJar `
    --manifest (Join-Path $ProjectRoot 'AndroidManifest.xml') `
    --java $GeneratedDir `
    --min-sdk-version 23 `
    --target-sdk-version 35 `
    --version-code 1 `
    --version-name '0.1.0' `
    -o $LinkedApk `
    $CompiledRes

$Sources = @()
$Sources += Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$Sources += Get-ChildItem -LiteralPath $GeneratedDir -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }

& $Javac -encoding UTF-8 -source 1.8 -target 1.8 -classpath $AndroidJar -d $ClassesDir @Sources

$ClassFiles = Get-ChildItem -LiteralPath $ClassesDir -Recurse -Filter '*.class' | ForEach-Object { $_.FullName }
& $D8 --min-api 23 --lib $AndroidJar --output $DexDir @ClassFiles

$DexFile = Join-Path $DexDir 'classes.dex'
if (!(Test-Path -LiteralPath $DexFile)) {
    throw 'Nepodařilo se vytvořit classes.dex.'
}

Push-Location $DexDir
try {
    & $Jar uf $LinkedApk 'classes.dex'
}
finally {
    Pop-Location
}

if (!(Test-Path -LiteralPath $Keystore)) {
    & $Keytool -genkeypair `
        -keystore $Keystore `
        -storepass android `
        -keypass android `
        -alias androiddebugkey `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname 'CN=Android Debug,O=Android,C=US'
}

& $Zipalign -p -f 4 $LinkedApk $AlignedApk
& $Apksigner sign `
    --ks $Keystore `
    --ks-pass pass:android `
    --key-pass pass:android `
    --out $FinalApk `
    $AlignedApk

& $Apksigner verify --verbose $FinalApk
Write-Host "APK hotové: $FinalApk"

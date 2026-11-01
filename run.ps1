$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force target/classes,target/dependency | Out-Null
    $ktxDependencies = @{
        'jcalendar.jar' = "$env:USERPROFILE\.m2\repository\com\toedter\jcalendar\1.4\jcalendar-1.4.jar"
        'mysql.jar' = "$env:USERPROFILE\.m2\repository\com\mysql\mysql-connector-j\9.4.0\mysql-connector-j-9.4.0.jar"
    }
    foreach ($entry in $ktxDependencies.GetEnumerator()) {
        $destination = Join-Path 'target/dependency' $entry.Key
        if (!(Test-Path -LiteralPath $destination)) {
            if (!(Test-Path -LiteralPath $entry.Value)) {
                throw 'Chua co thu vien. Hay cai Maven va chay mvn package truoc.'
            }
            Copy-Item -LiteralPath $entry.Value -Destination $destination
        }
    }
    $ktxSources = @(Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    javac -encoding UTF-8 --release 17 -cp 'target/dependency/*' -d target/classes $ktxSources
    if ($LASTEXITCODE -ne 0) { throw 'Bien dich khong thanh cong.' }
    java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/dependency/*' vn.ktx.Main
    if ($LASTEXITCODE -ne 0) { throw 'Ung dung ket thuc voi loi.' }
} finally {
    Pop-Location
}

$env:JAVA_HOME = "E:\JDK17\jdk-17.0.20.1+1"
Set-Location -LiteralPath "$PSScriptRoot\backend"
& "E:\Maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run

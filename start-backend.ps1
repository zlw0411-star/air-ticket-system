# 启动后端（不保存本机绝对路径）
# 运行前请在本机环境变量中配置 JAVA_HOME 与 MAVEN_HOME。
# 未设置时给出明确提示并停止，避免用错 JDK 或 Maven。

if (-not $env:JAVA_HOME) {
  Write-Error "缺少环境变量 JAVA_HOME（JDK 17 根目录），请先配置。"
  exit 1
}
if (-not $env:MAVEN_HOME) {
  Write-Error "缺少环境变量 MAVEN_HOME（Maven 3.9+ 根目录），请先配置。"
  exit 1
}
if (-not $env:APP_JWT_SECRET) {
  Write-Error "缺少环境变量 APP_JWT_SECRET（JWT 签名密钥）。可用以下命令生成：`n  [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))"
  exit 1
}

Set-Location -LiteralPath "$PSScriptRoot\backend"
& "$env:MAVEN_HOME\bin\mvn.cmd" spring-boot:run

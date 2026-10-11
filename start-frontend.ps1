Set-Location -LiteralPath "$PSScriptRoot\frontend"
if (-not (Test-Path "node_modules")) {
  npm install
}
npm run dev

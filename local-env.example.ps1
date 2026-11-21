# Copy to local-env.ps1 (git-ignored) and load with:  . .\local-env.ps1
# Local-only values. Never put real production secrets here.
$env:DB_HOST = "localhost"
$env:DB_PORT = "3307"
$env:DB_USERNAME = "noexit"
$env:DB_PASSWORD = "noexit"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:MAIL_HOST = "localhost"
$env:MAIL_PORT = "1025"
$env:MAIL_USERNAME = "dev"
$env:MAIL_PASSWORD = "dev"
# Random local JWT secrets (new each time this file is loaded)
$env:JWT_SECRET = [Convert]::ToBase64String([byte[]](1..64 | ForEach-Object { Get-Random -Maximum 256 }))
$env:JWT_SECRET_RT = [Convert]::ToBase64String([byte[]](1..64 | ForEach-Object { Get-Random -Maximum 256 }))
# Dummy AWS values: the app starts, but S3 uploads will fail locally
$env:AWS_ACCESS_KEY = "local-dummy"
$env:AWS_SECRET_KEY = "local-dummy"
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_BUCKET_NAME = "noexit-local"
Write-Host "NoExit local env loaded"

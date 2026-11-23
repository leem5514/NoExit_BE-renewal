# PreToolUse guard (L2). Exit 2 = block; stderr is shown to Claude.
$ErrorActionPreference = 'Continue'
[Console]::InputEncoding = [Text.Encoding]::UTF8
$e    = [Console]::In.ReadToEnd() | ConvertFrom-Json
$tool = [string]$e.tool_name
$cmd  = [string]$e.tool_input.command
$cwd  = if ($e.cwd) { [string]$e.cwd } else { (Get-Location).Path }
$log  = Join-Path $PSScriptRoot 'guard.log'

function Block([string]$id, [string]$reason) {
  $line = "{0}`tBLOCK`t{1}`t{2}`t{3}" -f (Get-Date -Format s), $tool, $id, ($cmd -replace '\s+', ' ')
  Add-Content -Path $log -Value $line -Encoding UTF8
  [Console]::Error.WriteLine("[guard:$id] $reason")
  exit 2
}

if (-not $cmd) { exit 0 }

$git = '\bgit\s+(-C\s+\S+\s+)?'
$rules = @(
  @('kill-all-java', '(?i)taskkill\b.*[/-]IM\s+"?java', 'Do not kill all java processes; other projects may be running. Find this app''s PID (netstat -ano | findstr :8080) and kill only that PID.'),
  @('kill-all-java', '(?i)\b(pkill|killall)\b.*java|Stop-Process\b.*-Name\s+"?java', 'Kill by PID only, never all java processes.'),
  @('push',          "(?i)${git}push\b", 'Pushing is done by the human. Commit, then stop and report.'),
  @('discard',       "(?i)${git}(reset\s+--hard|clean\s+-\w*f|checkout\s+(--\s|-f\b|\.\s*$)|restore\s+(?!--staged)\S)", 'Discarding uncommitted work is not allowed. Ask the human.'),
  @('rm-recursive',  '(?i)\brm\s+-\w*r|Remove-Item\b.*-Recurse|\b(rd|rmdir)\s+/s', 'Recursive delete is not allowed. Ask the human.'),
  @('db-wipe',       '(?i)docker\b.*\b(down\b.*(\s-v\b|--volumes)|volume\s+(rm|prune)|system\s+prune)', 'Deleting local DB/Redis volumes is not allowed.'),
  @('secret-file',   '(?i)local-env\.ps1', 'local-env.ps1 holds local secrets. Do not read, print, copy or stage it. Env vars are already loaded in this session.')
)
foreach ($r in $rules) { if ($cmd -match $r[1]) { Block $r[0] $r[2] } }

if ($cmd -match "(?i)${git}commit\b") {
  Push-Location $cwd
  $lines = @(git diff HEAD -U0 --no-color 2>$null | Where-Object { $_ -match '^\+(?!\+\+)' } | ForEach-Object { $_.Substring(1) })
  foreach ($f in @(git ls-files --others --exclude-standard 2>$null)) {
    if (Test-Path -LiteralPath $f -PathType Leaf) { $lines += @(Get-Content -LiteralPath $f -ErrorAction SilentlyContinue) }
  }
  Pop-Location
  $secret = '(?i)AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|^.*?(secret|password|passwd|api[_-]?key|token)[\w."''-]*\s*[:=]\s*["'']?(?!\$\{|\$env:)[A-Za-z0-9+/=_\-]{12,}["'']?;?\s*$'
  if ($lines | Where-Object { $_ -match $secret } | Select-Object -First 1) {
    Block 'secret-in-commit' 'A change looks like a hard-coded secret. Move it to an env var (${VAR} in application.yml) and retry. The value is not logged.'
  }
}
exit 0
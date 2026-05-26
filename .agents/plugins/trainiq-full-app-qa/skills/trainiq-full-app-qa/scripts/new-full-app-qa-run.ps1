param(
    [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
    [string]$ProjectRoot = (Resolve-Path ".").Path
)

$qaDir = Join-Path $ProjectRoot "TrainIQ-Project\docs\qa"
$template = Join-Path $qaDir "full-app-qa-run-template.md"
$target = Join-Path $qaDir "full-app-qa-run-$Date.md"

if (-not (Test-Path -LiteralPath $template)) {
    throw "Template not found: $template"
}

if (Test-Path -LiteralPath $target) {
    Write-Output "QA run already exists: $target"
    exit 0
}

Copy-Item -LiteralPath $template -Destination $target
Write-Output "Created QA run: $target"

$env:CONTROL_BOOTSTRAP_OWNER_EMAIL = "owner@example.test"
$env:CONTROL_BOOTSTRAP_TOKEN = "replace-with-one-time-strong-password"
$env:PUBLIC_REGISTRATION_ENABLED = "false"

Write-Host "Run backend once with these env vars, create OWNER, then clear CONTROL_BOOTSTRAP_*."

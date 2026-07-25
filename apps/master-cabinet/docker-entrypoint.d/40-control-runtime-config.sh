#!/bin/sh
set -eu

cat > /usr/share/nginx/html/control-runtime-config.js <<EOF
window.CONTROL_API_BASE = "${CONTROL_API_BASE:-http://localhost:4190}";
EOF

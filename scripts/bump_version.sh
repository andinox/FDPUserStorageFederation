#!/usr/bin/env bash
set -euo pipefail

# Determine project root (directory of this script's parent)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

POM="pom.xml"

# Extract version from pom.xml using Python for robust XML parsing
current_version=$(python3 - <<'PY'
import sys, xml.etree.ElementTree as ET
try:
    tree = ET.parse('pom.xml')
    root = tree.getroot()
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    version = root.find('m:version', ns)
    if version is None or not version.text:
        raise ValueError('No version element found')
    print(version.text)
except Exception as e:
    sys.exit(f'Error parsing pom.xml: {e}')
PY
)

# Validate and bump version
if [[ "$current_version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(.*)$ ]]; then
    major="${BASH_REMATCH[1]}"
    minor="${BASH_REMATCH[2]}"
    patch="${BASH_REMATCH[3]}"
    suffix="${BASH_REMATCH[4]}"
    patch=$((patch + 1))
    new_version="${major}.${minor}.${patch}${suffix}"
else
    echo "Unsupported version format: $current_version" >&2
    exit 1
fi

# Replace versions in files
sed -i "0,/<version>${current_version//\./\.}<\/version>/s//<version>$new_version<\/version>/" "$POM"
sed -i "s/UserStorageFederation-${current_version//\./\.}.jar/UserStorageFederation-$new_version.jar/g" Makefile docker-compose.dev.yml README.md

echo "Bumped version: $current_version -> $new_version"

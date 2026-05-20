#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
element_version="${ELEMENT_WEB_VERSION:-v1.12.17}"
release_name="element-${element_version}"
release_url="https://github.com/element-hq/element-web/releases/download/${element_version}/${release_name}.tar.gz"
asset_dir="${repo_root}/rich-client/src/main/assets/element"
work_dir="$(mktemp -d)"

cleanup() {
    rm -rf "${work_dir}"
}
trap cleanup EXIT

cp "${asset_dir}/matrix-rich-runtime.js" "${work_dir}/matrix-rich-runtime.js"

curl --fail --location --silent --show-error "${release_url}" --output "${work_dir}/${release_name}.tar.gz"
tar -xzf "${work_dir}/${release_name}.tar.gz" -C "${work_dir}"

rm -rf "${asset_dir}"
mkdir -p "${asset_dir}"
cp -R "${work_dir}/${release_name}/." "${asset_dir}/"
cp "${work_dir}/matrix-rich-runtime.js" "${asset_dir}/matrix-rich-runtime.js"

cat > "${asset_dir}/config.json" <<'JSON'
{
    "default_server_config": {
        "m.homeserver": {
            "base_url": "https://matrix-client.matrix.org",
            "server_name": "matrix.org"
        }
    },
    "disable_custom_urls": false,
    "disable_guests": true,
    "default_theme": "light",
    "setting_defaults": {
        "UIFeature.registration": false
    }
}
JSON

if ! grep -q "matrix-rich-runtime.js" "${asset_dir}/index.html"; then
    perl -0pi -e 's#(<script src="bundles/)#<script src="matrix-rich-runtime.js"></script>\n\n    $1#' "${asset_dir}/index.html"
fi

echo "Prepared Element Web ${element_version} runtime assets for rich-client"

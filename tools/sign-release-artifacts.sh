#!/bin/sh
set -eu
: "${AOO_RELEASE_SIGNING_KEY:?AOO_RELEASE_SIGNING_KEY must reference an approved PEM private key}"
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); manifest=${1:-"$root/work/release/SHA512SUMS"}; test -f "$manifest"
openssl dgst -sha512 -sign "$AOO_RELEASE_SIGNING_KEY" -out "$manifest.sig" "$manifest"
openssl dgst -sha512 -verify "${AOO_RELEASE_VERIFY_KEY:?AOO_RELEASE_VERIFY_KEY must reference the approved public key}" -signature "$manifest.sig" "$manifest"

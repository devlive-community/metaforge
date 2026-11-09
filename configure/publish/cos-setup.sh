#!/usr/bin/env bash
# Install the pinned COSCLI and write its temporary credentials configuration (same setup as ToolForge).
#
# Needs COS_SECRET_ID, COS_SECRET_KEY, COS_BUCKET, COS_REGION and RUNNER_TEMP; COS_SESSION_TOKEN is optional.
# Writes $RUNNER_TEMP/coscli and $RUNNER_TEMP/cos.yaml, and exports their paths to later workflow steps.
set -euo pipefail
umask 077
: "${RUNNER_TEMP:?Set the runner temporary directory}"
: "${COS_BUCKET:?Set the COS bucket name}"
: "${COS_REGION:?Set the COS region}"
[[ -n ${COS_SECRET_ID:-} && -n ${COS_SECRET_KEY:-} ]] || { echo 'Missing COS credentials' >&2; exit 1; }
export COSCLI_BIN=${COSCLI_BIN:-$RUNNER_TEMP/coscli}
export COS_CONFIG_PATH=${COS_CONFIG_PATH:-$RUNNER_TEMP/cos.yaml}
if [[ ! -x $COSCLI_BIN ]]; then
  curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' --output "$COSCLI_BIN" https://github.com/tencentyun/coscli/releases/download/v1.0.8/coscli-v1.0.8-linux-amd64
  printf '%s  %s\n' '7165f2ae16c5f7ac495864c963ca574a76e04ec72680d7bc8a8eee3234d8cf91' "$COSCLI_BIN" | sha256sum --check --status
  chmod 700 "$COSCLI_BIN"
fi
python3 - <<'PY'
import json, os, pathlib, re
region = os.environ['COS_REGION']
if not re.fullmatch(r'[a-z]+-[a-z]+', region):
    raise SystemExit('Invalid COS region')
config = {'cos': {'base': {'secretid': os.environ['COS_SECRET_ID'], 'secretkey': os.environ['COS_SECRET_KEY'], 'sessiontoken': os.environ.get('COS_SESSION_TOKEN', ''), 'protocol': 'https', 'disableencryption': 'true', 'disableautofetchbuckettype': 'true'}, 'buckets': [{'name': os.environ['COS_BUCKET'], 'alias': os.environ['COS_BUCKET'], 'region': region, 'endpoint': f'cos.{region}.myqcloud.com'}]}}
# JSON is also valid YAML. Never pass credentials as arguments or print this file.
pathlib.Path(os.environ['COS_CONFIG_PATH']).write_text(json.dumps(config))
PY
if [[ -n ${GITHUB_ENV:-} ]]; then
  printf 'COSCLI_BIN=%s\nCOS_CONFIG_PATH=%s\n' "$COSCLI_BIN" "$COS_CONFIG_PATH" >> "$GITHUB_ENV"
fi

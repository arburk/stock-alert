#!/usr/bin/env bash
#
# Triggers the GitHub Actions workflow run_scheduled.yml via workflow_dispatch.
#
# The GitHub token must be a fine-grained PAT limited to this repository with
# the permission "Actions: Read and write". It is read from, in this order:
#   1. environment variable GH_TOKEN
#   2. file $GH_TOKEN_FILE (default: github-token next to this script, chmod 600,
#      excluded from git via .gitignore)
#
# Crontab example (every working day at minute 21 from 9 to 21 o'clock, host
# local time - set the host to Europe/Zurich to follow daylight saving time):
#   21 9-21 * * 1-5  /path/to/trigger-workflow.sh >> "$HOME/stock-alert-trigger.log" 2>&1
#
# Optional overrides: GH_REPO, GH_WORKFLOW, GH_REF

set -euo pipefail

repo="${GH_REPO:-arburk/stock-alert}"
workflow="${GH_WORKFLOW:-run_scheduled.yml}"
ref="${GH_REF:-main}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
token_file="${GH_TOKEN_FILE:-$script_dir/github-token}"

log() {
  echo "$(date '+%Y-%m-%dT%H:%M:%S%z') $*"
}

token="${GH_TOKEN:-}"
if [[ -z "$token" && -r "$token_file" ]]; then
  token="$(tr -d '[:space:]' < "$token_file")"
fi
if [[ -z "$token" ]]; then
  log "ERROR: no token found, set GH_TOKEN or create $token_file" >&2
  exit 1
fi

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

# The token is passed via stdin config so it does not show up in the process list.
status="$(curl --silent --show-error --max-time 30 \
  --request POST \
  --config - \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28" \
  --output "$response_file" \
  --write-out '%{http_code}' \
  --data "{\"ref\":\"$ref\"}" \
  "https://api.github.com/repos/$repo/actions/workflows/$workflow/dispatches" \
  <<< "header = \"Authorization: Bearer $token\"")"

if [[ "$status" != "204" ]]; then
  log "ERROR: dispatch of $workflow on $repo@$ref failed with HTTP $status" >&2
  cat "$response_file" >&2
  exit 1
fi

log "dispatched $workflow on $repo@$ref"

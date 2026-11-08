#!/usr/bin/env bash
# Decide which CI components must run, based on the files changed in this push or pull request.
#
# Usage:
#   changed-components.sh [options]
#
# Options:
#   --config <file>   Component rules (default: .ci/components.conf)
#   --base <ref>      Diff base commit (default: auto-detected from the CI environment)
#   --head <ref>      Diff head commit (default: HEAD)
#   --files <file>    Read the changed file list from <file> ("-" = stdin) instead of git diff
#   --all             Mark every component as changed (full run)
#   --github-output   Also append the results to "$GITHUB_OUTPUT"
#   --files-of <name> Print only the changed files that trigger component <name>, one per line,
#                     or a single "*" on a full run (e.g. to pick the Maven modules to build)
#   -v, --verbose     Explain which file triggered each component
#
# Config format (one component per line, "#" starts a comment, lines may continue with "\"):
#   <name>: <pattern> [<pattern> ...]
#   _all:   <pattern> ...        # any match forces a full run
#   _ignore: <pattern> ...       # matching files never trigger anything
#
#   A pattern prefixed with "!" excludes files from that component.
#   "*" matches any characters including "/"; "**/" matches zero or more directories.
#   Patterns are matched against repo-relative paths, e.g. "backend/**", "**/*.py", "Cargo.lock".
#
# Output (stdout and, with --github-output, $GITHUB_OUTPUT):
#   <name>=true|false   for every component
#   any=true|false      whether at least one component changed
#   components=[...]    JSON array of changed component names (for a dynamic matrix)
#
# Base auto-detection (when --base is not given):
#   GitHub pull_request : first parent of the merge commit, else merge-base with origin/$GITHUB_BASE_REF
#   GitHub push         : "before" from $GITHUB_EVENT_PATH
#   GitLab merge request: $CI_MERGE_REQUEST_DIFF_BASE_SHA
#   GitLab push         : $CI_COMMIT_BEFORE_SHA
#   anything else, a new branch, or an unknown base -> full run (safe default)
#
# Compatible with bash 3.2 (macOS). Needs only git; jq is used when available.

set -uo pipefail

config=".ci/components.conf"
base=""
head="HEAD"
files_from=""
force_all=0
gh_output=0
verbose=0
files_of=""

die() { echo "error: $*" >&2; exit 2; }
note() { echo "$*" >&2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --config) config="${2:-}"; shift 2 ;;
    --base) base="${2:-}"; shift 2 ;;
    --head) head="${2:-}"; shift 2 ;;
    --files) files_from="${2:-}"; shift 2 ;;
    --all) force_all=1; shift ;;
    --github-output) gh_output=1; shift ;;
    --files-of) files_of="${2:-}"; shift 2 ;;
    -v|--verbose) verbose=1; shift ;;
    -h|--help) sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown option: $1" ;;
  esac
done

[ -f "$config" ] || die "config not found: $config"

# ---------------------------------------------------------------- config parsing
names=()      # component names, in file order
includes=()   # space-separated include patterns per component
excludes=()   # space-separated exclude patterns per component
all_patterns=""
ignore_patterns=""

add_rule() { # <name> <patterns...>
  local name="$1"; shift
  local inc="" exc="" p
  for p in "$@"; do
    case "$p" in
      '!'*) exc="$exc ${p#!}" ;;
      *) inc="$inc $p" ;;
    esac
  done
  case "$name" in
    _all) all_patterns="$all_patterns$inc"; return ;;
    _ignore) ignore_patterns="$ignore_patterns$inc"; return ;;
  esac
  printf '%s' "$name" | grep -Eq '^[A-Za-z][A-Za-z0-9_-]*$' || die "invalid component name: $name"
  local i
  for i in "${!names[@]}"; do
    if [ "${names[$i]}" = "$name" ]; then
      includes[$i]="${includes[$i]}$inc"; excludes[$i]="${excludes[$i]}$exc"; return
    fi
  done
  names+=("$name"); includes+=("$inc"); excludes+=("$exc")
}

pending=""
while IFS= read -r line || [ -n "$line" ]; do
  line="${line%%#*}"
  case "$line" in
    *\\) pending="$pending ${line%\\}"; continue ;;
  esac
  line="$pending $line"; pending=""
  # trim
  line="$(printf '%s' "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
  [ -z "$line" ] && continue
  case "$line" in
    *:*) ;;
    *) die "bad config line (expected 'name: patterns'): $line" ;;
  esac
  name="${line%%:*}"; name="$(printf '%s' "$name" | tr -d '[:space:]')"
  # shellcheck disable=SC2086  # intentional word splitting of the pattern list
  set -f; add_rule "$name" ${line#*:}; set +f
done < "$config"

[ "${#names[@]}" -gt 0 ] || die "no components defined in $config"

# ---------------------------------------------------------------- matching
matches() { # <file> <pattern>; each "**/" means zero or more directories
  local f="$1" p="$2" pre post
  case "$p" in
    *'**/'*)
      pre="${p%%'**/'*}"; post="${p#*'**/'}"
      matches "$f" "$pre$post" && return 0
      matches "$f" "$pre*/$post"; return ;;
  esac
  # shellcheck disable=SC2053  # unquoted RHS is a glob on purpose
  [[ $f == $p ]]
}

matches_any() { # <file> <space-separated patterns>
  local f="$1" p
  set -f
  for p in $2; do
    if matches "$f" "$p"; then set +f; return 0; fi
  done
  set +f
  return 1
}

# ---------------------------------------------------------------- base detection
zero='0000000000000000000000000000000000000000'
reason=""

detect_base() {
  local ev="${GITHUB_EVENT_NAME:-}"
  if [ -n "$ev" ]; then
    case "$ev" in
      pull_request|pull_request_target)
        if git rev-parse -q --verify "$head^2" >/dev/null 2>&1; then
          base="$(git rev-parse "$head^1")"; return
        fi
        if [ -n "${GITHUB_BASE_REF:-}" ]; then
          base="$(git merge-base "origin/$GITHUB_BASE_REF" "$head" 2>/dev/null || true)"; return
        fi ;;
      push)
        if [ -n "${GITHUB_EVENT_PATH:-}" ] && [ -f "$GITHUB_EVENT_PATH" ]; then
          if command -v jq >/dev/null 2>&1; then
            base="$(jq -r '.before // empty' "$GITHUB_EVENT_PATH")"
          else
            base="$(grep -o '"before": *"[0-9a-f]*"' "$GITHUB_EVENT_PATH" | head -n 1 | grep -o '[0-9a-f]\{40\}')"
          fi
        fi
        return ;;
      *) reason="event '$ev' always runs everything"; return ;;
    esac
  fi
  if [ -n "${GITLAB_CI:-}" ]; then
    base="${CI_MERGE_REQUEST_DIFF_BASE_SHA:-${CI_COMMIT_BEFORE_SHA:-}}"; return
  fi
  reason="no --base given and no CI environment detected"
}

changed=""
if [ "$force_all" -eq 1 ]; then
  reason="--all requested"
elif [ -n "$files_from" ]; then
  if [ "$files_from" = "-" ]; then changed="$(cat)"; else changed="$(cat "$files_from")"; fi
else
  [ -z "$base" ] && detect_base
  if [ -z "$reason" ]; then
    if [ -z "$base" ] || [ "$base" = "$zero" ]; then
      reason="no usable diff base (new branch or first push)"
    elif ! git cat-file -e "$base^{commit}" 2>/dev/null; then
      reason="base $base is not in the clone (use fetch-depth: 0 or fetch it)"
    else
      changed="$(git diff --name-only --no-renames "$base" "$head")" || die "git diff failed"
    fi
  fi
fi

# ---------------------------------------------------------------- evaluation
results=()
full=0
own_files=""
for i in "${!names[@]}"; do results[$i]=false; done

if [ -n "$reason" ]; then
  note "full run: $reason"
  full=1
  for i in "${!names[@]}"; do results[$i]=true; done
else
  count=0
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    count=$((count + 1))
    if [ -n "$ignore_patterns" ] && matches_any "$f" "$ignore_patterns"; then
      [ "$verbose" -eq 1 ] && note "  ignored: $f"
      continue
    fi
    if [ -n "$all_patterns" ] && matches_any "$f" "$all_patterns"; then
      note "full run: $f matches an _all pattern"
      full=1
      for i in "${!names[@]}"; do results[$i]=true; done
      break
    fi
    hit=0
    for i in "${!names[@]}"; do
      if matches_any "$f" "${includes[$i]}" && ! matches_any "$f" "${excludes[$i]}"; then
        [ "$verbose" -eq 1 ] && [ "${results[$i]}" = false ] && note "  ${names[$i]} <- $f"
        results[$i]=true; hit=1
        [ "${names[$i]}" = "$files_of" ] && own_files="$own_files$f
"
      fi
    done
    [ "$verbose" -eq 1 ] && [ "$hit" -eq 0 ] && note "  no component: $f"
  done <<EOF
$changed
EOF
  [ "$verbose" -eq 1 ] && note "changed files examined: $count"
fi

# ---------------------------------------------------------------- output
if [ -n "$files_of" ]; then
  if [ "$full" -eq 1 ]; then echo '*'; else printf '%s' "$own_files"; fi
  exit 0
fi

any=false
json=""
out=""
for i in "${!names[@]}"; do
  out="$out${names[$i]}=${results[$i]}
"
  if [ "${results[$i]}" = true ]; then
    any=true
    json="$json${json:+,}\"${names[$i]}\""
  fi
done
out="${out}any=$any
components=[$json]
"

printf '%s' "$out"
if [ "$gh_output" -eq 1 ]; then
  [ -n "${GITHUB_OUTPUT:-}" ] || die "--github-output used but GITHUB_OUTPUT is not set"
  printf '%s' "$out" >> "$GITHUB_OUTPUT"
fi

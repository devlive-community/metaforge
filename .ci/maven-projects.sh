#!/usr/bin/env bash
# Map changed files (stdin, one per line, as printed by `changed-components.sh --files-of java`)
# to the Maven reactor modules that own them.
#
# Prints a comma-separated module list for `-pl`, or nothing when the whole reactor must be built:
# a full run ("*"), or a changed file that belongs to no module (root pom, .mvn, configure/, ...).
#
# Usage: changed-components.sh --files-of java | maven-projects.sh [--github-output]

set -uo pipefail

gh_output=0
[ "${1:-}" = "--github-output" ] && gh_output=1

emit() {
  echo "projects=$1"
  if [ "$gh_output" -eq 1 ]; then echo "projects=$1" >> "$GITHUB_OUTPUT"; fi
  exit 0
}

# Reactor modules, longest path first so a nested module wins over its parent directory.
modules="$(sed -n 's:.*<module>\(.*\)</module>.*:\1:p' pom.xml | awk '{ print length, $0 }' | sort -rn | cut -d' ' -f2-)"

selected=""
while IFS= read -r file; do
  [ -z "$file" ] && continue
  [ "$file" = "*" ] && emit ""
  owner=""
  while IFS= read -r module; do
    case "$file" in "$module"/*) owner="$module"; break ;; esac
  done <<EOF
$modules
EOF
  if [ -z "$owner" ]; then
    echo "whole reactor: $file belongs to no module" >&2
    emit ""
  fi
  case ",$selected," in *",$owner,"*) ;; *) selected="$selected${selected:+,}$owner" ;; esac
done

echo "modules: ${selected:-none}" >&2
emit "$selected"

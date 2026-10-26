#!/usr/bin/env bash
# Spring Boot kata runner. Usage: ./kata.sh <command> [unit]
#   status         show which units are unlocked and which is next
#   next           unlock the next unit (copies its failing tests into src/test/java)
#   unlock A-0N    unlock a specific unit (the previous unit must already be unlocked)
#   test [A-0N]    run every unlocked test, or only one unit's tests
#   verify         prove the kata itself is sound: builds the reference solution
#                  against ALL unit tests in a temp folder (never touches your code)
set -euo pipefail
cd "$(dirname "$0")"

UNITS=(A-01 A-02 A-03 A-04 A-05 A-06 A-07)
TEST_ROOT="src/test/java/dev/brian/kata"

package_of() { echo "$1" | tr -d '-' | tr 'A' 'a'; }        # A-03 -> a03
is_unlocked() { [ -d "$TEST_ROOT/$(package_of "$1")" ]; }
is_known() { for unit in "${UNITS[@]}"; do [ "$unit" = "$1" ] && return 0; done; return 1; }

next_locked_unit() {
    for unit in "${UNITS[@]}"; do
        if ! is_unlocked "$unit"; then echo "$unit"; return; fi
    done
}

unlock() {
    local unit="$1"
    is_known "$unit" || { echo "Unknown unit '$unit'. Units: ${UNITS[*]}"; exit 1; }
    if is_unlocked "$unit"; then echo "$unit is already unlocked."; return; fi
    local i
    for i in "${!UNITS[@]}"; do
        if [ "${UNITS[$i]}" = "$unit" ] && [ "$i" -gt 0 ] && ! is_unlocked "${UNITS[$((i - 1))]}"; then
            echo "Unlock ${UNITS[$((i - 1))]} first. Units go in order."; exit 1
        fi
    done
    mkdir -p src/test/java
    cp -r "kata/units/$unit/test/." src/test/java/
    echo "Unlocked $unit. Its tests are now in $TEST_ROOT/$(package_of "$unit")/"
    echo "Instructions: kata/units/$unit/README.md"
    echo "Run them:     ./kata.sh test $unit"
}

case "${1:-status}" in
    status)
        for unit in "${UNITS[@]}"; do
            if is_unlocked "$unit"; then echo "  [unlocked] $unit"; else echo "  [locked]   $unit"; fi
        done
        next_unit="$(next_locked_unit)"
        if [ -n "$next_unit" ]; then echo "Next to unlock: $next_unit (./kata.sh next)"; else echo "All units unlocked."; fi
        ;;
    next)
        next_unit="$(next_locked_unit)"
        if [ -z "$next_unit" ]; then echo "All units already unlocked."; else unlock "$next_unit"; fi
        ;;
    unlock)
        unlock "${2:?Usage: ./kata.sh unlock A-0N}"
        ;;
    test)
        if [ -n "${2:-}" ]; then
            is_unlocked "$2" || { echo "$2 is not unlocked yet."; exit 1; }
            mvn -B test -Dtest="$(echo "$2" | tr -d '-')*" -Dsurefire.failIfNoSpecifiedTests=false
        else
            mvn -B test
        fi
        ;;
    verify)
        work_dir="$(mktemp -d)"
        cp pom.xml "$work_dir/"
        cp -r kata/reference/src "$work_dir/src"
        mkdir -p "$work_dir/src/test/java"
        for unit in "${UNITS[@]}"; do cp -r "kata/units/$unit/test/." "$work_dir/src/test/java/"; done
        echo "Building the reference solution against all unit tests in $work_dir ..."
        if (cd "$work_dir" && mvn -B -q test); then
            echo "VERIFY PASSED: every kata test is green against the reference solution."
            rm -rf "$work_dir"
        else
            echo "VERIFY FAILED: the kata itself has a problem. Surefire reports: $work_dir/target/surefire-reports"
            exit 1
        fi
        ;;
    *)
        sed -n '2,9p' "$0"; exit 1
        ;;
esac

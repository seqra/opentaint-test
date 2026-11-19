#!/usr/bin/env bash
# Stage analyzer JAR, autobuilder JAR, Go server, Go CLI, and rules from a
# checked-out opentaint tree into an output build/ directory ready to upload
# as a GH Actions artifact. A tree without the Go server gets none.
#
# Usage:
#     build_opentaint.sh <opentaint-checkout> <sha> <output-dir>
#
# Commands mirror opentaint/.github/workflows/ci-analyzer-owasp.yaml and
# ci-cli.yaml. See test-system-design-plan.md §5.1c.

set -euo pipefail

OPENTAINT_DIR=${1:?"path to opentaint checkout required"}
ANALYZER_SHA=${2:?"analyzer sha required"}
OUT_DIR=${3:?"output directory required"}

OPENTAINT_DIR=$(cd "$OPENTAINT_DIR" && pwd)
mkdir -p "$OUT_DIR"
OUT_DIR=$(cd "$OUT_DIR" && pwd)
GO_SERVER_DIR="$OPENTAINT_DIR/core/opentaint-ir/go/go-ssa-server"

echo "==> Building analyzer JAR"
( cd "$OPENTAINT_DIR/core" && ./gradlew --no-daemon :projectAnalyzerJar )

echo "==> Building autobuilder JAR"
( cd "$OPENTAINT_DIR/core" && ./gradlew --no-daemon opentaint-jvm-autobuilder:projectAutoBuilderJar )

if [ -d "$GO_SERVER_DIR" ]; then
    echo "==> Building Go server"
    ( cd "$OPENTAINT_DIR/core" && ./gradlew --no-daemon :opentaint-ir:go:buildGoServer )
fi

echo "==> Building Go CLI"
( cd "$OPENTAINT_DIR/cli" && go build -o opentaint . )

echo "==> Staging build/"
cp "$OPENTAINT_DIR/core/build/libs/opentaint-project-analyzer.jar"                 "$OUT_DIR/"
cp "$OPENTAINT_DIR/core/opentaint-jvm-autobuilder/build/libs/opentaint-project-auto-builder.jar" "$OUT_DIR/"
cp "$OPENTAINT_DIR/cli/opentaint"                                                   "$OUT_DIR/"
if [ -d "$GO_SERVER_DIR" ]; then
    cp "$GO_SERVER_DIR/go-ssa-server"                                               "$OUT_DIR/"
fi
rm -rf "$OUT_DIR/rules"
cp -R "$OPENTAINT_DIR/rules/ruleset"                                                "$OUT_DIR/rules"
echo "$ANALYZER_SHA" > "$OUT_DIR/analyzer_sha.txt"

echo "==> Contents of $OUT_DIR:"
ls -la "$OUT_DIR"

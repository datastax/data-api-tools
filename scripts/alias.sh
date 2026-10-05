function extract_root_dir() {
  dirname "$(dirname "$(realpath "$1")")"
}

if [ -n "$BASH_SOURCE" ]; then
  _CLI_DIR=$(extract_root_dir "${BASH_SOURCE[0]}")
elif [ -n "$ZSH_VERSION" ]; then
  # shellcheck disable=SC2296
  _CLI_DIR=$(extract_root_dir "${(%):-%x}")
else
  _CLI_DIR="."
fi

echo "Resolved docs-helper directory '$_CLI_DIR'"

function dh() {
  export CLI_DIR="$_CLI_DIR"

  "$CLI_DIR/gradlew" \
      --project-dir "$CLI_DIR" \
      -q fatJar && \
      java --enable-native-access=ALL-UNNAMED \
      -jar "$CLI_DIR/build/libs/verifier.jar" \
      "$@"
}

if [ "$1" != "--no-compgen" ]; then
  echo "Loading completions for dh..."
  dh -h >/dev/null
  # shellcheck disable=SC1090
  source <(dh compgen)
fi

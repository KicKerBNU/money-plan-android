#!/usr/bin/env bash
# Creates the Play Store upload keystore and keystore.properties (both gitignored).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE_DIR="$ROOT/release"
KEYSTORE="$RELEASE_DIR/money-plan-upload.keystore"
PROPS="$ROOT/keystore.properties"
ALIAS="money-plan-upload"

if [[ -f "$KEYSTORE" ]]; then
  echo "Keystore already exists: $KEYSTORE"
  echo "Delete it first if you need a new one."
  exit 1
fi

mkdir -p "$RELEASE_DIR"

echo "Create a strong password for your upload keystore."
echo "You must back up the keystore file and password — losing them blocks future updates."
echo ""
read -rsp "Store password: " STORE_PASS
echo ""
read -rsp "Confirm store password: " STORE_PASS_CONFIRM
echo ""

if [[ "$STORE_PASS" != "$STORE_PASS_CONFIRM" ]]; then
  echo "Passwords do not match."
  exit 1
fi

read -rsp "Key password (Enter to use same as store password): " KEY_PASS
echo ""
if [[ -z "$KEY_PASS" ]]; then
  KEY_PASS="$STORE_PASS"
fi

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=Money Plan, OU=Android, O=Money Plann, C=US"

cat > "$PROPS" <<EOF
storeFile=release/money-plan-upload.keystore
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$KEY_PASS
EOF

chmod 600 "$PROPS"

echo ""
echo "Created:"
echo "  $KEYSTORE"
echo "  $PROPS"
echo ""
echo "Upload key SHA-1 (add to Firebase → Android app → com.moneyplann.app):"
keytool -list -v -keystore "$KEYSTORE" -alias "$ALIAS" -storepass "$STORE_PASS" | rg "SHA1:"

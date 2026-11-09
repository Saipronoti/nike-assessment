#!/usr/bin/env sh
set -eu

product_count="${1:-100}"

case "$product_count" in
  ''|*[!0-9]*)
    echo "Usage: $0 [positive-product-count]" >&2
    exit 2
    ;;
esac

if [ "$product_count" -lt 1 ]; then
  echo "Product count must be at least 1." >&2
  exit 2
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

docker compose exec -T postgres \
  psql -v ON_ERROR_STOP=1 \
    -U "${POSTGRES_USER:-catalog}" \
    -d "${POSTGRES_DB:-catalog}" \
    -v product_count="$product_count" \
    -f - < "$script_dir/seed-products.sql"

echo "Inserted $product_count sample products."

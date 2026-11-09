# Product Catalog API

An initial Spring Boot 3 / Java 21 REST API draft for an e-commerce product catalog backed by PostgreSQL.

## Run locally

Start PostgreSQL:

```sh
docker compose up -d
```

Run the service (requires Maven and Java 21):

```sh
mvn spring-boot:run
```

Spring Boot DevTools is enabled for local development. When your IDE recompiles changed files, DevTools automatically restarts the application. In IntelliJ IDEA, enable **Build project automatically**; while the application is running, also enable **compiler.automake.allow.when.app.running** in the Registry (`Cmd+Shift+A` → `Registry`). With other IDEs, configure automatic builds or compile the project after edits. This is a fast application restart, not JVM hot code replacement. Refresh Swagger UI after the restart to load the updated API documentation.

Open Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) to browse and try the API. The generated OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`. Start PostgreSQL first, since the application connects to the database during startup.

The database schema starts empty. Once PostgreSQL is healthy and migrations have run, seed 100 products for trying the list and filter endpoints:

```sh
./scripts/seed-products.sh
```

Pass a count to seed a larger local dataset, such as 50,000 products:

```sh
./scripts/seed-products.sh 50000
```

Seeding appends products; it does not clear or replace existing rows. Re-running the script inserts that many additional products.

The default local database connection is `jdbc:postgresql://localhost:5432/catalog` with username/password `catalog` / `catalog`. Override these with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in other environments. Flyway applies the schema migration at startup; Hibernate validates it.

## API

Base path: `/api/v1/products`

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/api/v1/products` | Create a product; returns `201 Created` |
| `GET` | `/api/v1/products/{id}` | Retrieve one product; returns `404` if absent |
| `GET` | `/api/v1/products` | List products, optionally filtered and paginated |
| `PUT` | `/api/v1/products/{id}` | Replace a product's editable fields |
| `PATCH` | `/api/v1/products/{id}` | Partially update supplied fields; omitted fields stay unchanged |
| `DELETE` | `/api/v1/products/{id}` | Delete a product; returns `204 No Content` |

List parameters: `category`, inclusive `minPrice` / `maxPrice`, zero-based `page` (default `0`), `size` (default `20`, maximum `100`), and `sort` (default `id,asc`). Omit the filter parameters to browse all products in pages, for example `GET /api/v1/products?page=0&size=20`. Supported sort fields are `id`, `name`, `category`, `price`, `createdAt`, `updatedAt`; direction can be `asc` or `desc`. An ID tie-breaker is added for deterministic ordering. Filtered example:

```sh
curl 'http://localhost:8080/api/v1/products?category=shoes&minPrice=50&maxPrice=150&page=0&size=20&sort=price,asc'
```

Create/update bodies require `name`, `description`, `category`, `price` (non-negative), and `stockQuantity` (non-negative). Example:

```json
{
  "name": "Everyday Runner",
  "description": "Lightweight road running shoe",
  "category": "shoes",
  "price": 89.99,
  "stockQuantity": 42
}
```

PATCH accepts any non-empty subset of those editable fields. Omitted fields stay unchanged; explicit `null`, unknown properties, blank text, negative values, and prices that do not fit `NUMERIC(12,2)` are rejected with `400 Bad Request`. PATCH example:

```json
{ "price": 94.99 }
```

Errors use a consistent JSON body with `timestamp`, `status`, `error`, `message`, `path`, and `fieldErrors`. Missing products return `404`; validation and malformed requests return `400`; concurrent updates detected by JPA optimistic locking return `409`.

## Draft design notes

- PostgreSQL is a good starting point for a few hundred thousand catalog rows and read-heavy access. The `(category, price)` index supports the main combined filter, while the primary key supports ID lookups and stable default ordering.
- Page-number pagination is straightforward for the initial API. For deep pages at higher traffic, consider cursor/keyset pagination with a stable sort key to avoid large offsets.
- The entity includes a JPA version field for optimistic concurrency tracking. A later API revision can expose an ETag or version precondition for safe concurrent edits.
- Add authentication/authorization, rate limiting, observability, and production secret management before exposing the service publicly.

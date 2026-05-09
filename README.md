# Billing Server — Fine Fusion Textile

Spring Boot 4 backend for the Fine Fusion Textile billing & document
management system. Pairs with the React frontend at
[billing_client](https://github.com/zaidhassan282/billing_client).

## What it does

A REST API for a textile dyeing service. The full operational pipeline:

```
Permanent Party (customer master)
       │
       ▼
   Contract  ───────────────────────────────────┐
       │                                        │
       ▼                                        │
Inward Gate Pass  ──→  Inventory (GREIGE)       │
       │                    │                   │
       │                    ▼                   │
       │            Issue to Dyeing             │
       │                    │                   │
       │                    ▼                   │
       │            (off-site dyeing)           │
       │                    │                   │
       │                    ▼                   │
       │            Dyed Receive  ──→  Inventory (DYED)
       │                                        │
       ▼                                        ▼
   Order  ──→  status workflow  ──→  Outward Gate Pass
   IN_PROGRESS → TESTING → READY_FOR_DELIVERY → DELIVERED
```

Stock is **scoped per contract** — material received against contract A
is never confused with material from contract B. All quantities are
tracked in **kg, rolls, and meters** simultaneously, with validation at
every transition.

Every create / update / delete on every entity is recorded in an
**audit log** with full before/after JSON snapshots, so the frontend
Logs page can re-render any saved gate pass exactly as it was.

## Tech stack

- Java 21
- Spring Boot 4.0.5 (web-mvc + data-jpa + security)
- PostgreSQL (production) / H2 (tests only)
- Lombok for entity boilerplate
- Jackson for JSON
- Maven build, Docker for deploy

## Project layout

```
src/main/java/com/billing/system/
├── SystemApplication.java         (entry point)
├── config/                        (CORS, security, Jackson)
├── controller/                    (REST endpoints)
├── service/                       (business logic + transactions)
├── repository/                    (Spring Data JPA repos)
├── entity/                        (JPA entities)
├── dto/                           (request/response shapes)
└── enums/                         (FabricStage, MovementType, OrderStatus)
```

## Endpoints (high-level)

| Path                          | Purpose                                  |
| ----------------------------- | ---------------------------------------- |
| `/permanent-table`            | Customer master CRUD                     |
| `/contracts`, `/contracts-table` | Contract CRUD                          |
| `/inward`                     | Inward gate-pass save + list             |
| `/outward`                    | Outward gate-pass save (issue/return)    |
| `/dyeing/issue`               | Issue greige stock to dyeing             |
| `/dyed/receive`               | Receive dyed material                    |
| `/inventory`                  | Read-only stock view                     |
| `/orders`                     | Order workflow + status changes          |
| `/entries`                    | Daily billing entries                    |
| `/audit`                      | Audit log (paginated, filterable)        |
| `/admin/snapshot`             | One-shot dump of every entity (archive)  |

## Local development

```bash
# Prereqs: Java 21, Maven (wrapper included), Postgres running locally
createdb billing                     # or: psql -c "CREATE DATABASE billing;"

# Defaults connect to jdbc:postgresql://localhost:5432/billing as postgres/postgres
./mvnw spring-boot:run               # backend listens on :8080
```

To override DB connection, set env vars before starting:

```bash
export DB_URL=jdbc:postgresql://my-host/my-db
export DB_USERNAME=...
export DB_PASSWORD=...
```

CORS allow-list (optional in addition to the built-in `localhost:3000`
and `*.vercel.app` defaults):

```bash
export CORS_ALLOWED_ORIGINS=https://billing.example.com,https://staging.example.com
```

## Tests

```bash
./mvnw test         # uses an H2 in-memory DB; no Postgres required
```

## Deployment (Render)

A `render.yaml` blueprint and `Dockerfile` are committed.

1. On [render.com](https://render.com), **New → Blueprint** → connect this repo.
2. Set env vars on the web service: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `CORS_ALLOWED_ORIGINS`.
3. For the database, use [Neon](https://neon.tech) (free, no card, no expiry)
   and copy its JDBC URL into `DB_URL`.

The Dockerfile builds a slim runtime image from `eclipse-temurin:21-jre-alpine`.

## Schema notes

`spring.jpa.hibernate.ddl-auto=update` creates / updates tables on first
boot. If you change a column to `NOT NULL` and existing rows can't satisfy
that, just `DROP TABLE <name> CASCADE;` and let Hibernate recreate it on
the next start. The audit log is unaffected.

## Security note

`SecurityConfig` currently grants `permitAll()` on every endpoint. There
is no authentication. **Don't expose this server to the public internet
without adding auth first.**

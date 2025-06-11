## Database Application

This repository contains a **complete, self-running Java & MySQL demo** that showcases the entire lifecycle of a relational database project:

* **Domain modelling** and normalisation
* **Schema creation** (DDL)
* **Bulk data loading** from CSV
* **Advanced queries** (aggregations, constraints, joins)
* **Automated execution** through a headless Java driver

Everything is designed to spin up, populate, query, and tear down multiple independent databases in a single run—no manual input required.

---

### Domain Overview

Our sample domain models the world of *video-game collectibles*:

| Entity        | Key attributes                             | Typical relationships       |
| ------------- | ------------------------------------------ | --------------------------- |
| **Game**      | `game_id (PK)`, `title`, `release_year`    | *HAS* → **Edition**         |
| **Edition**   | `edition_id (PK)`, `region`, `platform`    | *CONTAINS* → **Item**       |
| **Item**      | `item_id (PK)`, `type`, `rarity`           | *OWNED\_BY* → **Collector** |
| **Collector** | `collector_id (PK)`, `nickname`, `country` | –                           |

The schema is third-normal-form, with meaningful foreign-key constraints and cascading rules. An ER diagram (`/docs/ERD.jpeg`) visualises the full structure.

---

### Repository Layout

```
.
├── src/main/java/
│   └── MainRunner.java       # Headless driver (no user prompts)
├── contributors/
│   ├── 22345124/
│   │   ├── schema.sql        # DDL
│   │   ├── dml.sql           # Queries & deletes
│   │   └── data.csv          # ≥ 200 rows, one table
│   ├── 5532367/
│   │   └── ...
│   └── ...
├── docs/
│   ├── ERD.jpeg
│   └── project-description.pdf
└── pom.xml
```

*Each contributor directory* bundles everything needed to build and exercise a fully isolated database instance (schema, data, queries, supporting docs).

---

### Quick Start

> **Prerequisites**
>
> * MySQL 8.x (running on `localhost:3306`)
> * Java 17+
> * Maven

```bash
# 1. Clone and build
git clone https://github.com/<your-user>/vg-collectibles-db.git
cd vg-collectibles-db
mvn -q package          # creates target/vg-db-demo.jar

# 2. Create a MySQL user with full privileges (once)
mysql -u root -p -e "CREATE USER 'demo'@'localhost' IDENTIFIED BY 'demopass';
                     GRANT ALL PRIVILEGES ON *.* TO 'demo'@'localhost';
                     FLUSH PRIVILEGES;"

# 3. Run the demo (no command-line arguments)
java -jar target/vg-db-demo.jar
```

The driver performs the following for **each** contributor package:

1. Opens a JDBC connection as `demo/demopass`.
2. Creates a fresh database (e.g. `db_22345124`), `USE`s it, and executes `schema.sql`.
3. Bulk-loads `data.csv` using batched inserts (or `LOAD DATA LOCAL INFILE` if available).
4. Executes every statement in `dml.sql`, printing results and reporting any constraint violations.
5. Drops the database and moves on to the next package.

You can watch the console for real-time progress and query output.

---

### Highlights

* **Zero interaction** – everything runs unattended; perfect for CI pipelines or demos.
* **Batch loading** – 200 × *n* rows imported in seconds via JDBC batches or `LOAD DATA`.
* **Resource-safe Java** – modern `try-with-resources` patterns; one connection per database.
* **Portable build** – plain Maven; no IDE files committed.
* **Easily extensible** – just add a new contributor folder with `schema.sql`, `dml.sql`, and `data.csv`.

---

### Extending or Re-using

* **New Domains** – swap out the ERD, adjust `schema.sql`, and provide new data; the driver is schema-agnostic.
* **Alternative RDBMS** – change the JDBC URL to target MariaDB or PostgreSQL (DDL tweaks may be required).
* **Containerisation** – see the `extras/docker-compose.yml` branch for a one-command local stack.
* **Testing** – integrate Testcontainers or GitHub Actions to validate every pull request automatically.

---



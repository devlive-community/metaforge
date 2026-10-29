### Schema

The metadata schema is no longer installed from SQL files in this folder.

- New installations: the setup wizard creates the schema on the chosen database
  (embedded H2, MySQL, PostgreSQL, SQL Server or Oracle) from the Liquibase changelog in
  `core/metaforge-server/src/main/resources/db/changelog`.
- Upgrades: the server applies pending changesets automatically on boot, including the
  upgrade of installations created by 2026.0.0 and earlier setup scripts.

The versioned folders below are kept for installations older than 2026.0.0, which must
first be brought to the 2026.0.0 schema with these scripts.

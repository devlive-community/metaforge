---
title: Sync executor
---

The MetaForge executor is the built-in data sync engine (formerly named `Local`; upgrades migrate the name). It reads a source query inside the MetaForge server and writes batches to the target table, for both dataset syncs and workflows.

By default it runs one query on one channel with no rate limit, which suits small and medium data. For large data, turn on parallel splits as described below.

#### How it works

A sync goes through these stages: prepare, count (optional), plan splits, transfer, and finalize (refresh the table's row count and size).

Transfer runs on one or more channels. Each channel has a reader thread and a writer thread with their own database connections and a bounded queue between them: the reader waits when the queue is full, so neither side stalls the other.

All syncs share a queue, and `maxConcurrentSyncs` under `System settings > Runtime settings > Dataset storage` caps how many run at once (4 by default). A dataset can have one unfinished sync at a time.

#### Settings

Change the defaults under `System settings > Runtime settings`, or change them for one sync in the `Sync Data` dialog.

**Concurrency and splitting**

| Setting | Default | Description |
|---|---|---|
| `concurrency` | `1` | Parallel channels, at most 16. More than 1 needs splitting to have any effect |
| `splitStrategy` | `NONE` | `NONE` keeps one query; `AUTO` splits when channels are above 1; `RANGE` always splits |
| `splitColumn` | empty | Column to split on. Empty uses the dataset's primary key, then its order-by key |
| `splitCount` | `0` | Number of splits, at most 256. `0` uses four per channel, so faster channels take more of the work on skewed data |

**Throughput and limits**

| Setting | Default | Description |
|---|---|---|
| `fetchSize` | `1000` | Rows fetched from the source per round trip. MySQL and MariaDB switch to streaming automatically |
| `batchSize` | `1000` | Rows written to the target per batch |
| `queueCapacity` | `4` | Batches buffered per channel |
| `maxRowsPerSecond` | `0` | Limit on rows read per second across all channels; `0` means no limit |
| `maxBytesPerSecond` | `0` | Limit on estimated bytes read per second across all channels; `0` means no limit |

**Fault tolerance and retries**

| Setting | Default | Description |
|---|---|---|
| `maxErrorRows` | `0` | Rows the target may reject; `0` fails on the first one |
| `writeRetries` | `3` | Times a batch is written again after a transient error; `0` fails instead |
| `writeRetryBackoffMs` | `1000` | Wait before the first batch retry in milliseconds; it doubles with each retry |
| `maxRetries` | `0` | Times the whole sync runs again after a transient failure |
| `retryIntervalSeconds` | `30` | Seconds to wait before the whole sync runs again |

**Other**

| Setting | Default | Description |
|---|---|---|
| `preCount` | `false` | Count the source first, so progress and time left are known from the start. Counting can be slow on large tables |
| `timeout` | `0` | Running time limit in seconds, without queued or paused time; `0` means no limit |

#### Splitting

Splitting cuts the query by value ranges of the split column. The outer ranges are open and rows with a NULL key get their own split, so every row belongs to exactly one split.

The split column must be an integer or date column, ideally indexed and evenly spread. Rows are read in that column's order within a split, and without an index that sort can cost more than parallelism gains.

These cases are not errors. The sync falls back to one query and the task log says why:

- The query uses `LIMIT`, `OFFSET`, `GROUP BY`, `DISTINCT`, `UNION`, window functions or `WITH`.
- There is no split column, or it is not a plain identifier.
- The column is not an integer or date, or its range cannot be read.
- The database rejects the split condition, for example on a case-sensitive column name.

Splits are separate queries and do not share a snapshot. If the source changes during the sync, the result is not a consistent view of one moment.

#### Fault tolerance

A failed write is handled by the kind of error:

- **Transient errors** (lost connection, deadlock, server restart): the batch is written again on a new connection after a wait, up to `writeRetries` times.
- **Data errors** (value too long, wrong type, constraint violation): with `maxErrorRows` above 0, bisection finds the rejected rows and skips them while the others are written. All skipped rows are saved to `<task>.dirty.jsonl` in the task directory, and the first 20 appear in the run metrics. Past the limit the sync fails.
- **Other errors** (syntax, permissions, missing table): the sync fails.

Rows are only skipped on targets where a failed write leaves nothing behind: databases with transactions, and ClickHouse. Other targets fail on the first rejected row.

When a read or write still fails on a transient error, the whole sync runs again if `maxRetries` is set.

#### Pause, resume and run again

A sync running in splits can pause. It commits the batches already formed, releases its connections and saves a checkpoint; resuming queues it again and continues from there. The checkpoint follows the split key, so repeated keys and NULLs lose and repeat nothing.

A failed, timed-out, stopped or interrupted sync can use `Run again from checkpoint` on the sync records page, as long as nothing gets written twice:

| Situation | Can it run again? |
|---|---|
| Ran in splits, has a checkpoint | Yes, from the checkpoint |
| No splits, already wrote rows | No. Clear the data and sync again |
| No splits, wrote nothing yet | Yes, from the start |
| The server died while it was paused or waiting to retry | Yes, the checkpoint is exact |
| The server died while it was running | No. Rows past the checkpoint may have been committed, so clear the data and sync again |

When more than 100,000 rows share one key they no longer fit in a batch. The sync gives up its checkpoint and finishes writing, but cannot pause or resume any more.

#### States

| State | Meaning |
|---|---|
| Created, Queued | Waiting to run |
| Running | Syncing |
| Pausing, Paused | Committing batches in flight; checkpoint saved and ready to resume |
| Retrying | Waiting for the next run after a transient failure |
| Stopping, Stopped | Cancelled by a user |
| Success | All done, including the table's row count and size |
| Failure, Timed out | The run failed; it passed `timeout` |
| Interrupted | The server died during the run |

Queued, paused and retrying syncs can be cancelled at once.

#### Run metrics

See the `Metrics` tab on the sync records page:

- **Volume**: progress, rows and bytes read and written, the total and where it comes from, elapsed time, time left, skipped and failed rows.
- **Throughput**: read and write rates over the last 5 seconds, average and peak rates, and a trend of the last 120 samples.
- **Pipeline**: committed batches, batch retries, average batch time, channels, the buffer queue, reader, writer and throttle waits, and the bottleneck (source, target or rate limit).
- **Split progress**: each split's range, state, channel, rows read and written, retries and time.
- **Rejected rows**: samples of the error and the row.

Without a known total, progress is estimated from finished splits. Without splits the page shows the rows written, and the total is known when the sync ends.

Bytes are estimated from values to show throughput. They are not network traffic or disk usage.

#### API

All paths start with `/api/v1/dataset`:

| Method | Path | Description |
|---|---|---|
| `PUT` | `/syncData/{code}` | Start a sync; the body may override the settings above. Returns the sync record |
| `POST` | `/history/{code}` | List sync records |
| `GET` | `/history/metrics/{id}` | State, metrics and state changes; `retryable` tells whether it can run again |
| `GET` | `/sync/active` | The signed-in user's unfinished syncs |
| `PUT` | `/history/pause/{id}` | Pause |
| `PUT` | `/history/resume/{id}` | Resume |
| `PUT` | `/history/stop/{id}` | Cancel |
| `PUT` | `/history/retry/{id}` | Run again |

#### Known limitation

When a connection is lost during a write, there is no way to know whether that batch took effect, so a retry or resume may repeat it. No rows are lost, and at most one batch per channel repeats. It is more likely on targets without transactions such as ClickHouse, where the server may already have run the `INSERT`. If that is not acceptable, set `writeRetries` to `0` or use a table engine that deduplicates, such as `ReplacingMergeTree`.

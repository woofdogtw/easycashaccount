Database Schema
===============

# Database Information

```mermaid
erDiagram
    db_info {
        TEXT    name        "Application. `Easy Cash Account`"
        INTEGER version     "Database verison. Current is `2`"
        TEXT    descript    "UTF-8. Cash account description"
        INTEGER last_modify "The database last updated time in Unix tick (in seconds)"
    }
```

# Transaction Types

```mermaid
erDiagram
    db_trans_type {
        TEXT type_name  "UTF-8. Type for transactions"
    }
```

# Transactions

Descriptions:

- `no`: The unique transaction identifier. Use Unix tick in seconds because users will not add more than one transaction in one second.
- `date`
    - In version 1: Unix `time_t` without time (always local 00:00:00).
    - In version 2: Date in decimal number. YYYY * 10000 + MM * 100 + DD.

```mermaid
erDiagram
    db_transaction {
        INTEGER no          "Unique. Transaction number"
        INTEGER date        "Transaction date. Decimal that contains YYYY*10000 + MM*100 + DD"
        TEXT    type        "Transaction type. Refer to the `db_trans_type` table"
        INTEGER in_out      "0: income. 1: expense. 2: budget"
        REAL    money       "The amount of this transaction"
        TEXT    location    "The location of this transaction"
        TEXT    descript    "Detail description"
    }
```

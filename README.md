# Author: Radulescu Alexandru
---

## 📑 Key Components

### ⚙️ Redis Caching
- **RedisTemplate** is configured for JSON serialization via Jackson.
- Supports **Time-To-Live (TTL)** for cache entries, configurable via `application.properties`.
- Custom cache operations:
    - Retrieve accounts from cache
    - Cache population after DB fetch
    - Cache update/delete after DB changes
    - Manual TTL parsing utility

### 📊 Database Access (Repository Layer)
- Uses **Spring JDBC (JdbcClient)** for fluent, type-safe SQL execution.
- All operations (CRUD) for `Account` entities.
- Custom exceptions for precise error reporting.
- Post-operation validation to ensure database changes.

### 🛠️ Service Layer
- Implements business logic for:
    - Fetching accounts (cache-first, fallback to DB)
    - Creating, updating, and deleting accounts with synchronized cache updates.
- Transactional boundaries (`@Transactional`) ensure consistency between cache and DB.

### 📝 Custom Exceptions
- Domain-specific runtime exceptions for clear, meaningful error propagation:
    - `AccountRetrievalException`
    - `AccountCreationException`
    - `AccountUpdateException`
    - `AccountDeletionException`
    - `AccountNotFoundException`

---


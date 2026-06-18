# JWT + Multi-Tenant REST API

This project is a Spring Boot REST API with:
- JWT authentication
- tenant isolation (`tenant_id` on tenant-owned tables)
- tenant switching for users assigned to multiple tenants

## Architecture (packages)

- `auth`: authentication controllers/services and DTOs
- `security`: JWT service/filter + Spring Security config
- `multitenancy`: `TenantContext`, tenant filter, tenant entity listener
- `tenant`: tenant entity/repository/DTOs
- `user`: user, role, user-tenant link, profile endpoints/services
- `invoice`: sample tenant-owned CRUD resource
- `shared.exception`: API exception handling

## Profiles

- `local`: PostgreSQL + local JWT secret in `application-local.yml`
- `prod`: PostgreSQL + JWT settings from environment variables
- `test`: H2 in-memory DB + JWT test secret in `application-test.yml`

Base config: `application.yml` sets `spring.profiles.default=local`.

## Required prod environment variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (must be at least 32 chars)
- `JWT_EXPIRATION_MINUTES` (optional, default `60`)

## Security and tenant behavior

- Public endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Auth required for all other endpoints.
- JWT contains:
  - `uid` (user id)
  - `sub` (email)
  - `tenantId` (selected tenant)
  - `tenants` (allowed tenant IDs)
- `TenantContext` is filled from JWT on each request and cleared after request.
- Tenant-owned writes auto-assign `tenant_id` using JPA listener.
- Tenant-owned reads/updates/deletes always query with current tenant id.

## Endpoints

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/switch-tenant`

### User

- `GET /api/me`
- `GET /api/me/tenants`

### Tenant-owned sample resource (invoices)

- `POST /api/invoices`
- `GET /api/invoices`
- `GET /api/invoices/{id}`
- `PUT /api/invoices/{id}`
- `DELETE /api/invoices/{id}`

## Example requests and responses

### Register

`POST /api/auth/register`

```json
{
  "email": "alice@example.com",
  "password": "StrongPass123!",
  "tenantName": "Acme"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInMinutes": 120,
  "selectedTenantId": 1,
  "allowedTenantIds": [1]
}
```

### Login

`POST /api/auth/login`

```json
{
  "email": "alice@example.com",
  "password": "StrongPass123!"
}
```

### Switch tenant

`POST /api/auth/switch-tenant`

```json
{
  "tenantId": 2
}
```

Response: same shape as login/register but with updated `selectedTenantId` and new token.

### Create invoice

`POST /api/invoices`

```json
{
  "reference": "INV-2026-001",
  "description": "Consulting services",
  "amount": 1500.00
}
```

`tenant_id` is not accepted from client and is assigned from current tenant context.

## Run

```powershell
Set-Location "D:\Projects\TESTINGS\api.facturesimple.ma"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

## Test

```powershell
Set-Location "D:\Projects\TESTINGS\api.facturesimple.ma"
.\mvnw.cmd test -Dspring.profiles.active=test
```


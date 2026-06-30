# Company Information Feature

This document describes the Company Information feature for the Facturesimple API. This feature allows tenants to store and manage their company information, which is used as the issuer details on invoices.

## Overview

- **One Company per Tenant**: Each tenant can have exactly one company record (enforced at database level)
- **Multiple Banks**: A company can have multiple bank accounts, with one marked as default
- **Multi-Tenant**: All company information is tenant-isolated and secure
- **Invoice Integration**: Company details are included in invoice responses

## Domain Model

### Company Entity

Stores the main company information.

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `name` | String (120) | Company legal name |
| `email` | String (190) | Official company email |
| `phone` | String (40) | Company contact phone |
| `address` | String (500) | Physical address |
| `taxId` | String (80) | Tax/VAT identification number |
| `registreCommerce` | String (80) | Business registration/commercial registry number |
| `logo` | String (500) | Company logo URL |
| `website` | String (255) | Company website URL |
| `currency` | String (3) | Default currency code (default: "MAD") |
| `defaultVatRate` | BigDecimal | Default VAT percentage for invoices |
| `paymentTermsInDays` | Integer | Default payment terms in days |
| `description` | String (500) | Additional company information |
| `tenantId` | Long | Tenant ownership (from BaseTenantAwareEntity) |
| `createdAt` | Instant | Creation timestamp |
| `updatedAt` | Instant | Last update timestamp |

### CompanyBank Entity

Represents a bank account for the company.

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `company` | Company | Foreign key to Company |
| `bankName` | String (120) | Bank name |
| `accountNumber` | String (80) | Bank account number |
| `swiftCode` | String (20) | SWIFT/BIC code |
| `iban` | String (80) | IBAN for international payments |
| `isDefault` | Boolean | Is this the default bank? |
| `createdAt` | Instant | Creation timestamp |

**Constraints:**
- One company can have multiple banks
- Only one bank per company can be marked as default
- When setting a bank as default, the previous default is automatically unset

## REST API Endpoints

### Base Path: `/v1/company`

#### Company Management

**GET** `/v1/company`
- Retrieve company information for the current tenant
- Returns 404 if company not found
- **Response:** `CompanyResponse`

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/v1/company
```

**POST** `/v1/company`
- Create company information for the current tenant
- Returns 409 if company already exists (use PUT to update)
- **Request:** `CompanyCreateRequest`
- **Response:** `CompanyResponse`

```bash
curl -X POST -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d @company.json \
  http://localhost:8080/v1/company
```

**PUT** `/v1/company`
- Update company information for the current tenant
- Returns 404 if company not found
- **Request:** `CompanyCreateRequest`
- **Response:** `CompanyResponse`

```bash
curl -X PUT -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d @company.json \
  http://localhost:8080/v1/company
```

**DELETE** `/v1/company`
- Delete company information for the current tenant
- Returns 404 if company not found
- **Response:** `204 No Content`

```bash
curl -X DELETE -H "Authorization: Bearer <token>" \
  http://localhost:8080/v1/company
```

#### Bank Management

**POST** `/v1/company/banks`
- Add a new bank to the company
- If no banks exist, this becomes the default
- Returns 404 if company not found
- **Request:** `CompanyBankRequest`
- **Response:** `CompanyBankResponse`

```bash
curl -X POST -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d @bank.json \
  http://localhost:8080/v1/company/banks
```

**PUT** `/v1/company/banks/{bankId}`
- Update bank details
- Can update all fields including `isDefault`
- Returns 404 if bank not found
- **Request:** `CompanyBankRequest`
- **Response:** `CompanyBankResponse`

```bash
curl -X PUT -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d @bank.json \
  http://localhost:8080/v1/company/banks/1
```

**PUT** `/v1/company/banks/{bankId}/default`
- Set a specific bank as the default bank
- Automatically unsets the current default
- Returns 404 if bank not found
- **Response:** `CompanyBankResponse`

```bash
curl -X PUT -H "Authorization: Bearer <token>" \
  http://localhost:8080/v1/company/banks/1/default
```

**DELETE** `/v1/company/banks/{bankId}`
- Delete a bank
- Returns 404 if bank not found
- **Response:** `204 No Content`

```bash
curl -X DELETE -H "Authorization: Bearer <token>" \
  http://localhost:8080/v1/company/banks/1
```

## Request/Response Examples

### Create Company

**Request:**
```json
POST /v1/company

{
  "name": "Acme Solutions SARL",
  "email": "billing@acme.ma",
  "phone": "+212661234567",
  "address": "123 Business Avenue, Casablanca, Morocco",
  "taxId": "ICE-123456789",
  "registreCommerce": "RC-2024-12345",
  "logo": "https://company.com/logo.png",
  "website": "https://acme.ma",
  "currency": "MAD",
  "defaultVatRate": 20.00,
  "paymentTermsInDays": 30,
  "description": "Leading consulting and IT solutions provider"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Acme Solutions SARL",
  "email": "billing@acme.ma",
  "phone": "+212661234567",
  "address": "123 Business Avenue, Casablanca, Morocco",
  "taxId": "ICE-123456789",
  "registreCommerce": "RC-2024-12345",
  "logo": "https://company.com/logo.png",
  "website": "https://acme.ma",
  "currency": "MAD",
  "defaultVatRate": 20.00,
  "paymentTermsInDays": 30,
  "description": "Leading consulting and IT solutions provider",
  "banks": [],
  "tenantId": 1,
  "createdAt": "2026-06-26T10:30:00Z",
  "updatedAt": "2026-06-26T10:30:00Z"
}
```

### Add Bank Account

**Request:**
```json
POST /v1/company/banks

{
  "bankName": "Bank of Morocco",
  "accountNumber": "1234567890123",
  "swiftCode": "BMCEMAMC",
  "iban": "MA64012345678901234567890",
  "isDefault": true
}
```

**Response:**
```json
{
  "id": 1,
  "bankName": "Bank of Morocco",
  "accountNumber": "1234567890123",
  "swiftCode": "BMCEMAMC",
  "iban": "MA64012345678901234567890",
  "isDefault": true,
  "createdAt": "2026-06-26T10:35:00Z"
}
```

### Get Full Company with Banks

**Request:**
```bash
GET /v1/company
```

**Response:**
```json
{
  "id": 1,
  "name": "Acme Solutions SARL",
  "email": "billing@acme.ma",
  "phone": "+212661234567",
  "address": "123 Business Avenue, Casablanca, Morocco",
  "taxId": "ICE-123456789",
  "registreCommerce": "RC-2024-12345",
  "logo": "https://company.com/logo.png",
  "website": "https://acme.ma",
  "currency": "MAD",
  "defaultVatRate": 20.00,
  "paymentTermsInDays": 30,
  "description": "Leading consulting and IT solutions provider",
  "banks": [
    {
      "id": 1,
      "bankName": "Bank of Morocco",
      "accountNumber": "1234567890123",
      "swiftCode": "BMCEMAMC",
      "iban": "MA64012345678901234567890",
      "isDefault": true,
      "createdAt": "2026-06-26T10:35:00Z"
    },
    {
      "id": 2,
      "bankName": "Attijariwafa Bank",
      "accountNumber": "9876543210987",
      "swiftCode": "ATBKMAMC",
      "iban": "MA64098765432109876543210",
      "isDefault": false,
      "createdAt": "2026-06-26T10:36:00Z"
    }
  ],
  "tenantId": 1,
  "createdAt": "2026-06-26T10:30:00Z",
  "updatedAt": "2026-06-26T10:30:00Z"
}
```

## Integration with Invoices

The company information is designed to be integrated with the invoice system:

- **Invoice Generation**: When creating an invoice, the company's default VAT rate and payment terms can be pre-populated
- **Invoice Response**: Future versions will include company details in the `InvoiceResponse` for complete invoice context
- **Default Bank**: The company's default bank information can be included in invoice payment instructions

## Error Handling

All endpoints follow standard REST error conventions:

**Common Error Responses:**

```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Company information not found for tenant",
  "timestamp": "2026-06-26T10:30:00Z"
}
```

```json
{
  "error": "BAD_REQUEST",
  "message": "Company information already exists for this tenant. Use update instead.",
  "timestamp": "2026-06-26T10:30:00Z"
}
```

## Security & Multi-Tenancy

- **Tenant Isolation**: All company data is automatically filtered by tenant_id
- **Authorization**: Only authenticated users of a tenant can access that tenant's company data
- **Database Constraint**: One company per tenant is enforced at the database level via unique constraint on tenant_id
- **Non-Root User**: Data access follows the application's security context

## Database Schema

### companies table

```sql
CREATE TABLE companies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190),
  phone VARCHAR(40),
  address VARCHAR(500),
  tax_id VARCHAR(80),
  registre_commerce VARCHAR(80),
  logo VARCHAR(500),
  website VARCHAR(255),
  currency VARCHAR(3) DEFAULT 'MAD',
  default_vat_rate DECIMAL(5, 2),
  payment_terms_in_days INT,
  description VARCHAR(500),
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  UNIQUE KEY uk_company_tenant (tenant_id),
  INDEX idx_companies_tenant_id (tenant_id)
);
```

### company_banks table

```sql
CREATE TABLE company_banks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  bank_name VARCHAR(120) NOT NULL,
  account_number VARCHAR(80),
  swift_code VARCHAR(20),
  iban VARCHAR(80),
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_company_banks_company_id (company_id),
  INDEX idx_company_banks_is_default (company_id, is_default),
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);
```

## Usage Flow

### 1. User Registration & Tenant Creation
When a user registers and creates a tenant via `/v1/auth/register`, no company is yet created.

### 2. Set Up Company Information
The user should create company information:
```bash
POST /v1/company
```

### 3. Add Bank Accounts
The user can then add one or more bank accounts:
```bash
POST /v1/company/banks
```

### 4. Create Invoices
When creating invoices, the company information is available:
```bash
POST /v1/invoices
```

The invoice response includes the company details.

## Future Enhancements

Potential features for future versions:
- Company logo URL storage with image retrieval
- Digital signature settings for invoices
- Tax configuration per company
- Payment method templates (integrate with bank info)
- Company branding for PDF exports
- Multiple company support (if multi-company mode is needed)

## Troubleshooting

**Company not found error when creating invoices:**
- Ensure you have created company information first using POST /v1/company
- Verify you are using the correct tenant token

**Cannot set bank as default:**
- Verify the bank ID exists for your company
- Check that you have the correct Authorization header

**Email validation error:**
- Email field must be a valid email format
- Leave empty if not applicable


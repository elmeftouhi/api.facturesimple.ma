# Invoice Implementation Summary

## Entity Structure

### 1. Invoice (Header)
- **invoiceNumber**: Auto-incremented per tenant (1, 2, 3, ...)
- **formattedNumber**: Configurable per tenant (e.g., "1-2026")
- **invoiceDate**: LocalDate
- **dueDate**: LocalDate (optional)
- **customer**: ManyToOne relationship to Customer (required)
- **description**: String (optional)
- **vatRate**: BigDecimal percentage (e.g., 20.00 for 20%)
- **lineItems**: OneToMany relationship to InvoiceLineItem (cascade delete)
- **payments**: OneToMany relationship to InvoicePayment (cascade delete)
- **createdAt**: Instant (immutable)

### 2. InvoiceLineItem (Many per Invoice)
- **itemReference**: String (required)
- **itemDescription**: String (optional)
- **quantity**: BigDecimal (required)
- **unitPrice**: BigDecimal (required)
- **invoice**: ManyToOne reference to Invoice

### 3. InvoicePayment (Many per Invoice)
- **paymentMethod**: PaymentMethod Enum (CASH, CHECK, TRANSFER, CARD, OTHER)
- **paymentReference**: String (optional)
- **paymentDate**: LocalDate (required)
- **paidAmount**: BigDecimal (required)
- **invoice**: ManyToOne reference to Invoice

## PaymentMethod Enum
Currently implemented as:
- CASH
- CHECK
- TRANSFER
- CARD
- OTHER

## REST API Endpoints

### Invoice Management
- `POST /v1/invoices` - Create invoice with line items and optional payments
- `GET /v1/invoices` - List all invoices for tenant
- `GET /v1/invoices/{id}` - Get invoice by ID
- `PUT /v1/invoices/{id}` - Update invoice (date, due date, description, VAT rate)
- `DELETE /v1/invoices/{id}` - Delete invoice (cascades to line items and payments)

### Payment Management
- `POST /v1/invoices/{id}/payments` - Add payment to invoice
- `DELETE /v1/invoices/{id}/payments/{paymentId}` - Remove payment from invoice

## Features Implemented

✅ **Auto-increment invoiceNumber** per tenant
✅ **Formatted number generation** (invoiceNumber-year)
✅ **Customer relationship** (required)
✅ **Line items support** (many per invoice, cascade delete)
✅ **Payments support** (many per invoice, cascade delete)
✅ **VAT rate** as percentage (BigDecimal)
✅ **PaymentMethod enum** (initially; can be migrated to database later)
✅ **Tenant isolation** throughout
✅ **Cascading operations** for aggregate integrity

## Future Enhancements

- [ ] Database-backed PaymentMethod (enum -> entity migration)
- [ ] Invoice totals calculation (subtotal, VAT amount, total)
- [ ] Payment balance tracking
- [ ] Invoice status tracking (draft, sent, paid, overdue)
- [ ] Payment reconciliation
- [ ] Invoice templates per customer


# Onlineshop - ER-Diagram

```mermaid
erDiagram
    CUSTOMER {
        int ID PK
        varchar FIRSTNAME
        varchar LASTNAME
        varchar STREET
        varchar CITY
    }

    INVOICE {
        int ID PK
        int CUSTOMERID FK
        decimal TOTAL
    }

    ITEM {
        int INVOICEID FK
        int ITEM PK
        int PRODUCTID FK
        int QUANTITY
        decimal COST
    }

    PRODUCT {
        int ID PK
        varchar NAME
        decimal PRICE
    }

    FLAT_SALES {
        int CUSTOMERID FK
        varchar FIRSTNAME
        varchar LASTNAME
        varchar CITY
        int INVOICEID FK
        decimal INVOICE_TOTAL
        varchar PRODUCT_NAME
        int QUANTITY
        decimal COST
    }

    CUSTOMER ||--o{ INVOICE : "stellt aus"
    INVOICE ||--o{ ITEM : enthaelt
    PRODUCT ||--o{ ITEM : "ist enthalten"
    CUSTOMER ||--o{ FLAT_SALES : "denormalisiert in"
    INVOICE ||--o{ FLAT_SALES : "denormalisiert in"
```

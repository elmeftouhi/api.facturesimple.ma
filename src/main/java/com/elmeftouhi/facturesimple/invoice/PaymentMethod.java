package com.elmeftouhi.facturesimple.invoice;

public enum PaymentMethod {
    CASH("Cash"),
    CHECK("Check"),
    TRANSFER("Bank Transfer"),
    CARD("Credit Card"),
    OTHER("Other");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


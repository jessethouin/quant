package com.jessethouin.quant.conf;

public enum OrderStatus {
    FILLED("filled"),
    EXPIRED("expired"),
    CANCELED("canceled"); // ugh, Americans. It's CANCELLED!!

    public final String status;
    OrderStatus(String s) {
        this.status = s;
    }

    @Override
    public String toString() {
        return this.status;
    }
}

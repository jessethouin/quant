package com.jessethouin.quant.conf;

public enum OrderStatus {
    FILLED("filled"),
    EXPIRED("expired"),
    CANCELED("canceled"); // ugh, Americans. It's CANCELLED!!

    final String status;
    OrderStatus(String s) {
        this.status = s;
    }

    public String getStatus() {
        return status;
    }
}

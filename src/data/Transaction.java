package data;

import java.util.Date;

public class Transaction {
    private Date date;
    private String productCode;
    private int amountBought;
    private Client client;

    public Transaction(Date date, String productCode, int amountBought, Client client) {
        this.date = date;
        this.productCode = productCode;
        this.amountBought = amountBought;
        this.client = client;
    }

    public Date getDate() {
        return date;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getAmountBought() {
        return amountBought;
    }

    public Client getClient() {
        return client;
    }
}

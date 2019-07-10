package data;

public class Transaction {
    private String storeName;
    private String productCode;
    private int amountBought;
    private Client client;

    public Transaction(String storeName, String productCode, int amountBought, Client client) {
        this.storeName = storeName;
        this.productCode = productCode;
        this.amountBought = amountBought;
        this.client = client;
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

    public String getStoreName() {
        return storeName;
    }
}

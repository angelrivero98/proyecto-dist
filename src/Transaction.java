import java.util.Date;

class Transaction {
    private Date date;
    private String productCode;
    private int amountBought;

    public Transaction(Date date, String productCode, int amountBought) {
        this.date = date;
        this.productCode = productCode;
        this.amountBought = amountBought;
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
}

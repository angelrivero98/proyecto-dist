package data;

public class Product {

    private String store;
    private String code;
    private Integer amount;

    public Product(String store, String code, Integer quantity) {
        this.store = store;
        this.code = code;
        this.amount = quantity;
    }

    public String getCode(){

        return  this.code;

    }

    public String getStore(){

        return this.store;

    }

    public Integer getAmount(){

        return this.amount;

    }

    public void setAmount(Integer amount){
        this.amount = amount;
    }

    public void sumAmount(Integer sum){
        this.amount += sum;
    }

    private boolean canBeBought(int amount) {
        return this.amount - amount >= 0;
    }


    /**
     * Validates that the product has enough supply to meet the order
     * and generates the corresponding data.Transaction object.
     *
     * @param amount Amount of product to be bought
     * @return data.Transaction object representing the order
     * @throws IllegalArgumentException If the desired amount if greater than the product quantity
     */
    public Transaction buy(String atStore, Client client, int amount) {
        if (!canBeBought(amount))
            throw new IllegalArgumentException("Can't buy that many units");

        this.amount -= amount;

        return new Transaction(atStore, this.getCode(), amount, client);
    }
}

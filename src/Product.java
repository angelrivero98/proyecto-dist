public class Product {

    private String store;
    private String code;
    private Integer amount;

    public Product(String tienda,String code, Integer cantidad){
        this.store = tienda;
        this.code = code;
        this.amount = cantidad;
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
}

package data;

public class Store {
    private String name;
    // TODO: List of items / instance of Inventory

    public Store(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // TODO: add methods to buy products, etc.
    //       Probably should be a simple wrapper to the same methods implemented in Inventory
}

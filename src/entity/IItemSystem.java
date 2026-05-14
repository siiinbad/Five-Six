package entity;

import java.util.List;
import java.util.Random;

public interface IItemSystem {
    ItemSystem.Item addRandom(Random rand);
    void remove(ItemSystem.Item item);
    int count(ItemSystem.Item item);
    boolean isEmpty();
    List<ItemSystem.Item> getItems();
    List<ItemSystem.Item> getFlatList();
    void setItems(List<ItemSystem.Item> items);
}

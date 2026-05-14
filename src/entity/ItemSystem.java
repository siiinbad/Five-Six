package entity;

import java.util.*;

public class ItemSystem implements IItemSystem {

    public enum Item {
        WATER        ("Water",              "Heals 10% of max HP."),
        BARNUTS      ("Barnuts",            "Heals 10 HP."),
        GREENCROSS   ("Greencross Alcohol", "Heals 5 HP for 3 rounds, but deals 2 damage on use."),
        COFFEE       ("Coffee",             "Adds 10 max HP permanently."),
        ENERGY_DRINK ("Energy Drink",       "Adds 0.05 to permanent damage multiplier."),
        SLEEPING_MASK("Sleeping Mask",      "Fully restores HP.");

        public final String displayName;
        public final String description;
        Item(String d, String desc) { displayName=d; description=desc; }
    }

    // Stacked inventory: item -> count
    private final Map<Item, Integer> inventory = new LinkedHashMap<>();

    @Override
    public Item addRandom(Random rand) {
        int roll = rand.nextInt(123);
        Item item;
        if      (roll < 30)  item = Item.WATER;
        else if (roll < 60)  item = Item.BARNUTS;
        else if (roll < 85)  item = Item.GREENCROSS;
        else if (roll < 100) item = Item.COFFEE;
        else if (roll < 115) item = Item.ENERGY_DRINK;
        else               item = Item.SLEEPING_MASK;
        inventory.merge(item, 1, Integer::sum);
        return item;
    }

    /** Remove one of the given item */
    @Override
    public void remove(Item item) {
        int count = inventory.getOrDefault(item, 0);
        if (count <= 1) inventory.remove(item);
        else inventory.put(item, count - 1);
    }

    @Override
    public int count(Item item)   { return inventory.getOrDefault(item, 0); }
    
    @Override
    public boolean isEmpty()      { return inventory.isEmpty(); }

    /** Returns list of unique items that have at least 1 in stock */
    @Override
    public List<Item> getItems()  { return new ArrayList<>(inventory.keySet()); }

    /** For save/load: get full flat list */
    @Override
    public List<Item> getFlatList() {
        List<Item> out = new ArrayList<>();
        for (Map.Entry<Item,Integer> e : inventory.entrySet())
            for (int i=0;i<e.getValue();i++) out.add(e.getKey());
        return out;
    }

    /** For save/load: restore from flat list */
    @Override
    public void setItems(List<Item> items) {
        inventory.clear();
        for (Item it : items) inventory.merge(it, 1, Integer::sum);
    }
}

package entity;

import java.util.*;

public class ItemSystem {

    public enum Item {
        WATER        ("Water Bottle",       "Heals 10% of max HP."),
        BARNUTS      ("Barnuts",            "Heals 50 HP."),
        GREENCROSS   ("Greencross Alcohol", "Heals 50 HP/round for 10 rounds; -2 HP on use."),
        COFFEE       ("Coffee",             "Adds 10 max HP permanently."),
        ENERGY_DRINK ("Energy Drink",       "20x damage for 10 rounds."),
        SLEEPING_MASK("Sleeping Mask",      "Fully restores HP.");

        public final String displayName;
        public final String description;
        Item(String d, String desc) { displayName=d; description=desc; }
    }

    // Stacked inventory: item -> count
    private final Map<Item, Integer> inventory = new LinkedHashMap<>();

    public Item addRandom(Random rand) {
    int roll = rand.nextInt(10);
    Item item;
    if      (roll < 4) item = Item.WATER;
    else if (roll < 7) item = Item.BARNUTS;
    else if (roll < 8) item = Item.GREENCROSS;
    else if (roll < 9) item = Item.COFFEE;
    else               item = rand.nextBoolean() ? Item.ENERGY_DRINK : Item.SLEEPING_MASK;
    inventory.merge(item, 1, Integer::sum);
    return item;
    }

    /** Remove one of the given item */
    public void remove(Item item) {
        int count = inventory.getOrDefault(item, 0);
        if (count <= 1) inventory.remove(item);
        else inventory.put(item, count - 1);
    }

    public int count(Item item)   { return inventory.getOrDefault(item, 0); }
    public boolean isEmpty()      { return inventory.isEmpty(); }

    /** Returns list of unique items that have at least 1 in stock */
    public List<Item> getItems()  { return new ArrayList<>(inventory.keySet()); }

    /** For save/load: get full flat list */
    public List<Item> getFlatList() {
        List<Item> out = new ArrayList<>();
        for (Map.Entry<Item,Integer> e : inventory.entrySet())
            for (int i=0;i<e.getValue();i++) out.add(e.getKey());
        return out;
    }

    /** For save/load: restore from flat list */
    public void setItems(List<Item> items) {
        inventory.clear();
        for (Item it : items) inventory.merge(it, 1, Integer::sum);
    }
}

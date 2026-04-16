package Inventory;

import Characters.Player;

public class Armor extends Item {
    private final int defenseBonus;

    public Armor(String name, String description, int defenseBonus) {
        super(name, description);
        this.defenseBonus = defenseBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public void use(Player target) {
        target.equipArmor(this);
    }
}

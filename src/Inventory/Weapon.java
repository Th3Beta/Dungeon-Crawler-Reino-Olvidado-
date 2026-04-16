package Inventory;

import Characters.Player;

public class Weapon extends Item {
    private final int attackBonus;

    public Weapon(String name, String description, int attackBonus) {
        super(name, description);
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public void use(Player target) {
        target.equipWeapon(this);
    }
}

// Inventory/Potion.java
package Inventory;

import Characters.Player;

public class Potion extends Item {
    // =========================================================
    //  PROPIEDADES DE LA POCIÓN
    // =========================================================

    private final int healAmount;

    // =========================================================
    //  CONSTRUCTOR Y USO DEL ÍTEM
    // =========================================================

    public Potion(String name, int healAmount) {
        super(name, "Restaura " + healAmount + " HP");
        this.healAmount = healAmount;
    }

    @Override
    public void use(Player target) {
        System.out.println("Usas " + getName() + "!");
        target.heal(healAmount); // heal() vive en Character, no aquí
    }
}
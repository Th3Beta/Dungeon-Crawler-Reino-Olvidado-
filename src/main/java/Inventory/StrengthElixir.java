// Inventory/StrengthElixir.java
package Inventory;

import Characters.Player;

// POR QUÉ una clase separada y no un parámetro en Potion:
// El elixir tiene lógica de "buff temporal" completamente distinta a curar.
// Mezclarlos violaría el Principio de Responsabilidad Única (SRP).
public class StrengthElixir extends Item {
    // =========================================================
    //  PROPIEDADES DEL ELIXIR
    // =========================================================

    private final int attackBonus;

    // =========================================================
    //  CONSTRUCTOR Y USO DEL ÍTEM
    // =========================================================

    public StrengthElixir(int attackBonus) {
        super("Elixir de Fuerza", "Aumenta ATK en " + attackBonus + " permanentemente");
        this.attackBonus = attackBonus;
    }

    @Override
    public void use(Player target) {
        System.out.println("Usas " + getName() + "! +" + attackBonus + " ATK");
        target.setAttack(target.getAttack() + attackBonus);
        // NOTA: setAttack es protected en Character pero accesible desde Player
        // Si quieres aplicarlo desde aquí, necesitas un método público en Player:
        // target.buffAttack(attackBonus) — delega la lógica al propio Player
    }
}
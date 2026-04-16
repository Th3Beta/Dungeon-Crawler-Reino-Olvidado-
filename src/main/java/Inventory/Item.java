// Inventory/Item.java
package Inventory;

import Characters.Player;

import java.io.Serializable;

public abstract class Item implements Serializable {
    // =========================================================
    //  DATOS BASE DE CUALQUIER ÍTEM
    // =========================================================

    private final String name;
    private final String description;

    // =========================================================
    //  CONSTRUCTOR Y ACCESORES
    // =========================================================

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }

    // POR QUÉ abstracto: cada subclase define su propio efecto.
    // Item no puede saber si cura, da fuerza o teleporta.
    public abstract void use(Player target);

    // =========================================================
    //  REPRESENTACIÓN EN TEXTO
    // =========================================================

    // POR QUÉ sobreescribir toString: hace que showItems() sea limpio
    // sin lógica especial en el Inventory.
    @Override
    public String toString() {
        return String.format("[%s] — %s", name, description);
    }
}
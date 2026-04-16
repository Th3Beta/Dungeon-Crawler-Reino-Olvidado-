// Inventory/Inventory.java
package Inventory;

import Characters.Player;
import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

public class Inventory implements Serializable {
    // =========================================================
    //  ESTRUCTURA INTERNA DEL INVENTARIO
    // =========================================================

    private final List<Item> items;
    private final int capacity;

    // =========================================================
    //  CONSTRUCTOR Y OPERACIONES PRINCIPALES
    // =========================================================

    public Inventory(int capacity) {
        this.capacity = capacity;
        this.items = new ArrayList<>();
    }

    public boolean addItem(Item item) {
        if (items.size() >= capacity) {
            System.out.println("Inventario lleno. No puedes recoger: " + item.getName());
            return false;
        }
        items.add(item);
        System.out.println("Obtienes: " + item.getName());
        return true;
    }

    // POR QUÉ devolver boolean: el llamador (Combat, Game) necesita saber
    // si el use fue exitoso para no gastar el turno en vano.
    public boolean useItem(int index, Player target) {
        if (index < 0 || index >= items.size()) {
            System.out.println("Índice de ítem inválido.");
            return false;
        }
        Item item = items.remove(index); // se consume al usarse
        item.use(target);
        return true;
    }

    // =========================================================
    //  VISUALIZACIÓN Y CONSULTAS
    // =========================================================

    public void showItems() {
        if (items.isEmpty()) {
            System.out.println("El inventario está vacío.");
            return;
        }
        System.out.println("\n=== INVENTARIO ===");
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, items.get(i));
        }
        System.out.println("==================");
    }

    public boolean isEmpty()  { return items.isEmpty(); }
    public int getSize()      { return items.size(); }

    /** Returns item display strings for web frontend JSON serialization. */
    public java.util.List<String> getItemNames() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (Item item : items) {
            result.add(item.toString());
        }
        return result;
    }
}
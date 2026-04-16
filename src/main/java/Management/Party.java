package Management;

import Characters.Player;

import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

public class Party implements IPartyManager, Serializable {
    private List<Player> members;

    // =========================================================
    //  CONSTRUCTOR
    // =========================================================

    public Party() {
        this.members = new ArrayList<>();
    }

    public List<Player> getMembers() {
        return members;
    }

    // =========================================================
    //  ALTAS, BAJAS Y BÚSQUEDAS DEL GRUPO
    // =========================================================

    @Override
    public void addCharacter(Player p) {
        members.add(p);
        System.out.println("¡" + p.getName() + " se ha unido a tu grupo!");
    }

    @Override
    public void removeCharacter(String name) {
        // removeIf busca en la lista y elimina si coincide el nombre (ignorando mayúsculas)
        boolean removed = members.removeIf(p -> p.getName().equalsIgnoreCase(name));
        if (removed) {
            System.out.println("El personaje '" + name + "' ha sido eliminado del grupo.");
        } else {
            System.out.println("No se encontró ningún personaje con ese nombre.");
        }
    }

    @Override
    public void listCharactersByType(String type) {
        System.out.println("\n--- Personajes de clase: " + type.toUpperCase() + " ---");
        boolean found = false;

        for (Player p : members) {
            // Compara el nombre de la clase (ej: "Mage", "Warrior") con lo que busca el usuario
            if (p.getClass().getSimpleName().equalsIgnoreCase(type)) {
                System.out.println("- " + p.getName() + " (HP: " + p.getHp() + ")");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No hay personajes de este tipo en el grupo.");
        }
    }

    // =========================================================
    //  ACCESO AL JUGADOR ACTIVO Y ESTADO DEL GRUPO
    // =========================================================

    // Método extra para obtener un personaje para jugar
    public Player getActivePlayer() {
        if (!members.isEmpty()) {
            return members.get(0); // Por ahora, devuelve el primer personaje creado
        }
        return null;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}
// Characters/factory/PlayerFactory.java
package Characters.factory;

import Characters.*;

public class PlayerFactory {
    // =========================================================
    //  FÁBRICA DE PERSONAJES JUGABLES
    // =========================================================

    // POR QUÉ static: no necesita estado propio, es un servicio puro de construcción
    public static Player create(String type, String name) {
        return switch (type.toLowerCase()) {
            case "warrior" -> new Warrior(name);
            case "mage"    -> new Mage(name);
            case "archer"  -> new Archer(name);
            // POR QUÉ lanzar excepción en vez de un default silencioso:
            // Fallar rápido y con un mensaje claro es mejor que crear un Warrior
            // "por defecto" cuando el tipo pedido era "Paladin" — eso es un bug oculto.
            default -> throw new IllegalArgumentException(
                    "Clase de héroe desconocida: '" + type + "'. Opciones: warrior, mage, archer");
        };
    }
}
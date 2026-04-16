package Management;

import Characters.Player;

public interface IPartyManager {
    // =========================================================
    //  OPERACIONES BÁSICAS DE GESTIÓN DEL GRUPO
    // =========================================================

    void addCharacter(Player p);
    void removeCharacter(String name);
    void listCharactersByType(String type);
}
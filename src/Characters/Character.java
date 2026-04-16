package Characters;

import java.io.Serializable;

public abstract class Character implements Serializable {
    // =========================================================
    //  ATRIBUTOS BASE DE CUALQUIER PERSONAJE
    // =========================================================

    // CAMBIO: todos private. Solo accesibles por métodos controlados.
    private String name;
    private int maxHp;
    private int hp;
    private int attack;
    private int defense;

    public Character(String name, int hp, int attack, int defense) {
        this.name = name;
        this.maxHp = hp;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
    }

    // =========================================================
    //  GETTERS Y SETTERS CONTROLADOS
    // =========================================================

    // --- Getters (lectura pública) ---
    public String getName()   { return name; }
    public int getHp()        { return hp; }
    public int getMaxHp()     { return maxHp; }
    public int getAttack()    { return attack; }
    public int getDefense()   { return defense; }

    // --- Setters protegidos (solo subclases pueden modificar stats) ---
    // POR QUÉ protected y no public: un enemigo no debería poder llamar
    // goblin.setAttack(9999) desde fuera. Solo la propia jerarquía gestiona sus stats.
    protected void setHp(int hp)           { this.hp = Math.max(0, hp); } // nunca negativo
    protected void setMaxHp(int maxHp)     { this.maxHp = maxHp; }
    public void setAttack(int attack)   { this.attack = attack; }
    protected void setDefense(int defense) { this.defense = defense; }

    // Método especial para restaurar estado desde archivo (Save/Load)
    public void restoreBaseState(int hp, int maxHp, int attack, int defense) {
        this.maxHp = maxHp;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
    }

    public boolean isAlive() { return hp > 0; }

    // =========================================================
    //  MECÁNICAS COMUNES DE COMBATE
    // =========================================================

    public void takeDamage(int dmg) {
        int realDamage = Math.max(1, dmg - defense);
        // Ahora usamos el setter: la lógica de "nunca negativo" vive en UN solo lugar
        setHp(this.hp - realDamage);
        System.out.printf("%s recibe %d de daño. HP: %d/%d%n",
                name, realDamage, this.hp, this.maxHp);
    }

    // Método de curación: lógica centralizada, no duplicada
    public void heal(int amount) {
        setHp(Math.min(this.hp + amount, this.maxHp)); // no superar maxHp
        System.out.printf("%s se cura %d HP. HP: %d/%d%n",
                name, amount, this.hp, this.maxHp);
    }

    // =========================================================
    //  CONTRATO DE ATAQUE PARA SUBCLASES
    // =========================================================

    public abstract int attack();
    public abstract void special(Character target);
}
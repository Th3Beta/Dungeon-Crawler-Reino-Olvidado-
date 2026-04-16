package Characters;

import Inventory.Weapon;
import Inventory.Armor;

public abstract class Player extends Character {
    // =========================================================
    //  PROGRESIÓN DEL JUGADOR (NIVEL, EXP Y MANÁ)
    // =========================================================

    private int level;
    private int exp;
    private int mana;
    private int maxMana;
    private final int specialManaCost;

    private Weapon equippedWeapon;
    private Armor equippedArmor;

    public Player(String name, int hp, int attack, int defense,
                  int maxMana, int specialManaCost) {
        super(name, hp, attack, defense);
        this.level = 1;
        this.exp = 0;
        this.maxMana = maxMana;
        this.mana = maxMana;
        this.specialManaCost = specialManaCost;
    }

    // =========================================================
    //  GETTERS — necesarios para que Game.java pueda leerlos
    // =========================================================

    public int getLevel()           { return level; }
    public int getExp()             { return exp; }
    public int getMana()            { return mana; }
    public int getMaxMana()         { return maxMana; }
    public int getSpecialManaCost() { return specialManaCost; }

    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public Armor getEquippedArmor()   { return equippedArmor; }

    public void equipWeapon(Weapon w) {
        this.equippedWeapon = w;
        System.out.println("Te has equipado el arma: " + w.getName());
    }

    public void equipArmor(Armor a) {
        this.equippedArmor = a;
        System.out.println("Te has equipado la armadura: " + a.getName());
    }

    // =========================================================
    //  RESTAURACIÓN ESTADO (SAVE / LOAD)
    // =========================================================

    public void restorePlayerState(int level, int exp, int mana, int maxMana) {
        this.level = level;
        this.exp = exp;
        this.mana = mana;
        this.maxMana = maxMana;
    }

    // =========================================================
    //  ATAQUE BASE
    // =========================================================

    @Override
    public int getDefense() {
        int baseDef = super.getDefense();
        return (equippedArmor != null) ? baseDef + equippedArmor.getDefenseBonus() : baseDef;
    }

    @Override
    public int attack() {
        int wBonus = (equippedWeapon != null) ? equippedWeapon.getAttackBonus() : 0;
        return super.getAttack() + (level * 2) + wBonus;
    }

    // =========================================================
    //  SISTEMA DE MANÁ
    // =========================================================

    public boolean canUseSpecial() {
        return mana >= specialManaCost;
    }

    protected void spendMana() {
        if (!canUseSpecial()) {
            System.out.println("¡Maná insuficiente! Necesitas " + specialManaCost
                    + " y tienes " + mana + ".");
            throw new IllegalStateException("Maná insuficiente");
        }
        mana -= specialManaCost;
    }

    public void regenMana(int amount) {
        mana = Math.min(mana + amount, maxMana);
    }

    // =========================================================
    //  SISTEMA DE EXPERIENCIA Y NIVEL
    // =========================================================

    public void gainExp(int amount) {
        exp += amount;
        System.out.printf("%s gana %d EXP. (Total: %d/100)%n", getName(), amount, exp);
        if (exp >= 100) {
            level++;
            exp -= 100; // conservar el exceso, no perderlo
            setMaxHp(getMaxHp() + 20);
            setHp(getMaxHp());        // curación completa al subir nivel
            setAttack(getAttack() + 3);
            setDefense(getDefense() + 2);
            maxMana += 10;
            mana = maxMana;
            System.out.printf(">>> ¡%s sube a NIVEL %d! Stats mejorados. <<<\n",
                    getName(), level);
        }
    }
}
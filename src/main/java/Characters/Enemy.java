package Characters;

public class Enemy extends Character {
    // =========================================================
    //  CONSTRUCTOR Y COMPORTAMIENTO BÁSICO ENEMIGO
    // =========================================================

    public Enemy(String name, int hp, int attack, int defense) {
        super(name, hp, attack, defense);
    }

    @Override
    public int attack() {
        return getAttack(); // CORRECCIÓN: Usar getter porque attack es private
    }

    @Override
    public void special(Character target) {
        // Los enemigos no tienen habilidad especial
    }
}
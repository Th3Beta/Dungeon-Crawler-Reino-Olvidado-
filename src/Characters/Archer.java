package Characters;

public class Archer extends Player {
    // =========================================================
    //  CONFIGURACIÓN BASE DEL ARQUERO
    // =========================================================

    public Archer(String name) {
        super(name, 90, 18, 6, 50, 18); // maxMana=50, coste especial=18
    }

    // =========================================================
    //  HABILIDAD ESPECIAL
    // =========================================================

    @Override
    public void special(Character target) {
        spendMana(); // CORRECCIÓN
        System.out.println(getName() + " dispara Flecha Crítica!");
        target.takeDamage(getAttack() + 15);
    }
}
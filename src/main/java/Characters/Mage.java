package Characters;

public class Mage extends Player {
    // =========================================================
    //  CONFIGURACIÓN BASE DEL MAGO
    // =========================================================

    public Mage(String name) {
        super(name, 80, 15, 5, 60, 20); // maxMana=60, coste especial=20
    }

    // =========================================================
    //  HABILIDAD ESPECIAL
    // =========================================================

    @Override
    public void special(Character target) {
        spendMana(); // CORRECCIÓN: Gasta maná
        System.out.println(getName() + " lanza Bola de Fuego!");
        target.takeDamage(getAttack() + 20);
    }
}
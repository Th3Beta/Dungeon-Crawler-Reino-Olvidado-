package Characters;

public class Warrior extends Player {
    // =========================================================
    //  CONFIGURACIÓN BASE DEL GUERRERO
    // =========================================================

    public Warrior(String name) {
        super(name, 120, 20, 10, 40, 15); // maxMana=40, coste especial=15
    }

    // =========================================================
    //  HABILIDAD ESPECIAL
    // =========================================================

    @Override
    public void special(Character target) { // CORRECCIÓN: Limpiado el "Characters.Character"
        spendMana(); // CORRECCIÓN: Ahora el ataque especial gasta maná
        System.out.println(getName() + " usa Golpe Fuerte!"); // CORRECCIÓN: getName()
        target.takeDamage(getAttack() + 10); // CORRECCIÓN: getAttack()
    }
}
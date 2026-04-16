package Characters;

public class Boss extends Enemy {

    public Boss(String name, int hp, int attack, int defense) {
        super("[BOSS] " + name, hp, attack, defense);
    }

    @Override
    public int attack() {
        System.out.println("¡El jefe " + getName() + " realiza un ataque devastador!");
        return (int)(getAttack() * 1.5);
    }
}

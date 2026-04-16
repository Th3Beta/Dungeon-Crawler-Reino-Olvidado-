// Characters/factory/EnemyFactory.java
package Characters.factory;

import Characters.Enemy;
import Characters.Boss;

public class EnemyFactory {
    // =========================================================
    //  FÁBRICA DE ENEMIGOS ESCALABLES
    // =========================================================

    public enum EnemyType { GOBLIN, ORC, DRAGON }

    public static Enemy create(EnemyType type, int floorLevel) {
        // Stats incrementados en un 15% por cada nivel de piso
        double multiplier = 1.0 + ((floorLevel - 1) * 0.15);

        return switch (type) {
            case GOBLIN -> new Enemy("Goblin", (int)(40*multiplier), (int)(10*multiplier), (int)(3*multiplier));
            case ORC    -> new Enemy("Orco",   (int)(80*multiplier), (int)(18*multiplier), (int)(8*multiplier));
            case DRAGON -> new Enemy("Dragón", (int)(200*multiplier), (int)(35*multiplier), (int)(15*multiplier));
        };
    }

    public static Enemy createRandom(int floorLevel) {
        EnemyType[] types = EnemyType.values();
        EnemyType random = types[(int)(Math.random() * types.length)];
        return create(random, floorLevel);
    }

    public static Boss createBoss(int floorLevel) {
        double multiplier = 1.0 + ((floorLevel - 1) * 0.20);
        return new Boss("Rey Demonio", (int)(400*multiplier), (int)(45*multiplier), (int)(20*multiplier));
    }
}
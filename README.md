# ⚔️ Reino Olvidado — Crónicas del Abismo

> *«En las profundidades del reino yacen secretos que los mortales han olvidado...»*

Un **RPG Roguelike** de mazmorras con estética medieval, desarrollado en **Java 17 + Spring Boot** para el backend y **HTML / CSS / JS Vanilla** para el frontend. Explora mazmorras generadas proceduralmente, combate enemigos y jefes, gestiona tu inventario y escala al tope del Salón de la Gloria.

---

## 🖼️ Características principales

| Característica | Descripción |
|---|---|
| 🗺️ **Mazmorras procedurales** | Pisos generados aleatoriamente con terrenos variados (agua, montañas, árboles, pasillos) |
| ⚔️ **Sistema de combate por turnos** | Ataques normales, habilidades especiales con coste de maná y uso de ítems mid-fight |
| 🧙 **3 clases jugables** | Guerrero, Mago y Arquero, cada uno con stats y especial únicos |
| 📈 **Progresión de personaje** | Sistema de niveles con EXP, mejora de stats y regeneración de maná por nivel |
| 🎒 **Inventario & Equipamiento** | Pociones, Elixires de Fuerza, Armas y Armaduras con bonificación real a stats |
| 💾 **Guardado persistente** | Serialización binaria (`game_save.dat`) y log de aventura en texto (`adventure_log.txt`) |
| 🏆 **Ranking global** | Puntuación persistente en `ranking.json`, consultable desde la UI sin recargar |
| 🌐 **Arquitectura full-stack** | Backend REST con Spring Boot + Frontend SPA sin frameworks |

---

## 🛠️ Stack tecnológico

**Backend**
- Java 17
- Spring Boot 3.2.4 (Spring Web + Tomcat embebido)
- Jackson Databind (serialización JSON)
- Maven (gestión de dependencias y build)

**Frontend**
- HTML5 / CSS3 / JavaScript Vanilla
- Google Fonts: *Cinzel*, *IM Fell English*, *Source Code Pro*
- Sin frameworks ni dependencias externas de JS

---

## 🗂️ Estructura del proyecto

```
PROYECTO IA/
├── src/main/java/
│   ├── Characters/          # Jerarquía de personajes
│   │   ├── Character.java   # Clase base abstracta
│   │   ├── Player.java      # Lógica de niveles, EXP y maná
│   │   ├── Warrior.java     # Especial: Golpe Aplastante
│   │   ├── Mage.java        # Especial: Bola de Fuego
│   │   ├── Archer.java      # Especial: Lluvia de Flechas
│   │   ├── Enemy.java       # Enemigos genéricos escalables
│   │   ├── Boss.java        # Enemigos jefe con stats aumentados
│   │   └── factory/         # Factory para instanciar personajes
│   ├── Combat/
│   │   └── Combat.java      # Motor de combate por turnos
│   ├── Inventory/
│   │   ├── Inventory.java   # Gestión del inventario del jugador
│   │   ├── Item.java        # Interfaz/abstracta de ítems
│   │   ├── Potion.java      # Curación de HP
│   │   ├── StrengthElixir.java # Buff temporal de ataque
│   │   ├── Weapon.java      # Equipamiento de ataque
│   │   └── Armor.java       # Equipamiento de defensa
│   ├── Management/
│   │   ├── FileManager.java # Guardado/carga binario + log de aventura
│   │   ├── Party.java       # Gestión del grupo de héroes
│   │   └── IPartyManager.java # Interfaz de gestión de grupo
│   ├── Map/
│   │   └── Map.java         # Generación procedural de mazmorras
│   └── com/rpg/
│       ├── RpgApplication.java  # Entry point Spring Boot
│       ├── WebConfig.java       # CORS y recursos estáticos
│       ├── web/
│       │   ├── GameController.java  # REST controller principal
│       │   ├── WebGame.java         # Sesión de juego por usuario
│       │   ├── GameStateDTO.java    # DTO de estado para el frontend
│       │   ├── RankingService.java  # Lectura/escritura ranking.json
│       │   └── RankingEntry.java    # Modelo de entrada del ranking
│       └── cli/                 # Modo consola (legacy)
├── web/
│   ├── index.html           # SPA — 5 pantallas en un único HTML
│   ├── style.css            # Diseño completo (~33 KB, medieval dark)
│   └── game.js              # Lógica cliente + comunicación con la API
├── game_save.dat            # Partida guardada (binario, generado en runtime)
├── adventure_log.txt        # Log de eventos de aventura (texto plano)
├── ranking.json             # Tabla de puntuaciones persistente
└── pom.xml                  # Configuración Maven
```

---

## 🚀 Cómo ejecutar

### Prerrequisitos
- **Java 17+** instalado y en el `PATH`
- **Maven 3.8+** (o usar el wrapper `mvnw.cmd` incluido)

### 1. Clonar el repositorio
```bash
git clone https://github.com/Th3Beta/Dungeon-Crawler-Reino-Olvidado-.git
cd "Dungeon-Crawler-Reino-Olvidado-"
```

### 2. Compilar y arrancar el servidor
```bash
# Con Maven instalado
mvn spring-boot:run

# O con el wrapper incluido (Windows)
mvnw.cmd spring-boot:run
```

### 3. Abrir el juego
Abre tu navegador en **[http://localhost:8080](http://localhost:8080)**

> El servidor sirve automáticamente el frontend desde la carpeta `web/`.

---

## 🎮 Cómo jugar

1. **Menú principal** → pulsa *Nueva Partida*
2. Escribe el **nombre de tu héroe** y elige tu **clase**:
   - ⚔️ **Guerrero** — 120 HP · 20 ATK · 10 DEF · Golpe Aplastante
   - 🔮 **Mago** — 80 HP · 15 ATK · 5 DEF · Bola de Fuego
   - 🏹 **Arquero** — 90 HP · 18 ATK · 6 DEF · Lluvia de Flechas
3. Explora el mapa con las teclas **W / A / S / D**
4. Al encontrar un enemigo se abre el **modal de combate** automáticamente
5. En combate puedes **Atacar**, usar tu **Especial** (cuesta maná) o consumir un **Ítem**
6. Presiona **I** para abrir el inventario fuera de combate
7. Presiona **X** o el botón *Guardar & Salir* para persistir la partida
8. Al morir o guardar, tu puntuación se envía al **Salón de la Gloria** 🏆

---

## 🌐 API REST

El backend expone los siguientes endpoints:

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/game/new` | Crea una sesión nueva y devuelve `gameId` + estado inicial |
| `POST` | `/api/game/{id}/action` | Envía una acción (`move_up`, `attack`, `special`, `save`, `quit`…) |
| `GET`  | `/api/game/{id}/state` | Consulta el estado actual de la partida |
| `GET`  | `/api/ranking` | Devuelve la lista de puntuaciones ordenada |

**Ejemplo — nueva partida:**
```http
POST /api/game/new
Content-Type: application/json

{
  "playerName": "Aragorn",
  "playerClass": "warrior"
}
```

---

## 📋 Sistema de puntuación

La puntuación final se calcula al morir o guardar:

```
Score = (Piso alcanzado × 100) + (Nivel × 50) + (Turnos sobrevividos × 2)
```

Las partidas quedan registradas en `ranking.json` y se muestran en el **Salón de la Gloria** ordenadas de mayor a menor puntuación.

---

## 🤝 Contribuir

1. Haz un fork del repositorio
2. Crea una rama para tu feature: `git checkout -b feature/mi-mejora`
3. Haz commit de tus cambios: `git commit -m "feat: descripción del cambio"`
4. Abre un Pull Request describiendo los cambios

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Consulta el archivo `LICENSE` para más detalles.

---

<p align="center">
  Hecho con ☕ Java y ⚔️ espíritu aventurero
</p>

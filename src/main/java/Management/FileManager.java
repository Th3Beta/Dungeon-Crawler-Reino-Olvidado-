package Management;

import java.io.*;

public class FileManager {

    private static final String SAVE_FILE = "game_save.dat";
    private static final String LOG_FILE = "adventure_log.txt";

    // =========================================================
    //  GUARDADO Y CARGA DEL JUEGO COMPLETO (BINARIO)
    // =========================================================

    public static void saveGame(Object game) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(game);
            System.out.println("Partida guardada correctamente en " + SAVE_FILE);
            logEvent("El jugador ha guardado la partida.");
        } catch (IOException e) {
            System.out.println("Error al guardar el juego: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Object loadGame() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            Object game = ois.readObject();
            System.out.println("Partida cargada exitosamente.");
            logEvent("El jugador ha retomado una partida anterior.");
            return game;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error al cargar la partida: " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    //  REGISTRO DE AVENTURA (LOG TEXT FILE)
    // =========================================================

    public static void logEvent(String event) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(event);
        } catch (IOException e) {
            // Ignorar
        }
    }
}

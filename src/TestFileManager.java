import Management.FileManager;

public class TestFileManager {
    public static void main(String[] args) {
        System.out.println("Iniciando test de guardado...");
        String testData = "Datos de prueba para el archivo binario";
        FileManager.saveGame(testData);
        System.out.println("Test finalizado.");
    }
}

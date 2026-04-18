package app;

public class Main {
    public static void main(String[] args) {
        // This tricks the Java runtime into starting the JavaFX environment correctly
        // from a non-modular Fat JAR.
        Launcher.main(args);
    }
}

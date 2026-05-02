package app;

public class Main {
    public static void main(String[] args) {
        System.setProperty("logback.configurationFile", "ui/logback.xml");
        Launcher.main(args);
    }
}

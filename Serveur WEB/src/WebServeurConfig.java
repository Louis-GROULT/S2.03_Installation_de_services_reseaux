// WebServeurConfig.java

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebServeurConfig {

    // --- Constantes pour le fichier de configuration et les valeurs par défaut ---
    private static final String CONFIG_FILE_PATH = "XML/conf.xml";

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_DOCUMENT_ROOT = Paths.get(System.getProperty("user.dir"), "www").toAbsolutePath().toString(); // Default to ./www
    private static final String DEFAULT_DIRECTORY_LISTING_SETTING = "off";
    private static final List<String> DEFAULT_ALLOWED_IPS = Arrays.asList("127.0.0.1", "::1");
    private static final List<String> DEFAULT_DENIED_IPS = new ArrayList<>(); // Par défaut, aucune IP spécifiquement refusée
    private static final String DEFAULT_ACCESS_LOG_PATH = null; // Par défaut, pas de log d'accès
    private static final String DEFAULT_ERROR_LOG_PATH = null;   // Par défaut, pas de log d'erreur

    // Variables pour stocker la configuration actuelle
    private int port;
    private String documentRoot;
    private String directoryListing;
    private List<String> allowedIps;
    private List<String> deniedIps;
    private String accessLogPath;
    private String errorLogPath;

    public WebServeurConfig() {
        // Initialiser avec les valeurs par défaut
        this.port = DEFAULT_PORT;
        this.documentRoot = DEFAULT_DOCUMENT_ROOT;
        this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
        this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS);
        this.deniedIps = new ArrayList<>(DEFAULT_DENIED_IPS);
        this.accessLogPath = DEFAULT_ACCESS_LOG_PATH;
        this.errorLogPath = DEFAULT_ERROR_LOG_PATH;

        loadConfiguration(); // Charger la configuration depuis le fichier XML
    }

    private void loadConfiguration() {
        // Charger le port
        String portString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "port");
        if (!portString.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(portString);
                if (parsedPort >= 0 && parsedPort <= 65535) { // Valider la plage de ports
                    this.port = parsedPort;
                } else {
                    System.out.println("Avertissement : Le port spécifié dans conf.xml est hors plage valide (0-65535). Utilisation du port par défaut : " + DEFAULT_PORT);
                    this.port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.out.println("Avertissement : Le port spécifié dans conf.xml est invalide. Utilisation du port par défaut : " + DEFAULT_PORT);
                this.port = DEFAULT_PORT;
            }
        } else {
            System.out.println("Avertissement : La balise <port> est introuvable ou vide dans conf.xml. Utilisation du port par défaut : " + DEFAULT_PORT);
        }

        // Charger le DocumentRoot
        String documentRootString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DocumentRoot");
        if (!documentRootString.isEmpty()) {
            File rootDir = new File(documentRootString);
            if (!rootDir.isAbsolute()) {
                rootDir = Paths.get(System.getProperty("user.dir"), documentRootString).toFile();
            }

            if (rootDir.exists() && rootDir.isDirectory() && rootDir.canRead()) {
                this.documentRoot = rootDir.getAbsolutePath();
            } else {
                System.out.println("Avertissement : Le DocumentRoot spécifié dans conf.xml est invalide ou inaccessible : " + documentRootString + ". Utilisation du DocumentRoot par défaut : " + DEFAULT_DOCUMENT_ROOT);
                this.documentRoot = DEFAULT_DOCUMENT_ROOT;
            }
        } else {
            System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide dans conf.xml. Utilisation du DocumentRoot par défaut : " + DEFAULT_DOCUMENT_ROOT);
        }

        // Charger DirectoryListing
        String directoryListingString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DirectoryListing");
        if (!directoryListingString.isEmpty()) {
            if ("on".equalsIgnoreCase(directoryListingString) || "off".equalsIgnoreCase(directoryListingString)) {
                this.directoryListing = directoryListingString.toLowerCase();
            } else {
                System.out.println("Avertissement : La valeur de <DirectoryListing> est invalide (" + directoryListingString + "). Utilisation de la valeur par défaut : " + DEFAULT_DIRECTORY_LISTING_SETTING);
                this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
            }
        } else {
            System.out.println("Avertissement : La balise <DirectoryListing> est introuvable ou vide. Utilisation de la valeur par défaut : " + DEFAULT_DIRECTORY_LISTING_SETTING);
        }

        // Charger les IPs autorisées
        String allowedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Allow");
        this.allowedIps.clear(); // Toujours effacer avant de recharger
        if (!allowedIpsString.isEmpty()) {
            for (String ip : allowedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.allowedIps.add(trimmedIp);
                }
            }
        }
        if (this.allowedIps.isEmpty()) { // Si la balise était là mais vide/mal formée ou non présente
            System.out.println("Avertissement : La balise <Allow> est introuvable ou vide/mal formée. Seules les IPs par défaut seront autorisées.");
            this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS); // Restaurer les IPs par défaut
        }


        // Charger les IPs refusées
        String deniedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Deny");
        this.deniedIps.clear(); // Toujours effacer avant de recharger
        if (!deniedIpsString.isEmpty()) {
            for (String ip : deniedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.deniedIps.add(trimmedIp);
                }
            }
        } else {
            System.out.println("Avertissement : La balise <Deny> est introuvable ou vide. Aucune IP ne sera spécifiquement refusée.");
        }

        // Charger le chemin du log d'accès
        String accessLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "AccessLog");
        if (!accessLogPathString.isEmpty()) {
            try {
                File logFile = new File(accessLogPathString);
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (parentDir.mkdirs()) {
                        System.out.println("Répertoire de log d'accès créé : " + parentDir.getAbsolutePath());
                    } else {
                        System.out.println("Avertissement : Impossible de créer le répertoire pour le log d'accès : " + parentDir.getAbsolutePath() + ". Le log pourrait ne pas fonctionner.");
                    }
                }
                this.accessLogPath = accessLogPathString;
            } catch (SecurityException e) {
                System.out.println("Erreur de sécurité : Impossible de créer ou d'accéder au chemin du log d'accès : " + accessLogPathString + " : " + e.getMessage());
                this.accessLogPath = DEFAULT_ACCESS_LOG_PATH;
            }
        } else {
            System.out.println("Avertissement : La balise <AccessLog> est introuvable ou vide. Le journal d'accès sera désactivé.");
            this.accessLogPath = DEFAULT_ACCESS_LOG_PATH; // S'assurer qu'il est null si non configuré
        }

        // Charger le chemin du log d'erreur
        String errorLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "ErrorLog");
        if (!errorLogPathString.isEmpty()) {
            try {
                File logFile = new File(errorLogPathString);
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (parentDir.mkdirs()) {
                        System.out.println("Répertoire de log d'erreur créé : " + parentDir.getAbsolutePath());
                    } else {
                        System.out.println("Avertissement : Impossible de créer le répertoire pour le log d'erreur : " + parentDir.getAbsolutePath() + ". Le log pourrait ne pas fonctionner.");
                    }
                }
                this.errorLogPath = errorLogPathString;
            } catch (SecurityException e) {
                System.out.println("Erreur de sécurité : Impossible de créer ou d'accéder au chemin du log d'erreur : " + errorLogPathString + " : " + e.getMessage());
                this.errorLogPath = DEFAULT_ERROR_LOG_PATH;
            }
        } else {
            System.out.println("Avertissement : La balise <ErrorLog> est introuvable ou vide. Le journal d'erreur sera désactivé.");
            this.errorLogPath = DEFAULT_ERROR_LOG_PATH; // S'assurer qu'il est null si non configuré
        }
    }


    // --- Getters pour accéder aux valeurs de configuration ---
    public int getPort() { return port; }

    public String getDocumentRoot() { return documentRoot; }

    public String getDirectoryListing() { return directoryListing; }

    public List<String> getAllowedIps() { return new ArrayList<>(allowedIps); } // Retourne une copie pour éviter les modifications externes

    public List<String> getDeniedIps() { return new ArrayList<>(deniedIps); }   // Retourne une copie

    public String getAccessLogPath() { return accessLogPath; }

    public String getErrorLogPath() { return errorLogPath; }
}
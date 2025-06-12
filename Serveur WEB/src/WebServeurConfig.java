// WebServeurConfig.java

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebServeurConfig {

    // --- Constantes pour le fichier de configuration et les valeurs par défaut ---
    private static final String CONFIG_FILE_PATH = "XML/conf.xml";

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_DOCUMENT_ROOT = System.getProperty("user.dir"); // Répertoire courant par défaut
    private static final String DEFAULT_DIRECTORY_LISTING_SETTING = "off";
    private static final List<String> DEFAULT_ALLOWED_IPS = Arrays.asList("127.0.0.1", "::1");
    private static final List<String> DEFAULT_DENIED_IPS = new ArrayList<>();
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
    private String activeConfigFilePath; // Pour indiquer d'où la config a été chargée

    public WebServeurConfig() { // Renommé de ServerConfig()
        // Initialiser avec les valeurs par défaut
        this.port = DEFAULT_PORT;
        this.documentRoot = DEFAULT_DOCUMENT_ROOT;
        this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
        this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS);
        this.deniedIps = new ArrayList<>(DEFAULT_DENIED_IPS);
        this.accessLogPath = DEFAULT_ACCESS_LOG_PATH;
        this.errorLogPath = DEFAULT_ERROR_LOG_PATH;
        this.activeConfigFilePath = CONFIG_FILE_PATH; // Par défaut, on cherche ici

        loadConfiguration();
    }

    private void loadConfiguration() {
        XmlValueExtracteur xmlValueExtracteur = new XmlValueExtracteur(); // Instancier XmlValueExtracteur

        // Charger le port
        String portStr = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "port");
        if (!portStr.isEmpty()) {
            try {
                this.port = Integer.parseInt(portStr);
                if (this.port < 0 || this.port > 65535) {
                    System.out.println("Avertissement : Le port spécifié dans conf.xml (" + portStr + ") est hors de la plage valide (0-65535). Utilisation du port par défaut : " + DEFAULT_PORT);
                    this.port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.out.println("Avertissement : Le port spécifié dans conf.xml n'est pas un nombre valide. Utilisation du port par défaut : " + DEFAULT_PORT);
                this.port = DEFAULT_PORT;
            }
        } else {
            System.out.println("Avertissement : La balise <port> est introuvable ou vide dans conf.xml. Utilisation du port par défaut : " + DEFAULT_PORT);
        }

        // Charger le DocumentRoot
        String documentRootStr = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DocumentRoot");
        if (!documentRootStr.isEmpty()) {
            File docRootFile = new File(documentRootStr);
            if (docRootFile.exists() && docRootFile.isDirectory()) {
                this.documentRoot = documentRootStr;
            } else {
                System.out.println("Avertissement : Le chemin DocumentRoot spécifié dans conf.xml (" + documentRootStr + ") n'existe pas ou n'est pas un répertoire. Utilisation du répertoire courant par défaut : " + DEFAULT_DOCUMENT_ROOT);
                this.documentRoot = DEFAULT_DOCUMENT_ROOT;
            }
        } else {
            System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide. Utilisation du répertoire courant par défaut : " + DEFAULT_DOCUMENT_ROOT);
        }

        // Charger DirectoryListing
        String directoryListingStr = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DirectoryListing");
        if (!directoryListingStr.isEmpty()) {
            if (directoryListingStr.equalsIgnoreCase("on") || directoryListingStr.equalsIgnoreCase("off")) {
                this.directoryListing = directoryListingStr.toLowerCase();
            } else {
                System.out.println("Avertissement : La valeur de DirectoryListing dans conf.xml doit être 'on' ou 'off'. Utilisation de la valeur par défaut : " + DEFAULT_DIRECTORY_LISTING_SETTING);
                this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
            }
        } else {
            System.out.println("Avertissement : La balise <DirectoryListing> est introuvable ou vide. Utilisation de la valeur par défaut : " + DEFAULT_DIRECTORY_LISTING_SETTING);
        }

        // Charger AllowedIps
        String allowedIpsString = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Allow");
        if (!allowedIpsString.isEmpty()) {
            this.allowedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
            for (String ip : allowedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.allowedIps.add(trimmedIp);
                }
            }
            if (this.allowedIps.isEmpty()) { // Si la balise était là mais vide/mal formée
                System.out.println("Avertissement : La balise <Allow> ne contient pas d'IPs valides. Toutes les IPs seront autorisées par défaut.");
                this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS); // Revenir aux valeurs par défaut si vide
            }
        } else {
            System.out.println("Avertissement : La balise <Allow> est introuvable ou vide. Toutes les IPs seront autorisées par défaut.");
            // Maintenir les valeurs par défaut initiales si la balise est absente ou vide
        }


        // Charger DeniedIps
        String deniedIpsString = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Deny");
        if (!deniedIpsString.isEmpty()) {
            this.deniedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
            for (String ip : deniedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.deniedIps.add(trimmedIp);
                }
            }
            if (this.deniedIps.isEmpty()) { // Si la balise était là mais vide/mal formée
                System.out.println("Avertissement : La balise <Deny> ne contient pas d'IPs valides. Aucune IP ne sera spécifiquement refusée.");
            }
        } else {
            System.out.println("Avertissement : La balise <Deny> est introuvable ou vide. Aucune IP ne sera spécifiquement refusée.");
        }

        // Charger AccessLog
        String accessLogPathString = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "AccessLog");
        if (!accessLogPathString.isEmpty()) {
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
        } else {
            System.out.println("Avertissement : La balise <AccessLog> est introuvable ou vide. Le journal d'accès sera désactivé.");
            this.accessLogPath = DEFAULT_ACCESS_LOG_PATH; // S'assurer qu'il est null si non configuré
        }

        // Charger ErrorLog
        String errorLogPathString = xmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "ErrorLog");
        if (!errorLogPathString.isEmpty()) {
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
        } else {
            System.out.println("Avertissement : La balise <ErrorLog> est introuvable ou vide. Le journal d'erreur sera désactivé.");
            this.errorLogPath = DEFAULT_ERROR_LOG_PATH; // S'assurer qu'il est null si non configuré
        }
    }

    // --- Getters pour accéder aux valeurs de configuration ---
    public int getPort() { return port; }

    public String getDocumentRoot() { return documentRoot; }

    public String getDirectoryListing() { return directoryListing; }

    public List<String> getAllowedIps() { return new ArrayList<>(allowedIps); }

    public List<String> getDeniedIps() { return new ArrayList<>(deniedIps); }

    public String getAccessLogPath() { return accessLogPath; }

    public String getErrorLogPath() { return errorLogPath; }

    public String getActiveConfigFilePath() {
        return activeConfigFilePath;
    }
}
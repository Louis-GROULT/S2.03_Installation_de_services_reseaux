// WebServeurConfig.java

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebServeurConfig {

    // --- Constantes pour le fichier de configuration et les valeurs par défaut ---\
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

    public WebServeurConfig() {
        // Initialiser avec les valeurs par défaut
        this.port = DEFAULT_PORT;
        this.documentRoot = DEFAULT_DOCUMENT_ROOT;
        this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
        this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS);
        this.deniedIps = new ArrayList<>(DEFAULT_DENIED_IPS);
        this.accessLogPath = DEFAULT_ACCESS_LOG_PATH;
        this.errorLogPath = DEFAULT_ERROR_LOG_PATH;

        loadConfiguration(); // Charger la configuration à partir du fichier XML
    }

    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists() || !configFile.isFile()) {
            System.out.println("Avertissement : Fichier de configuration " + CONFIG_FILE_PATH + " non trouvé ou n'est pas un fichier. Utilisation des valeurs par défaut.");
            this.activeConfigFilePath = null; // Pas de fichier de configuration externe chargé
            return; // Utiliser les valeurs par défaut déjà initialisées
        }

        this.activeConfigFilePath = configFile.getAbsolutePath(); // Chemin du fichier chargé
        XmlValueExtracteur XmlValueExtracteur = new XmlValueExtracteur(); // Instancier XmlValueExtracteur

        // --- Récupération du port ---
        String portString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "port");
        if (!portString.isEmpty()) {
            try {
                this.port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                System.out.println("Avertissement : La valeur de la balise <port> ('" + portString + "') n'est pas un nombre valide. Utilisation du port par défaut (" + DEFAULT_PORT + ").");
            }
        } else {
            System.out.println("Avertissement : La balise <port> est introuvable ou vide. Utilisation du port par défaut (" + DEFAULT_PORT + ").");
        }

        // --- Récupération du DocumentRoot ---
        String documentRootString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DocumentRoot");
        if (!documentRootString.isEmpty()) {
            // Vérifier si le chemin existe et est un répertoire
            File docRootFile = new File(documentRootString);
            if (docRootFile.exists() && docRootFile.isDirectory()) {
                this.documentRoot = documentRootString;
            } else {
                System.out.println("Avertissement : Le DocumentRoot spécifié dans la balise <DocumentRoot> ('" + documentRootString + "') n'existe pas ou n'est pas un répertoire. Utilisation du répertoire courant par défaut (" + DEFAULT_DOCUMENT_ROOT + ").");
            }
        } else {
            System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide. Utilisation du répertoire courant par défaut (" + DEFAULT_DOCUMENT_ROOT + ").");
        }

        // --- Récupération du DirectoryListing ---
        String directoryListingString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Directory");
        if (!directoryListingString.isEmpty()) {
            if (directoryListingString.equalsIgnoreCase("on") || directoryListingString.equalsIgnoreCase("off")) {
                this.directoryListing = directoryListingString.toLowerCase();
            } else {
                System.out.println("Avertissement : La valeur de la balise <Directory> ('" + directoryListingString + "') est invalide. Attendu 'on' ou 'off'. Utilisation de la valeur par défaut (" + DEFAULT_DIRECTORY_LISTING_SETTING + ").");
            }
        } else {
            System.out.println("Avertissement : La balise <Directory> est introuvable ou vide. Utilisation de la valeur par défaut (" + DEFAULT_DIRECTORY_LISTING_SETTING + ").");
        }

        // --- Récupération des IPs autorisées (Allow) ---
        String allowedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Allow");
        if (!allowedIpsString.isEmpty()) {
            this.allowedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
            String[] ips = allowedIpsString.split(",");
            boolean ipsFound = false;
            for (String ip : ips) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.allowedIps.add(trimmedIp);
                    ipsFound = true;
                }
            }
            if (!ipsFound) {
                System.out.println("Avertissement : La balise <Allow> ne contient pas d'IPs valides. Aucune IP ne sera spécifiquement autorisée (toutes non refusées seront implicitement autorisées si Deny est vide).");
                // Si la balise Allow est présente mais ne contient aucune IP valide,
                // nous pourrions vouloir revenir au comportement par défaut (toutes IPs autorisées)
                // ou maintenir une liste vide. Pour l'instant, une liste vide signifie "aucune IP explicitement autorisée".
                // Les IPs par défaut sont déjà initialisées dans le constructeur, donc si on vide et rien n'est ajouté,
                // la liste reste vide si le XML ne contient rien de valide.
                // Ici, on remet les IPs par défaut si Allow est vide ou mal formée:
                this.allowedIps.addAll(DEFAULT_ALLOWED_IPS); // Retablir les valeurs par défaut
            }
        } else {
            System.out.println("Avertissement : La balise <Allow> est introuvable ou vide. Utilisation des IPs autorisées par défaut (" + DEFAULT_ALLOWED_IPS + ").");
            // Les IPs par défaut sont déjà là, pas besoin de les rajouter.
        }

        // --- Récupération des IPs refusées (Deny) ---
        String deniedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Deny");
        if (!deniedIpsString.isEmpty()) {
            this.deniedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
            String[] ips = deniedIpsString.split(",");
            boolean ipsFound = false;
            for (String ip : ips) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.deniedIps.add(trimmedIp);
                    ipsFound = true;
                }
            }
            if (!ipsFound) {
                System.out.println("Avertissement : La balise <Deny> ne contient pas d'IPs valides. Aucune IP ne sera spécifiquement refusée.");
            }
        } else {
            System.out.println("Avertissement : La balise <Deny> est introuvable ou vide. Aucune IP ne sera spécifiquement refusée.");
        }

        // --- Récupération du chemin du log d'accès (AccessLog) ---
        String accessLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "AccessLog");
        if (!accessLogPathString.isEmpty()) {
            // Optionnel : vérifier si le répertoire parent existe ou est créable
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

        // --- Récupération du chemin du log d'erreur (ErrorLog) ---
        String errorLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "ErrorLog");
        if (!errorLogPathString.isEmpty()) {
            // Optionnel : vérifier si le répertoire parent existe ou est créable
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

    public String getActiveConfigFilePath() { return activeConfigFilePath; }
}
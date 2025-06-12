// WebServeurConfig.java
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class WebServeurConfig {

    // --- Critères pour la recherche du fichier XML ---
    private static final String EXPECTED_ROOT_TAG = "WebServerConfiguration"; // Nom de la balise racine attendue

    // Les répertoires où chercher les fichiers XML.
    // L'ordre est important : le premier trouvé est utilisé.
    private static final String[] CANDIDATE_SEARCH_DIRECTORIES = {
            ".",                       // 1. Répertoire courant de l'exécution
            "XML",                     // 2. Sous-répertoire "XML"
            "config",                  // 3. Sous-répertoire "config"
            "/tmp/etc/myweb"           // 4. Chemin de déploiement spécifié pour Linux (selon PDF)
            // Ajoutez ici d'autres chemins absolus spécifiques pour Windows ou macOS si vous en avez :
            // "C:\\Program Files\\MyWebServer\\config", // Exemple pour Windows
            // "/Library/Application Support/MyWebServer/config" // Exemple pour macOS
    };

    // --- Constantes pour les valeurs par défaut (si aucun fichier n'est trouvé ou balise manquante) ---
    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_DOCUMENT_ROOT = System.getProperty("user.dir");
    private static final String DEFAULT_DIRECTORY_LISTING_SETTING = "off";
    private static final List<String> DEFAULT_ALLOWED_IPS = Arrays.asList("127.0.0.1", "::1"); // IPs par défaut toujours autorisées
    private static final List<String> DEFAULT_DENIED_IPS = new ArrayList<>(); // Aucune IP refusée par défaut
    private static final String DEFAULT_ACCESS_LOG_PATH = null; // Aucun log par défaut, à définir dans le XML
    private static final String DEFAULT_ERROR_LOG_PATH = null;  // Aucun log par défaut, à définir dans le XML


    // Variables pour stocker la configuration actuelle
    private int port;
    private String documentRoot;
    private String directoryListing;
    private List<String> allowedIps;
    private List<String> deniedIps;
    private String accessLogPath;
    private String errorLogPath;
    private String activeConfigFilePath; // Chemin du fichier de config réellement utilisé

    public WebServeurConfig() {
        // Initialiser avec les valeurs par défaut au cas où le fichier de config ne soit pas trouvé ou soit invalide
        this.port = DEFAULT_PORT;
        this.documentRoot = DEFAULT_DOCUMENT_ROOT;
        this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
        this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS); // Créer une copie modifiable
        this.deniedIps = new ArrayList<>(DEFAULT_DENIED_IPS);   // Créer une copie modifiable
        this.accessLogPath = DEFAULT_ACCESS_LOG_PATH;
        this.errorLogPath = DEFAULT_ERROR_LOG_PATH;

        loadConfiguration(); // Tenter de charger la configuration depuis un fichier
    }

    private void loadConfiguration() {
        String foundConfigPath = findConfigFile();

        if (foundConfigPath == null) {
            System.out.println("Avertissement : Aucun fichier de configuration XML valide trouvé avec la balise racine '" + EXPECTED_ROOT_TAG + "' dans les répertoires de recherche. Le serveur utilisera toutes les configurations par défaut.");
            return; // Arrêter le chargement si aucun fichier n'est trouvé
        }

        this.activeConfigFilePath = foundConfigPath;
        System.out.println("Fichier de configuration trouvé et chargé depuis : " + activeConfigFilePath);

        // --- Lecture de la balise <port> ---
        String portString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "port");
        if (!portString.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(portString);
                if (parsedPort >= 0 && parsedPort <= 65535) {
                    this.port = parsedPort;
                } else {
                    System.out.println("Avertissement : La valeur du port '" + parsedPort + "' est hors de la plage valide (0-65535) dans " + activeConfigFilePath + ". Utilisation du port par défaut : " + this.port);
                }
            } catch (NumberFormatException e) {
                System.out.println("Erreur : La valeur de la balise <port> '" + portString + "' n'est pas un nombre valide dans " + activeConfigFilePath + ". Utilisation du port par défaut : " + this.port);
            }
        } else {
            System.out.println("Avertissement : La balise <port> est introuvable ou vide dans " + activeConfigFilePath + ". Utilisation du port par défaut : " + this.port);
        }

        // --- Lecture de la balise <DocumentRoot> ---
        String docRootString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "DocumentRoot");
        if (!docRootString.isEmpty()) {
            File rootDir = new File(docRootString);
            if (rootDir.isDirectory() && rootDir.exists()) {
                this.documentRoot = docRootString;
            } else {
                System.out.println("Avertissement : Le chemin DocumentRoot '" + docRootString + "' n'est pas un répertoire valide ou n'existe pas dans " + activeConfigFilePath + ". Utilisation du répertoire par défaut : " + this.documentRoot);
            }
        } else {
            System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide dans " + activeConfigFilePath + ". Utilisation du répertoire par défaut (répertoire courant d'exécution) : " + this.documentRoot);
        }

        // --- Lecture de la balise <Directory> (pour l'affichage des répertoires) ---
        String directoryString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "Directory");
        if (!directoryString.isEmpty()) {
            if (directoryString.equalsIgnoreCase("on") || directoryString.equalsIgnoreCase("off")) {
                this.directoryListing = directoryString.toLowerCase();
            } else {
                System.out.println("Avertissement : La valeur de la balise <Directory> '" + directoryString + "' n'est pas 'on' ou 'off' dans " + activeConfigFilePath + ". Utilisation de la valeur par défaut : '" + this.directoryListing + "'.");
            }
        } else {
            System.out.println("Avertissement : La balise <Directory> est introuvable ou vide dans " + activeConfigFilePath + ". Utilisation de la valeur par défaut : '" + this.directoryListing + "'.");
        }

        // --- Lecture de la balise <Allow> (pour les IPs autorisées) ---
        String allowedIpsString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "Allow");
        if (!allowedIpsString.isEmpty()) {
            this.allowedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
            for (String ip : allowedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.allowedIps.add(trimmedIp);
                }
            }
            if (this.allowedIps.isEmpty()) {
                System.out.println("Avertissement : La balise <Allow> ne contient pas d'IPs valides dans " + activeConfigFilePath + ". Revert aux IPs autorisées par défaut : " + DEFAULT_ALLOWED_IPS);
                this.allowedIps.addAll(DEFAULT_ALLOWED_IPS); // Revenir aux valeurs par défaut si la balise est présente mais vide/invalide
            }
        } else {
            System.out.println("Avertissement : La balise <Allow> est introuvable ou vide dans " + activeConfigFilePath + ". Utilisation des IPs autorisées par défaut : " + this.allowedIps);
        }

        // --- Lecture de la balise <Deny> (pour les IPs refusées) ---
        String deniedIpsString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "Deny");
        if (!deniedIpsString.isEmpty()) {
            this.deniedIps.clear(); // Efface les valeurs par défaut (qui sont vides) avant d'ajouter celles du XML
            for (String ip : deniedIpsString.split(",")) {
                String trimmedIp = ip.trim();
                if (!trimmedIp.isEmpty()) {
                    this.deniedIps.add(trimmedIp);
                }
            }
            if (this.deniedIps.isEmpty()) {
                System.out.println("Avertissement : La balise <Deny> ne contient pas d'IPs valides dans " + activeConfigFilePath + ". Aucune IP ne sera spécifiquement refusée.");
            }
        } else {
            System.out.println("Avertissement : La balise <Deny> est introuvable ou vide dans " + activeConfigFilePath + ". Aucune IP ne sera spécifiquement refusée.");
        }

        // --- Lecture de la balise <AccessLog> ---
        String accessLogString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "AccessLog");
        if (!accessLogString.isEmpty()) {
            Path logPath = Paths.get(accessLogString);
            // Vérifier si le répertoire parent existe ou peut être créé.
            // Si le chemin du log est juste un nom de fichier sans répertoire, le parent est null.
            // Dans ce cas, le fichier sera créé dans le répertoire d'exécution.
            if (logPath.getParent() == null || Files.exists(logPath.getParent()) || logPath.getParent().toFile().mkdirs()) {
                this.accessLogPath = accessLogString;
                System.out.println("Fichier de log d'accès configuré : " + this.accessLogPath);
            } else {
                System.out.println("Avertissement : Le chemin du journal d'accès '" + accessLogString + "' est invalide ou le répertoire parent ne peut être créé. Le journal d'accès sera désactivé.");
                this.accessLogPath = null;
            }
        } else {
            System.out.println("Avertissement : La balise <AccessLog> est introuvable ou vide dans " + activeConfigFilePath + ". Le journal d'accès sera désactivé.");
            this.accessLogPath = null;
        }

        // --- Lecture de la balise <ErrorLog> ---
        String errorLogString = XmlValueExtracteur.getTagTextValue(activeConfigFilePath, "ErrorLog");
        if (!errorLogString.isEmpty()) {
            Path logPath = Paths.get(errorLogString);
            if (logPath.getParent() == null || Files.exists(logPath.getParent()) || logPath.getParent().toFile().mkdirs()) {
                this.errorLogPath = errorLogString;
                System.out.println("Fichier de log d'erreur configuré : " + this.errorLogPath);
            } else {
                System.out.println("Avertissement : Le chemin du journal d'erreur '" + errorLogString + "' est invalide ou le répertoire parent ne peut être créé. Le journal d'erreur sera désactivé.");
                this.errorLogPath = null;
            }
        } else {
            System.out.println("Avertissement : La balise <ErrorLog> est introuvable ou vide dans " + activeConfigFilePath + ". Le journal d'erreur sera désactivé.");
            this.errorLogPath = null;
        }
    }

    /**
     * Recherche le fichier de configuration XML en parcourant des répertoires candidats
     * et en vérifiant que le fichier trouvé se termine par .xml ou .conf et possède la balise racine attendue.
     * @return Le chemin absolu du premier fichier XML valide trouvé, ou null si aucun fichier n'est trouvé.
     */
    private String findConfigFile() {
        for (String dirPath : CANDIDATE_SEARCH_DIRECTORIES) {
            Path directory = Paths.get(dirPath);
            // Vérifier si le répertoire existe et est un répertoire
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                System.out.println("Info : Répertoire de recherche non valide ou inexistant : " + directory.toAbsolutePath());
                continue; // Passer au répertoire suivant
            }

            System.out.println("Info : Recherche de fichiers XML dans le répertoire : " + directory.toAbsolutePath());

            try (Stream<Path> walk = Files.walk(directory, 1)) { // Profondeur max 1: ne pas chercher dans les sous-répertoires
                List<Path> candidateFiles = walk
                        .filter(Files::isRegularFile) // Ne considérer que les fichiers réguliers
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xml") || p.getFileName().toString().toLowerCase().endsWith(".conf")) // Filtrer par l'extension .xml OU .conf
                        .toList(); // Collecter tous les fichiers .xml/.conf à ce niveau

                for (Path file : candidateFiles) {
                    System.out.println("Info : Test du fichier de configuration potentiel : " + file.toAbsolutePath());
                    // Tenter d'extraire la balise racine
                    String rootTagName = XmlValueExtracteur.getRootElementName(file.toAbsolutePath().toString());

                    // Si la balise racine correspond à celle attendue, c'est notre fichier de configuration
                    if (EXPECTED_ROOT_TAG.equals(rootTagName)) {
                        return file.toAbsolutePath().toString(); // Retourne le chemin absolu du fichier trouvé et validé
                    } else {
                        System.out.println("Info : Le fichier '" + file.getFileName() + "' n'est pas le fichier de configuration attendu (balise racine '" + (rootTagName != null ? rootTagName : "N/A") + "' au lieu de '" + EXPECTED_ROOT_TAG + "').");
                    }
                }
            } catch (IOException e) {
                System.out.println("Erreur lors de la lecture du répertoire " + directory.toAbsolutePath() + " : " + e.getMessage());
            }
        }
        return null; // Aucun fichier valide n'a été trouvé après avoir parcouru tous les répertoires candidats
    }

    // --- Getters pour accéder aux valeurs de configuration ---
    public int getPort() { return port; }
    public String getDocumentRoot() { return documentRoot; }
    public String getDirectoryListing() { return directoryListing; }
    public List<String> getAllowedIps() { return new ArrayList<>(allowedIps); } // Retourne une copie pour éviter les modifications externes
    public List<String> getDeniedIps() { return new ArrayList<>(deniedIps); }   // Retourne une copie
    public String getAccessLogPath() { return accessLogPath; }
    public String getErrorLogPath() { return errorLogPath; }
    public String getActiveConfigFilePath() { return activeConfigFilePath; }
}
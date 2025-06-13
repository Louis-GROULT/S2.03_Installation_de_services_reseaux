import java.io.File;
import java.io.IOException; // Ajout de l'import pour IOException
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

    public class WebServeurConfig {

        // --- Constantes pour le fichier de configuration et les valeurs par défaut ---
        // Correction ici : le chemin du fichier de configuration doit être /tmp/etc/myweb/myweb.conf
        private static final String CONFIG_FILE_PATH = "/tmp/etc/myweb/myweb.conf";

        private static final int DEFAULT_PORT = 80; // Utilisation du port 80 par défaut comme demandé
        private static final String DEFAULT_DOCUMENT_ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString(); // Répertoire racine par défaut selon la SAE
        private static final String DEFAULT_DIRECTORY_LISTING_SETTING = "off"; // Par défaut, désactivé
        private static final List<String> DEFAULT_ALLOWED_IPS = Arrays.asList("127.0.0.1", "::1"); // IPs locales
        private static final List<String> DEFAULT_DENIED_IPS = new ArrayList<>(); // Aucune IP refusée par défaut
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

            loadConfiguration(); // Charger la configuration à partir du fichier XML
        }

        private void loadConfiguration() {
            System.out.println("Chargement de la configuration depuis : " + CONFIG_FILE_PATH);
            File configFile = new File(CONFIG_FILE_PATH);

            if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
                System.out.println("Avertissement : Fichier de configuration " + CONFIG_FILE_PATH + " introuvable ou illisible. Utilisation des valeurs par défaut.");
                // Si le fichier n'est pas trouvé ou accessible, on garde les valeurs par défaut déjà initialisées
                return;
            }

            try {
                // Utiliser XmlValueExtracteur pour lire les valeurs
                String portString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "port");
                if (!portString.isEmpty()) {
                    try {
                        int parsedPort = Integer.parseInt(portString);
                        if (parsedPort >= 0 && parsedPort <= 65535) { // Vérifier la validité du port
                            this.port = parsedPort;
                        } else {
                            System.out.println("Avertissement : Port configuré invalide (" + parsedPort + "). Utilisation du port par défaut : " + DEFAULT_PORT); // System.out.println
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Avertissement : Le port configuré n'est pas un nombre valide. Utilisation du port par défaut : " + DEFAULT_PORT); // System.out.println
                    }
                } else {
                    System.out.println("Avertissement : La balise <port> est introuvable ou vide. Utilisation du port par défaut : " + DEFAULT_PORT);
                }

                String documentRootString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DocumentRoot");
                if (!documentRootString.isEmpty()) {
                    // S'assurer que le documentRoot est un chemin absolu et valide
                    File rootDir = new File(documentRootString);
                    // Utiliser getCanonicalPath pour résoudre les chemins relatifs et les ".."
                    // et s'assurer que le répertoire existe et est accessible
                    try {
                        if (rootDir.exists() && rootDir.isDirectory() && rootDir.canRead()) {
                            this.documentRoot = rootDir.getCanonicalPath(); // Assurer un chemin canonique
                        } else {
                            System.out.println("Avertissement : DocumentRoot configuré (" + documentRootString + ") est invalide ou inaccessible. Utilisation du DocumentRoot par défaut : " + DEFAULT_DOCUMENT_ROOT); // System.out.println
                        }
                    } catch (IOException e) {
                        System.out.println("Avertissement : Erreur lors de la résolution du chemin canonical de DocumentRoot (" + documentRootString + "). Utilisation du DocumentRoot par défaut : " + DEFAULT_DOCUMENT_ROOT + " : " + e.getMessage()); // System.out.println
                    }
                } else {
                    System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide. Utilisation du DocumentRoot par défaut : " + DEFAULT_DOCUMENT_ROOT);
                }

                String directoryListingString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DirectoryListing");
                if (!directoryListingString.isEmpty()) {
                    // Normaliser la valeur pour être insensible à la casse
                    this.directoryListing = directoryListingString.toLowerCase();
                } else {
                    System.out.println("Avertissement : La balise <DirectoryListing> est introuvable ou vide. Utilisation de la valeur par défaut : " + DEFAULT_DIRECTORY_LISTING_SETTING);
                }

                String allowedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Allow");
                if (!allowedIpsString.isEmpty()) {
                    this.allowedIps.clear(); // Efface les valeurs par défaut avant d'ajouter celles du XML
                    for (String ip : allowedIpsString.split(",")) {
                        String trimmedIp = ip.trim();
                        if (!trimmedIp.isEmpty()) {
                            this.allowedIps.add(trimmedIp);
                        }
                    }
                    if (this.allowedIps.isEmpty()) { // Si la balise était là mais vide/mal formée
                        System.out.println("Avertissement : La balise <Allow> ne contient pas d'IPs valides. Aucune IP ne sera spécifiquement autorisée (toutes les IPs sont autorisées si 'Allow' est vide).");
                        this.allowedIps = new ArrayList<>(); // Réinitialiser à une liste vide si la configuration explicite est vide
                    }
                } else {
                    System.out.println("Avertissement : La balise <Allow> est introuvable ou vide. Toutes les IPs seront autorisées.");
                    this.allowedIps = new ArrayList<>(); // Si la balise n'existe pas, toutes les IPs sont autorisées
                }


                String deniedIpsString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Deny");
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

                // Chargement et validation du chemin du log d'accès
                String accessLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "AccessLog");
                if (!accessLogPathString.isEmpty()) {
                    try {
                        File logFile = new File(accessLogPathString);
                        File parentDir = logFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            if (parentDir.mkdirs()) {
                                System.out.println("Répertoire de log d'accès créé : " + parentDir.getAbsolutePath());
                            } else {
                                System.out.println("Avertissement : Impossible de créer le répertoire pour le log d'accès : " + parentDir.getAbsolutePath() + ". Le log pourrait ne pas fonctionner."); // System.out.println
                            }
                        }
                        this.accessLogPath = accessLogPathString;
                    } catch (SecurityException e) {
                        System.out.println("Erreur: Impossible de créer ou d'accéder au chemin du log d'accès : " + accessLogPathString + " : " + e.getMessage()); // System.out.println
                        this.accessLogPath = DEFAULT_ACCESS_LOG_PATH; // Désactiver si problème
                    }
                } else {
                    System.out.println("Avertissement : La balise <AccessLog> est introuvable ou vide. Le journal d'accès sera désactivé.");
                    this.accessLogPath = DEFAULT_ACCESS_LOG_PATH; // S'assurer qu'il est null si non configuré
                }

                // Chargement et validation du chemin du log d'erreur
                String errorLogPathString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "ErrorLog");
                if (!errorLogPathString.isEmpty()) {
                    try {
                        File logFile = new File(errorLogPathString);
                        File parentDir = logFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            if (parentDir.mkdirs()) {
                                System.out.println("Répertoire de log d'erreur créé : " + parentDir.getAbsolutePath());
                            } else {
                                System.out.println("Avertissement : Impossible de créer le répertoire pour le log d'erreur : " + parentDir.getAbsolutePath() + ". Le log pourrait ne pas fonctionner."); // System.out.println
                            }
                        }
                        this.errorLogPath = errorLogPathString;
                    } catch (SecurityException e) {
                        System.out.println("Erreur: Impossible de créer ou d'accéder au chemin du log d'erreur : " + errorLogPathString + " : " + e.getMessage()); // System.out.println
                        this.errorLogPath = DEFAULT_ERROR_LOG_PATH; // Désactiver si problème
                    }
                } else {
                    System.out.println("Avertissement : La balise <ErrorLog> est introuvable ou vide. Le journal d'erreur sera désactivé.");
                    this.errorLogPath = DEFAULT_ERROR_LOG_PATH; // S'assurer qu'il est null si non configuré
                }

            } catch (Exception e) {
                System.out.println("Erreur critique lors du parsing du fichier de configuration : " + e.getMessage()); // System.out.println
                // Les valeurs par défaut seront utilisées
            }
        }

        // --- Getters pour accéder aux valeurs de configuration ---
        public int getPort() { return this.port; }

        public String getDocumentRoot() { return this.documentRoot; }

        public String getDirectoryListing() { return this.directoryListing; }

        public List<String> getAllowedIps() { return new ArrayList<>(allowedIps); } // Retourne une copie défensive

        public List<String> getDeniedIps() { return new ArrayList<>(deniedIps); }   // Retourne une copie défensive

        public String getAccessLogPath() { return accessLogPath; }

        public String getErrorLogPath() { return errorLogPath; }
    }
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
    private static final String DEFAULT_DOCUMENT_ROOT = System.getProperty("user.dir"); // Répertoire courant par défaut
    private static final String DEFAULT_DIRECTORY_LISTING_SETTING = "off";
    private static final List<String> DEFAULT_ALLOWED_IPS = Arrays.asList("127.0.0.1", "::1");
    private static final List<String> DEFAULT_DENIED_IPS = new ArrayList<>();

    // Variables pour stocker la configuration actuelle
    private int port;
    private String documentRoot;
    private String directoryListing;
    private List<String> allowedIps;
    private List<String> deniedIps;

    public WebServeurConfig() { // Renommé de ServerConfig()
        // Initialiser avec les valeurs par défaut
        this.port = DEFAULT_PORT;
        this.documentRoot = DEFAULT_DOCUMENT_ROOT;
        this.directoryListing = DEFAULT_DIRECTORY_LISTING_SETTING;
        this.allowedIps = new ArrayList<>(DEFAULT_ALLOWED_IPS);
        this.deniedIps = new ArrayList<>(DEFAULT_DENIED_IPS);

        loadConfiguration(); // Charger la configuration au moment de l'instanciation
    }

    private void loadConfiguration() {
        System.out.println("Tentative de chargement de la configuration depuis : " + CONFIG_FILE_PATH);

        // Port
        String portString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "port");
        if (!portString.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(portString);
                if (parsedPort >= 0 && parsedPort <= 65535) {
                    this.port = parsedPort;
                } else {
                    System.out.println("Avertissement : La valeur du port '" + portString + "' est hors de la plage valide (0-65535). Utilisation du port par défaut : " + this.port);
                }
            } catch (NumberFormatException e) {
                System.out.println("Erreur : La valeur de la balise <port> '" + portString + "' n'est pas un nombre valide. Utilisation du port par défaut : " + this.port);
            }
        } else {
            System.out.println("Avertissement : La balise <port> est introuvable ou vide. Utilisation du port par défaut : " + this.port);
        }

        // DocumentRoot
        String docRootString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "DocumentRoot");
        if (!docRootString.isEmpty()) {
            File rootDir = new File(docRootString);
            if (rootDir.isDirectory() && rootDir.exists()) {
                this.documentRoot = docRootString;
            } else {
                System.out.println("Avertissement : Le chemin DocumentRoot '" + docRootString + "' n'est pas un répertoire valide ou n'existe pas. Utilisation du répertoire par défaut : " + this.documentRoot);
            }
        } else {
            System.out.println("Avertissement : La balise <DocumentRoot> est introuvable ou vide. Utilisation du répertoire par défaut (répertoire courant d'exécution) : " + this.documentRoot);
        }

        // Directory Listing
        String directoryString = XmlValueExtracteur.getTagTextValue(CONFIG_FILE_PATH, "Directory");
        if (!directoryString.isEmpty()) {
            if (directoryString.equalsIgnoreCase("on") || directoryString.equalsIgnoreCase("off")) {
                this.directoryListing = directoryString.toLowerCase();
            } else {
                System.out.println("Avertissement : La valeur de la balise <Directory> '" + directoryString + "' n'est pas 'on' ou 'off'. Utilisation de la valeur par défaut : '" + this.directoryListing + "'.");
            }
        } else {
            System.out.println("Avertissement : La balise <Directory> est introuvable ou vide. Utilisation de la valeur par défaut : '" + this.directoryListing + "'.");
        }

        // Allowed IPs
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
                System.out.println("Avertissement : La balise <Allow> ne contient pas d'IPs valides. Revert aux IPs autorisées par défaut.");
                this.allowedIps.addAll(DEFAULT_ALLOWED_IPS);
            }
        } else {
            System.out.println("Avertissement : La balise <Allow> est introuvable ou vide. Utilisation des IPs autorisées par défaut : " + this.allowedIps);
        }

        // Denied IPs
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
    }

    // --- Getters pour accéder aux valeurs de configuration ---
    public int getPort() {
        return port;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public String getDirectoryListing() {
        return directoryListing;
    }

    public List<String> getAllowedIps() {
        return new ArrayList<>(allowedIps); // Retourne une copie pour éviter les modifications externes
    }

    public List<String> getDeniedIps() {
        return new ArrayList<>(deniedIps); // Retourne une copie pour éviter les modifications externes
    }
}
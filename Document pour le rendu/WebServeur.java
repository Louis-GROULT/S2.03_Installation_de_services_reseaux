import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets; // Importation pour UTF-8

public class WebServeur {

    // Déclarations statiques pour les configurations et les logs, accessibles dans les méthodes statiques
    private static String accessLogPath;
    private static String errorLogPath;
    private static String currentDocumentRoot;
    private static String currentDirectoryListing;
    private static List<String> currentAllowedIps;
    private static List<String> currentDeniedIps;

    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_HTML_FILE = "index.html";

    public static void main(String[] args) {
        // Créer une instance de WebServeurConfig pour charger et gérer la configuration
        WebServeurConfig config = new WebServeurConfig();

        // Récupérer les valeurs de configuration et les affecter aux variables statiques
        int currentPort = config.getPort();
        currentDocumentRoot = config.getDocumentRoot(); // Affectation pour l'accès statique
        currentDirectoryListing = config.getDirectoryListing(); // Affectation pour l'accès statique
        currentAllowedIps = config.getAllowedIps(); // Affectation pour l'accès statique
        currentDeniedIps = config.getDeniedIps(); // Affectation pour l'accès statique
        accessLogPath = config.getAccessLogPath();
        errorLogPath = config.getErrorLogPath();

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(currentPort);
            System.out.println("\nServeur HTTP démarré sur le port " + currentPort);
            System.out.println("Répertoire racine du site : " + currentDocumentRoot);
            System.out.println("Affichage des répertoires : " + currentDirectoryListing);
            System.out.println("IPs autorisées : " + (currentAllowedIps.isEmpty() ? "Toutes" : currentAllowedIps));
            System.out.println("IPs refusées : " + (currentDeniedIps.isEmpty() ? "Aucune" : currentDeniedIps));
            if (accessLogPath != null) {
                System.out.println("Chemin du log d'accès : " + accessLogPath);
            } else {
                System.out.println("Journal d'accès : Désactivé");
            }
            if (errorLogPath != null) {
                System.out.println("Chemin du log d'erreur : " + errorLogPath);
            } else {
                System.out.println("Journal d'erreur : Désactivé");
            }


            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();

                    if (!isIpAllowed(clientIp)) {
                        sendHttpResponse(clientSocket.getOutputStream(), "403 Forbidden", "text/plain", "Accès refusé : votre IP est bloquée.\n");
                        logAccess(clientIp, "N/A", "N/A", "403 Forbidden");
                        closeSocket(clientSocket);
                        continue;
                    }

                    handleClient(clientSocket);

                } catch (IOException e) {
                    logError("Erreur d'acceptation du client ou de traitement : " + e.getMessage());
                    System.out.println("Erreur d'acceptation du client ou de traitement : " + e.getMessage()); // System.out.println
                } finally {
                    closeSocket(clientSocket);
                }
            }
        } catch (IOException e) {
            logError("Impossible de démarrer le serveur sur le port " + currentPort + " : " + e.getMessage());
            System.out.println("Impossible de démarrer le serveur sur le port " + currentPort + " : " + e.getMessage()); // System.out.println
        } finally {
            closeSocket(serverSocket);
        }
    }

    private static boolean isIpAllowed(String clientIp) {
        if (currentDeniedIps != null && currentDeniedIps.contains(clientIp)) {
            return false; // Explicitement refusé
        }
        // Si la liste des IPs autorisées est vide, toutes les IPs sont implicitement autorisées.
        // Si elle n'est pas vide, l'IP du client doit être dans la liste.
        if (currentAllowedIps != null && !currentAllowedIps.isEmpty() && !currentAllowedIps.contains(clientIp)) {
            return false; // Si des IPs sont spécifiées dans 'Allow', et l'IP du client n'en fait pas partie
        }
        return true; // Autorisé par défaut ou explicitement autorisé
    }


    private static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = null;
        OutputStream out = null;
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        String requestLine = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)); // Lire en UTF-8
            out = clientSocket.getOutputStream();

            requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return; // Requête vide
            }

            System.out.println("Requête reçue : " + requestLine + " de " + clientIp);

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendHttpResponse(out, "400 Bad Request", "text/plain", "Requête invalide.\n");
                logAccess(clientIp, "N/A", "N/A", "400 Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            // Lire les en-têtes restants de la requête (utile pour POST plus tard)
            // C'est important de consommer tout le stream de la requête
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Pour le moment, nous ignorons les en-têtes, mais ils sont lus.
            }

            if (method.equalsIgnoreCase("GET")) {
                if ("/info.html".equalsIgnoreCase(path)) {
                    String infoHtml = SystemInfo.getSystemInfoHtml();
                    sendHttpResponse(out, "200 OK", "text/html; charset=UTF-8", infoHtml);
                    logAccess(clientIp, method, path, "200 OK");
                } else {
                    serveFile(out, clientIp, method, path);
                }
            } else if (method.equalsIgnoreCase("POST")) {
                // Pour la gestion des formulaires POST, vous devrez lire le corps de la requête.
                // Cela nécessitera de récupérer le Content-Length de la requête et de lire ce nombre d'octets.
                // Pour l'instant, juste une réponse simple.
                sendHttpResponse(out, "200 OK", "text/plain", "Requête POST reçue. Traitement des formulaires non implémenté pour le moment.\n");
                logAccess(clientIp, method, path, "200 OK");
            } else {
                sendHttpResponse(out, "405 Method Not Allowed", "text/plain", "Méthode non autorisée.\n");
                logAccess(clientIp, method, path, "405 Method Not Allowed");
            }
        } catch (IOException e) {
            logError("Erreur lors du traitement de la requête de " + clientIp + " : " + e.getMessage());
            System.out.println("Erreur lors du traitement de la requête de " + clientIp + " : " + e.getMessage()); // System.out.println
            // Tente d'envoyer une erreur 500 si possible
            try {
                if (out != null) {
                    sendHttpResponse(out, "500 Internal Server Error", "text/plain", "Erreur interne du serveur.\n");
                }
            } catch (IOException e2) {
                logError("Erreur lors de l'envoi de l'erreur 500 : " + e2.getMessage());
                System.out.println("Erreur lors de l'envoi de l'erreur 500 : " + e2.getMessage()); // System.out.println
            }
        } finally {
            closeStreams(in, out);
        }
    }

    private static void serveFile(OutputStream out, String clientIp, String method, String path) throws IOException {
        // Définition de la variable pour l'emplacement du fichier HTML par défaut
        // Cette variable n'est utilisée que si le chemin est la racine "/"

        if ("/".equals(path)) {
            path = "/" + DEFAULT_HTML_FILE; // Serve index.html by default
        }

        // Construire le chemin complet du fichier demandé
        Path requestedPath = Paths.get(currentDocumentRoot, path).normalize();
        File file = requestedPath.toFile();

        // Vérifier si le fichier est à l'intérieur du DocumentRoot (sécurité: éviter le "directory traversal")
        // Utilisation de getCanonicalPath pour résoudre les ../ et autres.
        // Cette vérification est cruciale.
        try {
            if (!file.getCanonicalPath().startsWith(new File(currentDocumentRoot).getCanonicalPath())) {
                sendHttpResponse(out, "403 Forbidden", "text/plain", "Accès refusé : Tentative d'accès en dehors du répertoire racine.\n");
                logAccess(clientIp, method, path, "403 Forbidden");
                return;
            }
        } catch (IOException e) {
            logError("Erreur de sécurité (canonical path) pour le chemin " + path + " : " + e.getMessage());
            System.out.println("Erreur de sécurité (canonical path) pour le chemin " + path + " : " + e.getMessage()); // System.out.println
            sendHttpResponse(out, "500 Internal Server Error", "text/plain", "Erreur interne du serveur lors de la vérification du chemin.\n");
            logAccess(clientIp, method, path, "500 Internal Server Error");
            return;
        }


        if (file.isDirectory()) {
            if ("on".equalsIgnoreCase(currentDirectoryListing)) {
                sendDirectoryListing(out, file, clientIp, method, path);
            } else {
                sendHttpResponse(out, "403 Forbidden", "text/plain", "L'affichage des répertoires est désactivé.\n");
                logAccess(clientIp, method, path, "403 Forbidden");
            }
        } else if (file.exists() && file.isFile()) {
            // Fichier trouvé, le servir
            String contentType = getContentType(file.getName());
            try {
                // Utiliser Files.readAllBytes pour lire le fichier en octets (important pour Content-Length)
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String responseHeader = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "; charset=UTF-8\r\n" + // Toujours spécifier l'encodage
                        "Content-Length: " + fileContent.length + "\r\n" + // Taille en octets du contenu
                        "Connection: close\r\n\r\n"; // Ferme la connexion après chaque requête

                out.write(responseHeader.getBytes(StandardCharsets.UTF_8)); // En-têtes aussi en UTF-8
                out.write(fileContent); // Écrire les octets du fichier
                out.flush(); // S'assurer que tout est envoyé
                logAccess(clientIp, method, path, "200 OK");
            } catch (IOException e) {
                logError("Erreur de lecture du fichier " + file.getAbsolutePath() + " : " + e.getMessage());
                System.out.println("Erreur de lecture du fichier " + file.getAbsolutePath() + " : " + e.getMessage()); // System.out.println
                sendHttpResponse(out, "500 Internal Server Error", "text/plain", "Erreur interne du serveur lors de la lecture du fichier.\n");
                logAccess(clientIp, method, path, "500 Internal Server Error");
            }
        } else {
            // Fichier non trouvé
            sendHttpResponse(out, "404 Not Found", "text/plain", "Le fichier demandé n'a pas été trouvé.\n");
            logAccess(clientIp, method, path, "404 Not Found");
        }
    }

    private static void sendHttpResponse(OutputStream out, String status, String contentType, String body) throws IOException {
        // Le Content-Length doit être la taille en octets du corps, pas le nombre de caractères.
        // Utiliser StandardCharsets.UTF_8 pour obtenir les octets du corps.
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String responseHeader = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n\r\n"; // Ferme la connexion après chaque réponse

        out.write(responseHeader.getBytes(StandardCharsets.UTF_8)); // En-têtes en UTF-8
        out.write(bodyBytes); // Corps en UTF-8
        out.flush();
    }

    private static void sendDirectoryListing(OutputStream out, File directory, String clientIp, String method, String requestedPath) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html><head><title>Index of ").append(requestedPath).append("</title>");
        htmlContent.append("<meta charset=\"UTF-8\">"); // Ajout de l'encodage
        htmlContent.append("<style>");
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }");
        htmlContent.append("h1 { color: #0056b3; border-bottom: 2px solid #0056b3; padding-bottom: 10px; }");
        htmlContent.append("ul { list-style-type: none; padding: 0; }");
        htmlContent.append("li { margin-bottom: 5px; }");
        htmlContent.append("a { color: #007bff; text-decoration: none; }");
        htmlContent.append("a:hover { text-decoration: underline; }");
        htmlContent.append("</style></head><body>");
        htmlContent.append("<h1>Index of ").append(requestedPath).append("</h1><ul>");

        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                // Les répertoires apparaissent avant les fichiers
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                // Puis tri alphabétique insensible à la casse
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            // Lien vers le répertoire parent (si ce n'est pas le DocumentRoot lui-même)
            // Correction pour le chemin parent : s'assurer qu'il est correct et relatif
            try {
                String canonicalDirectoryPath = directory.getCanonicalPath();
                String canonicalDocumentRootPath = new File(currentDocumentRoot).getCanonicalPath();

                if (!canonicalDirectoryPath.equals(canonicalDocumentRootPath)) {
                    // Si on n'est pas à la racine du DocumentRoot
                    Path currentPath = Paths.get(requestedPath);
                    Path parentRelativePath = currentPath.getParent();

                    String parentLink = (parentRelativePath != null ? parentRelativePath.toString() : "/") + "/";

                    // Assurez-vous que l'URL encodée des chemins est correcte, en gardant les slashes
                    String encodedParentLink = URLEncoder.encode(parentLink, StandardCharsets.UTF_8.toString())
                            .replace("%2F", "/") // Garde les slashes non encodés
                            .replace("+", "%20"); // Remplace les espaces par %20

                    htmlContent.append("<li><a href=\"").append(encodedParentLink).append("\">.. (Parent Directory)</a></li>");
                }
            } catch (IOException e) {
                logError("Erreur lors de la détermination du chemin parent pour le listing de répertoire : " + e.getMessage());
                System.out.println("Erreur lors de la détermination du chemin parent pour le listing de répertoire : " + e.getMessage()); // System.out.println
                // Ne pas bloquer l'affichage, mais ajouter un message d'erreur dans les logs
            }


            for (File item : files) {
                String itemName = item.getName();
                // Construire le chemin pour le lien, en s'assurant qu'il est relatif au serveur HTTP
                String itemLinkPath = requestedPath;
                if (!itemLinkPath.endsWith("/")) {
                    itemLinkPath += "/";
                }
                itemLinkPath += itemName;

                if (item.isDirectory()) {
                    itemName += "/"; // Ajoute un slash pour les répertoires
                }
                // Encoder l'URL pour les noms de fichiers contenant des caractères spéciaux
                String encodedItemPath = URLEncoder.encode(itemLinkPath, StandardCharsets.UTF_8.toString())
                        .replace("%2F", "/") // Garde les slashes non encodés pour les chemins
                        .replace("+", "%20"); // Remplace les espaces par %20 pour lisibilité

                htmlContent.append("<li><a href=\"").append(encodedItemPath).append("\">").append(itemName).append("</a></li>");
            }
        } else {
            htmlContent.append("<li>Impossible de lister le contenu du répertoire ou répertoire vide.</li>");
        }
        htmlContent.append("</ul></body></html>");

        sendHttpResponse(out, "200 OK", "text/html; charset=UTF-8", htmlContent.toString());
        logAccess(clientIp, method, requestedPath, "200 OK");
    }

    private static String getContentType(String fileName) {
        // Détection de Content-Type basée sur l'extension. Liste extensible.
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerFileName.endsWith(".css")) {
            return "text/css";
        } else if (lowerFileName.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerFileName.endsWith(".json")) {
            return "application/json";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFileName.endsWith(".ico")) {
            return "image/x-icon";
        }
        // Formats audio
        else if (lowerFileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerFileName.endsWith(".wav")) {
            return "audio/wav";
        } else if (lowerFileName.endsWith(".ogg") || lowerFileName.endsWith(".oga")) {
            return "audio/ogg";
        } else if (lowerFileName.endsWith(".m4a")) {
            return "audio/mp4";
        } else if (lowerFileName.endsWith(".flac")) {
            return "audio/flac";
        }
        // Formats vidéo
        else if (lowerFileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerFileName.endsWith(".webm")) {
            return "video/webm";
        } else if (lowerFileName.endsWith(".ogv")) {
            return "video/ogg";
        }
        // Fichiers texte génériques
        else if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        }
        // Par défaut, octet stream si le type MIME est inconnu
        return "application/octet-stream";
    }

    /**
     * Enregistre les accès au serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] [IP Client] [Méthode] [Chemin] [Statut HTTP]
     */
    private static void logAccess(String clientIp, String method, String path, String status) {
        if (accessLogPath == null) {
            return; // Le logging d'accès est désactivé si accessLogPath est null
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] %s %s %s %s%n",
                    timestamp,
                    clientIp != null ? clientIp : "UNKNOWN_IP",
                    method != null ? method : "UNKNOWN_METHOD",
                    path != null ? path : "UNKNOWN_PATH",
                    status != null ? status : "UNKNOWN_STATUS");
            // Utilisation de StandardCharsets.UTF_8 pour les logs
            Files.write(Paths.get(accessLogPath), logEntry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier de log d'accès " + accessLogPath + " : " + e.getMessage());
            // On ne log pas dans errorLog ici pour éviter une boucle infinie si errorLog a aussi un problème
        }
    }

    /**
     * Enregistre les erreurs du serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] ERROR: [Message d'erreur]
     */
    private static void logError(String errorMessage) {
        if (errorLogPath == null) {
            return; // Le logging d'erreur est désactivé si errorLogPath est null
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] ERROR: %s%n",
                    timestamp,
                    errorMessage != null ? errorMessage : "NO_ERROR_MESSAGE");
            // Utilisation de StandardCharsets.UTF_8 pour les logs
            Files.write(Paths.get(errorLogPath), logEntry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier de log d'erreur " + errorLogPath + " : " + e.getMessage());
        }
    }

    // Méthodes utilitaires pour fermer les sockets et les streams en toute sécurité
    private static void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket client : " + e.getMessage()); // System.out.println
                logError("Erreur à la fermeture du socket client : " + e.getMessage());
            }
        }
    }

    private static void closeSocket(ServerSocket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du ServerSocket : " + e.getMessage()); // System.out.println
                logError("Erreur à la fermeture du ServerSocket : " + e.getMessage());
            }
        }
    }

    private static void closeStreams(Closeable... streams) {
        for (Closeable stream : streams) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    System.out.println("Erreur à la fermeture du flux : " + e.getMessage()); // System.out.println
                    logError("Erreur à la fermeture du flux : " + e.getMessage());
                }
            }
        }
    }
}
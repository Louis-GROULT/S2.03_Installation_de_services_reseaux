import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WebServeur {

    private static String accessLogPath;
    private static String errorLogPath;
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // Créer une instance de WebServeurConfig pour charger et gérer la configuration
        WebServeurConfig config = new WebServeurConfig();

        // Récupérer les valeurs de configuration
        int currentPort = config.getPort();
        String currentDocumentRoot = config.getDocumentRoot();
        String currentDirectoryListing = config.getDirectoryListing();
        List<String> currentAllowedIps = config.getAllowedIps();
        List<String> currentDeniedIps = config.getDeniedIps();
        accessLogPath = config.getAccessLogPath(); // Récupérer le chemin du log d'accès
        errorLogPath = config.getErrorLogPath();   // Récupérer le chemin du log d'erreur

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(currentPort);
            System.out.println("\nServeur HTTP démarré sur le port " + currentPort);
            System.out.println("Répertoire racine du site : " + currentDocumentRoot);
            System.out.println("Affichage des répertoires : " + currentDirectoryListing);
            System.out.println("IPs autorisées : " + currentAllowedIps);
            System.out.println("IPs refusées : " + currentDeniedIps);
            if (config.getActiveConfigFilePath() != null) {
                System.out.println("Configuration chargée depuis : " + config.getActiveConfigFilePath());
            } else {
                System.out.println("Aucun fichier de configuration externe trouvé, utilisant les valeurs par défaut.");
            }
            if (accessLogPath != null) {
                System.out.println("Journal d'accès configuré : " + accessLogPath);
            } else {
                System.out.println("Journal d'accès désactivé.");
            }
            if (errorLogPath != null) {
                System.out.println("Journal d'erreur configuré : " + errorLogPath);
            } else {
                System.out.println("Journal d'erreur désactivé.");
            }


            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    handleClient(clientSocket, currentDocumentRoot, currentDirectoryListing,
                            currentAllowedIps, currentDeniedIps);
                } catch (IOException e) {
                    System.out.println("Erreur lors du traitement du client : " + e.getMessage());
                    logError("IOException during client handling: " + e.getMessage());
                } finally {
                    closeSocket(clientSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur au démarrage du serveur : " + e.getMessage());
            logError("IOException during server startup: " + e.getMessage());
        } finally {
            closeSocket(serverSocket);
        }
    }

    private static void handleClient(Socket socket, String documentRoot, String directoryListingSetting, List<String> allowedIps, List<String> deniedIps) {
        BufferedReader in = null;
        OutputStream out = null;
        String clientIp = null;
        String requestLine = null;

        try {
            clientIp = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();

            // Vérification des IPs refusées en premier
            if (deniedIps.contains(clientIp)) {
                System.out.println("Connexion refusée pour IP : " + clientIp + " (bloquée par liste de refus)");
                sendErrorResponse(out, "403 Forbidden", "Forbidden", "Your IP is blocked by the server configuration.");
                logAccess(clientIp, null, "403 Forbidden", "Blocked by Deny list");
                return;
            }
            // Vérification des IPs autorisées (si la liste n'est pas vide)
            // Si allowedIps est vide, toutes les IPs non denied sont implicitement autorisées.
            if (!allowedIps.isEmpty() && !allowedIps.contains(clientIp)) {
                System.out.println("Connexion refusée pour IP : " + clientIp + " (non autorisée par liste d'autorisation)");
                sendErrorResponse(out, "403 Forbidden", "Forbidden", "You are not allowed to access this server from your IP address.");
                logAccess(clientIp, null, "403 Forbidden", "Not allowed by Allow list");
                return;
            }

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Requête reçue de " + clientIp + " : " + requestLine);

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "UNKNOWN";
            String path = parts.length >= 2 ? parts[1] : "/";

            // Normalisation du chemin : si c'est la racine, on cherche index.html par défaut
            if (path.equals("/")) path = "/index.html";

            File file = new File(documentRoot + path);
            String statusCode = "200 OK";
            String logMessage = "Request successful";


            if (file.isDirectory()) {
                if (directoryListingSetting.equalsIgnoreCase("on")) {
                    System.out.println("Tentative d'accès au répertoire : " + file.getPath() + " (affichage activé)");
                    sendHtmlListing(out, file, documentRoot);
                    statusCode = "200 OK";
                    logMessage = "Directory listing served";
                } else {
                    System.out.println("Accès au répertoire refusé : " + file.getPath() + " (affichage désactivé)");
                    sendErrorResponse(out, "403 Forbidden", "Forbidden", "Directory listing is disabled for this server.");
                    statusCode = "403 Forbidden";
                    logMessage = "Directory listing denied";
                }
                logAccess(clientIp, requestLine, statusCode, logMessage);
                return;
            }

            if (file.exists() && !file.isDirectory()) {
                byte[] content = Files.readAllBytes(file.toPath());
                String contentType = getMimeType(file.getName());

                String responseHeader = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n\r\n";

                out.write(responseHeader.getBytes());
                out.write(content);
                statusCode = "200 OK";
                logMessage = "File served";
            } else {
                System.out.println("Fichier non trouvé : " + file.getPath());
                sendErrorResponse(out, "404 Not Found", "Not Found", "The requested file was not found.");
                statusCode = "404 Not Found";
                logMessage = "File not found";
            }

            out.flush();
            logAccess(clientIp, requestLine, statusCode, logMessage);

        } catch (IOException e) {
            System.out.println("Erreur pendant le traitement de la requête : " + e.getMessage());
            logError("IOException processing request from " + (clientIp != null ? clientIp : "UNKNOWN_IP") + " for '" + (requestLine != null ? requestLine : "NO_REQUEST_LINE") + "': " + e.getMessage());
            // Attempt to send 500 error if headers haven't been sent yet
            if (out != null) {
                try {
                    sendErrorResponse(out, "500 Internal Server Error", "Internal Server Error", "An unexpected server error occurred.");
                } catch (IOException ioException) {
                    System.out.println("Erreur lors de l'envoi de la réponse d'erreur 500 : " + ioException.getMessage());
                }
            }
            logAccess(clientIp, requestLine, "500 Internal Server Error", "Server error processing request");
        } finally {
            closeStreams(in, out);
            closeSocket(socket);
        }
    }

    private static String getMimeType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filePath.endsWith(".gif")) {
            return "image/gif";
        } else if (filePath.endsWith(".ico")) {
            return "image/x-icon";
        } else if (filePath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filePath.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    private static void sendErrorResponse(OutputStream out, String statusCode, String statusText, String message) throws IOException {
        String errorMessage = "<html><body><h1>" + statusText + "</h1><p>" + message + "</p></body></html>";
        String responseHeader = "HTTP/1.1 " + statusCode + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + errorMessage.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        // Vérifier si out n'est pas null avant d'écrire
        if (out != null) {
            out.write(responseHeader.getBytes());
            out.write(errorMessage.getBytes());
            out.flush();
        }
    }

    private static void sendHtmlListing(OutputStream out, File directory, String documentRoot) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body><h1>Listing of ").append(directory.getName()).append("</h1><ul>");

        Path docRootPath = Paths.get(documentRoot).toAbsolutePath();
        Path dirPath = directory.toPath().toAbsolutePath();

        String relativePathToDirectory = "";
        try {
            relativePathToDirectory = docRootPath.relativize(dirPath).toString().replace("\\", "/");
            if (!relativePathToDirectory.isEmpty() && !relativePathToDirectory.endsWith("/")) {
                relativePathToDirectory += "/";
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Avertissement: DocumentRoot '" + documentRoot + "' n'est pas un ancêtre de '" + directory.getAbsolutePath() + "'. Les liens de répertoire peuvent être incorrects.");
            relativePathToDirectory = directory.getName() + "/"; // Fallback pour le lien si DocumentRoot n'est pas parent
        }

        // Ajouter un lien pour le répertoire parent, sauf si c'est déjà le DocumentRoot
        if (!dirPath.equals(docRootPath)) {
            Path parentPathRelativeToDocRoot = docRootPath.relativize(dirPath.getParent()).normalize();
            htmlContent.append("<li><a href=\"/").append(URLEncoder.encode(parentPathRelativeToDocRoot.toString(), "UTF-8").replace("+", "%20"));
            // S'assurer que le lien du parent se termine par '/' s'il représente un répertoire non racine
            if (!parentPathRelativeToDocRoot.toString().isEmpty() && !parentPathRelativeToDocRoot.toString().endsWith("/")) {
                htmlContent.append("/");
            }
            htmlContent.append("\">../ (Parent Directory)</a></li>");
        }


        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1; // Directories first
                if (!f1.isDirectory() && f2.isDirectory()) return 1;  // Files after
                return f1.getName().compareToIgnoreCase(f2.getName()); // Then by name
            });

            for (File file : files) {
                String name = file.getName();
                String encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20"); // Encoder les espaces

                htmlContent.append("<li><a href=\"/");
                if (!relativePathToDirectory.isEmpty()) {
                    htmlContent.append(relativePathToDirectory);
                }
                htmlContent.append(encodedName);
                if (file.isDirectory()) {
                    htmlContent.append("/"); // Ajouter un slash pour les répertoires
                }
                htmlContent.append("\">").append(name);
                if (file.isDirectory()) {
                    htmlContent.append("/"); // Afficher un slash après le nom pour les répertoires
                }
                htmlContent.append("</a></li>");
            }
        }
        htmlContent.append("</ul></body></html>");

        String response = htmlContent.toString();
        String responseHeader = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + response.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(responseHeader.getBytes());
        out.write(response.getBytes());
        out.flush();
    }

    private static void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket : " + e.getMessage());
            }
        }
    }

    private static void closeSocket(ServerSocket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket : " + e.getMessage());
            }
        }
    }

    private static void closeStreams(Closeable... streams) {
        for (Closeable stream : streams) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    System.out.println("Erreur lors de la fermeture du stream : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Enregistre les accès au serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] [IP Client] [Requête] [Statut HTTP] [Message]
     */
    private static void logAccess(String clientIp, String requestLine, String httpStatus, String message) {
        if (accessLogPath == null) {
            return; // Le logging d'accès est désactivé si accessLogPath est null
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] %s \"%s\" %s \"%s\"%n",
                    timestamp,
                    clientIp != null ? clientIp : "UNKNOWN_IP",
                    requestLine != null ? requestLine.replace("\n", "\\n").replace("\r", "\\r") : "-", // Remplacer les retours à la ligne dans la requête
                    httpStatus != null ? httpStatus : "UNKNOWN_STATUS",
                    message != null ? message : "-");
            Files.write(Paths.get(accessLogPath), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Changement ici : System.out.println() au lieu de System.err.println()
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
            Files.write(Paths.get(errorLogPath), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Changement ici : System.out.println() au lieu de System.err.println()
            System.out.println("Erreur lors de l'écriture dans le fichier de log d'erreur " + errorLogPath + " : " + e.getMessage());
        }
    }
}
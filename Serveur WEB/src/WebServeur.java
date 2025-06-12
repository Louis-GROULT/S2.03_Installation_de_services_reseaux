// WebServeur.java

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

    private final WebServeurConfig config; // Instance de la configuration
    private final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WebServeur(WebServeurConfig config) {
        this.config = config;
    }

    public void start() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(config.getPort());
            System.out.println("\nServeur HTTP démarré sur le port " + config.getPort());
            System.out.println("Répertoire racine du site : " + config.getDocumentRoot());
            System.out.println("Affichage des répertoires : " + config.getDirectoryListing());
            System.out.println("IPs autorisées : " + config.getAllowedIps());
            System.out.println("IPs refusées : " + config.getDeniedIps());
            System.out.println("Chemin des logs d'accès : " + (config.getAccessLogPath() != null ? config.getAccessLogPath() : "Désactivé"));
            System.out.println("Chemin des logs d'erreur : " + (config.getErrorLogPath() != null ? config.getErrorLogPath() : "Désactivé"));


            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.out.println("Erreur lors du traitement du client : " + e.getMessage());
                    logError("Erreur lors du traitement du client : " + e.getMessage());
                } finally {
                    closeSocket(clientSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur au démarrage du serveur : " + e.getMessage());
            logError("Erreur au démarrage du serveur : " + e.getMessage());
        } finally {
            closeSocket(serverSocket);
        }
    }

    private void handleClient(Socket socket) { // Non-static
        BufferedReader in = null;
        OutputStream out = null;
        String clientIpAddress = socket.getInetAddress().getHostAddress();
        String requestedPath = ""; // Pour logging

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return; // Requête vide
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(out, 400, "Bad Request", "Requête mal formée.");
                logAccess(clientIpAddress, requestedPath, "400 Bad Request");
                logError("Requête mal formée de l'IP " + clientIpAddress + ": " + requestLine);
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            requestedPath = path; // Stocker pour le logging

            System.out.println("Requête reçue de " + clientIpAddress + " : " + method + " " + path);

            // Vérifier les IPs autorisées/refusées avant de traiter la requête
            if (!isIpAllowed(clientIpAddress)) {
                sendErrorResponse(out, 403, "Forbidden", "Accès refusé pour cette adresse IP.");
                logAccess(clientIpAddress, requestedPath, "403 Forbidden");
                logError("Accès refusé pour l'IP : " + clientIpAddress + " pour le chemin " + requestedPath);
                return;
            }


            // Gérer la requête pour /status
            if ("/status".equals(path)) {
                System.out.println("Requête pour les infos système...");
                sendSystemInfoPage(out);
                logAccess(clientIpAddress, requestedPath, "200 OK");
                return;
            }

            // Decode URL-encoded characters (e.g., %20 for space)
            String decodedPath = URLDecoder.decode(path, "UTF-8");

            // Build the full file path, handling potential directory traversal attempts
            Path filePath = Paths.get(config.getDocumentRoot(), decodedPath).normalize();

            // Ensure the path does not go outside the document root
            if (!filePath.startsWith(config.getDocumentRoot())) {
                sendErrorResponse(out, 403, "Forbidden", "Accès interdit : tentative de traversée de répertoire.");
                logAccess(clientIpAddress, requestedPath, "403 Forbidden");
                logError("Tentative de traversée de répertoire : " + decodedPath + " depuis " + clientIpAddress);
                return;
            }

            File file = filePath.toFile();

            if (file.isDirectory()) {
                if ("on".equalsIgnoreCase(config.getDirectoryListing())) {
                    // Try to serve index.html if it exists in the directory
                    File indexFile = new File(file, "index.html");
                    if (indexFile.exists() && indexFile.isFile() && indexFile.canRead()) {
                        sendFile(out, indexFile, "text/html");
                        logAccess(clientIpAddress, requestedPath, "200 OK");
                    } else {
                        // If no index.html, list directory contents
                        sendDirectoryListing(out, file, path);
                        logAccess(clientIpAddress, requestedPath, "200 OK");
                    }
                } else {
                    sendErrorResponse(out, 403, "Forbidden", "L'affichage des répertoires est désactivé.");
                    logAccess(clientIpAddress, requestedPath, "403 Forbidden");
                    logError("Affichage de répertoire désactivé pour " + decodedPath + " depuis " + clientIpAddress);
                }
            } else if (file.exists() && file.isFile() && file.canRead()) {
                String contentType = getContentType(filePath.toString());
                sendFile(out, file, contentType);
                logAccess(clientIpAddress, requestedPath, "200 OK");
            } else {
                sendErrorResponse(out, 404, "Not Found", "Le fichier demandé n'existe pas.");
                logAccess(clientIpAddress, requestedPath, "404 Not Found");
                logError("Fichier non trouvé : " + decodedPath + " depuis " + clientIpAddress);
            }

            out.flush();

        } catch (IOException e) {
            System.out.println("Erreur pendant le traitement de la requête : " + e.getMessage());
            logError("Erreur IO pendant le traitement de la requête " + requestedPath + " de " + clientIpAddress + " : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur inattendue : " + e.getMessage());
            logError("Erreur inattendue pendant le traitement de la requête " + requestedPath + " de " + clientIpAddress + " : " + e.getMessage());
            try {
                sendErrorResponse(out, 500, "Internal Server Error", "Une erreur interne du serveur s'est produite.");
                logAccess(clientIpAddress, requestedPath, "500 Internal Server Error");
            } catch (IOException ioException) {
                System.out.println("Erreur lors de l'envoi de la réponse d'erreur 500 : " + ioException.getMessage());
            }
        } finally {
            closeStreams(in, out);
            closeSocket(socket);
        }
    }

    // Moved from static to instance method
    private boolean isIpAllowed(String ipAddress) {
        // Si la liste des IPs autorisées est vide, toutes les IPs sont autorisées (cas par défaut)
        if (config.getAllowedIps().isEmpty()) {
            return true;
        }
        // Si l'IP est dans la liste des IPs refusées, elle est refusée
        if (config.getDeniedIps().contains(ipAddress)) {
            return false;
        }
        // Si l'IP est dans la liste des IPs autorisées, elle est autorisée
        if (config.getAllowedIps().contains(ipAddress)) {
            return true;
        }
        // Si la liste des IPs autorisées n'est pas vide et l'IP n'est pas dedans, elle est refusée
        return false;
    }


    private void sendFile(OutputStream out, File file, String contentType) throws IOException {
        long fileLength = file.length();
        String responseHeader = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileLength + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(responseHeader.getBytes());

        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void sendDirectoryListing(OutputStream out, File directory, String requestPath) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html><head><title>Index of ").append(requestPath).append("</title>");
        htmlContent.append("<style>");
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }");
        htmlContent.append("h1 { color: #0056b3; border-bottom: 2px solid #0056b3; padding-bottom: 10px; }");
        htmlContent.append("ul { list-style-type: none; padding: 0; }");
        htmlContent.append("li { margin-bottom: 5px; }");
        htmlContent.append("a { color: #0056b3; text-decoration: none; }");
        htmlContent.append("a:hover { text-decoration: underline; }");
        htmlContent.append("</style>");
        htmlContent.append("</head><body>\n");
        htmlContent.append("<h1>Index of ").append(requestPath).append("</h1>\n");
        htmlContent.append("<ul>\n");

        // Add parent directory link if not at root
        if (!"/".equals(requestPath) && !requestPath.isEmpty()) {
            Path currentPath = Paths.get(requestPath);
            Path parentPath = currentPath.getParent();
            if (parentPath != null) {
                htmlContent.append("<li><a href=\"").append(URLEncoder.encode(parentPath.toString(), "UTF-8")).append("\">.. (Parent Directory)</a></li>\n");
            } else {
                // If currentPath is just a filename at root, this handles ".."
                htmlContent.append("<li><a href=\"/\">.. (Parent Directory)</a></li>\n");
            }
        }

        File[] files = directory.listFiles();
        if (files != null) {
            // Sort files and directories alphabetically, directories first
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                }
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            for (File item : files) {
                String itemName = item.getName();
                String itemPath = requestPath.endsWith("/") ? requestPath + itemName : requestPath + "/" + itemName;
                if (item.isDirectory()) {
                    htmlContent.append("<li><a href=\"").append(URLEncoder.encode(itemPath, "UTF-8")).append("/\">").append(itemName).append("/</a></li>\n");
                } else {
                    htmlContent.append("<li><a href=\"").append(URLEncoder.encode(itemPath, "UTF-8")).append("\">").append(itemName).append("</a></li>\n");
                }
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
    }

    private void sendErrorResponse(OutputStream out, int statusCode, String statusMessage, String message) throws IOException {
        String htmlError = "<!DOCTYPE html><html><head><title>Erreur " + statusCode + "</title></head><body><h1>" + statusCode + " " + statusMessage + "</h1><p>" + message + "</p></body></html>";
        String responseHeader = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + htmlError.length() + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(responseHeader.getBytes());
        out.write(htmlError.getBytes());
        out.flush();
    }

    private void sendSystemInfoPage(OutputStream out) throws IOException {
        String systemInfoHtml = SystemInfo.getSystemInfoHtml();
        String responseHeader = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + systemInfoHtml.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(responseHeader.getBytes());
        out.write(systemInfoHtml.getBytes());
        out.flush();
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".ico")) {
            return "image/x-icon";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        // Default content type if unknown
        return "application/octet-stream";
    }

    /**
     * Enregistre les accès au serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] [IP Client] [Méthode] [Chemin] [Statut]
     */
    private void logAccess(String clientIp, String requestedPath, String status) {
        if (config.getAccessLogPath() == null) {
            return; // Le logging est désactivé
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] %s \"%s\" %s%n",
                    timestamp,
                    clientIp,
                    requestedPath,
                    status);
            Files.write(Paths.get(config.getAccessLogPath()), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier de log d'accès " + config.getAccessLogPath() + " : " + e.getMessage());
        }
    }

    /**
     * Enregistre les erreurs du serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] ERROR: [Message d'erreur]
     */
    private void logError(String errorMessage) {
        if (config.getErrorLogPath() == null) {
            return; // Le logging d'erreur est désactivé si errorLogPath est null
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] ERROR: %s%n",
                    timestamp,
                    errorMessage != null ? errorMessage : "NO_ERROR_MESSAGE");
            Files.write(Paths.get(config.getErrorLogPath()), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier de log d'erreur " + config.getErrorLogPath() + " : " + e.getMessage());
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket client : " + e.getMessage());
            }
        }
    }

    private void closeSocket(ServerSocket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du ServerSocket : " + e.getMessage());
            }
        }
    }

    private void closeStreams(Closeable... streams) {
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

    public static void main(String[] args) {
        // Charger la configuration une seule fois au début
        WebServeurConfig config = new WebServeurConfig();

        // Créer une instance du serveur web avec la configuration
        WebServeur server = new WebServeur(config);

        // Démarrer le serveur
        server.start();
    }
}
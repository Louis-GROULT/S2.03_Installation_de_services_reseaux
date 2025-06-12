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

    private static String accessLogPath;
    private static String errorLogPath;
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // Créer une instance de WebServeurConfig pour charger et gérer la configuration
        WebServeurConfig config = new WebServeurConfig(); // CHANGEMENT ICI

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


            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    // Vérification de l'adresse IP du client
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    if (isDenied(clientIp, currentDeniedIps)) {
                        System.out.println("Accès refusé pour l'IP : " + clientIp);
                        logError("Accès refusé pour l'IP : " + clientIp);
                        sendErrorResponse(clientSocket, 403, "Forbidden");
                        continue; // Passer au client suivant
                    }
                    if (!isAllowed(clientIp, currentAllowedIps)) {
                        System.out.println("Accès non autorisé pour l'IP : " + clientIp);
                        logError("Accès non autorisé pour l'IP : " + clientIp);
                        sendErrorResponse(clientSocket, 401, "Unauthorized");
                        continue; // Passer au client suivant
                    }

                    handleClient(clientSocket, currentDocumentRoot, currentDirectoryListing);
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

    private static void handleClient(Socket socket, String documentRoot, String directoryListing) {
        BufferedReader in = null;
        OutputStream out = null;
        String clientIp = socket.getInetAddress().getHostAddress();
        String requestedResource = "UNKNOWN"; // Initialisation pour le log

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                logAccess(clientIp, requestedResource, "400 Bad Request");
                sendErrorResponse(socket, 400, "Bad Request");
                return;
            }

            // GET /chemin/vers/fichier.html HTTP/1.1
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2 || !requestParts[0].equals("GET")) {
                logAccess(clientIp, requestedResource, "400 Bad Request");
                sendErrorResponse(socket, 400, "Bad Request");
                return;
            }

            String filePath = requestParts[1];
            requestedResource = filePath; // Pour le log
            // Supprimer le premier slash pour obtenir un chemin relatif au DocumentRoot
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }

            File requestedFile = new File(documentRoot, filePath);

            if (requestedFile.isDirectory()) {
                if ("on".equalsIgnoreCase(directoryListing)) {
                    sendDirectoryListing(out, requestedFile, documentRoot);
                    logAccess(clientIp, requestedResource, "200 OK (Directory Listing)");
                } else {
                    // Si le listing est désactivé, chercher index.html ou renvoyer 403
                    File indexFile = new File(requestedFile, "index.html");
                    if (indexFile.exists() && indexFile.isFile()) {
                        sendFile(out, indexFile, "text/html");
                        logAccess(clientIp, requestedResource, "200 OK (index.html)");
                    } else {
                        logAccess(clientIp, requestedResource, "403 Forbidden (Directory listing off)");
                        sendErrorResponse(socket, 403, "Forbidden");
                    }
                }
            } else if (requestedFile.exists() && requestedFile.isFile()) {
                String contentType = getContentType(requestedFile.getName());
                sendFile(out, requestedFile, contentType);
                logAccess(clientIp, requestedResource, "200 OK");
            } else {
                logAccess(clientIp, requestedResource, "404 Not Found");
                sendErrorResponse(socket, 404, "Not Found");
            }

            out.flush();
        } catch (IOException e) {
            System.out.println("Erreur pendant le traitement de la requête : " + e.getMessage());
            logError("Erreur pendant le traitement de la requête pour " + clientIp + " - " + requestedResource + " : " + e.getMessage());
        } finally {
            closeStreams(in, out);
            closeSocket(socket);
        }
    }

    /**
     * Vérifie si une adresse IP est dans la liste des IPs refusées.
     * @param ip L'adresse IP à vérifier.
     * @param deniedIps La liste des adresses IP refusées.
     * @return true si l'IP est refusée, false sinon.
     */
    private static boolean isDenied(String ip, List<String> deniedIps) {
        return deniedIps.contains(ip);
    }

    /**
     * Vérifie si une adresse IP est dans la liste des IPs autorisées.
     * Si la liste des IPs autorisées est vide, toutes les IPs sont considérées comme autorisées.
     * @param ip L'adresse IP à vérifier.
     * @param allowedIps La liste des adresses IP autorisées.
     * @return true si l'IP est autorisée, false sinon.
     */
    private static boolean isAllowed(String ip, List<String> allowedIps) {
        if (allowedIps.isEmpty()) {
            return true; // Si aucune IP n'est spécifiée dans <Allow>, toutes sont autorisées.
        }
        return allowedIps.contains(ip);
    }

    private static void sendFile(OutputStream out, File file, String contentType) throws IOException {
        long fileSize = file.length();
        String responseHeader = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileSize + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(responseHeader.getBytes());

        Files.copy(file.toPath(), out);
    }

    private static void sendDirectoryListing(OutputStream out, File directory, String documentRoot) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html><head><title>Index of ");
        htmlContent.append(URLEncoder.encode(directory.getName(), "UTF-8").replace("+", "%20"));
        htmlContent.append("</title></head><body><h1>Index of ");
        htmlContent.append(URLEncoder.encode(directory.getName(), "UTF-8").replace("+", "%20"));
        htmlContent.append("</h1><ul>\n");

        // Lien pour revenir au répertoire parent
        if (!directory.equals(new File(documentRoot))) {
            String parentPath = Paths.get(documentRoot).relativize(directory.toPath().getParent()).toString();
            htmlContent.append("<li><a href=\"/").append(URLEncoder.encode(parentPath, "UTF-8").replace("+", "%20")).append("/\">..</a></li>\n");
        }

        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            for (File file : files) {
                String name = file.getName();
                String relativePath = Paths.get(documentRoot).relativize(file.toPath()).toString();
                String encodedPath = URLEncoder.encode(relativePath, "UTF-8").replace("+", "%20");
                htmlContent.append("<li><a href=\"/").append(encodedPath);
                if (file.isDirectory()) {
                    htmlContent.append("/"); // Ajouter un slash pour les répertoires
                }
                htmlContent.append("\">").append(name);
                if (file.isDirectory()) {
                    htmlContent.append("/");
                }
                htmlContent.append("</a></li>\n");
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

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
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
        } else {
            return "application/octet-stream"; // Type par défaut pour les fichiers inconnus
        }
    }

    private static void sendErrorResponse(Socket socket, int statusCode, String statusMessage) throws IOException {
        OutputStream out = socket.getOutputStream();
        String errorMessage = "<h1>" + statusCode + " " + statusMessage + "</h1>";
        String responseHeader = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + errorMessage.length() + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(responseHeader.getBytes());
        out.write(errorMessage.getBytes());
    }

    /**
     * Enregistre les accès au serveur dans un fichier de log.
     * Le format est un exemple simple : [Date Heure] [IP Client] [Ressource demandée] [Code HTTP]
     */
    private static void logAccess(String clientIp, String requestedResource, String httpStatus) {
        if (accessLogPath == null) {
            return; // Le logging d'accès est désactivé si accessLogPath est null
        }
        try {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMATTER);
            String logEntry = String.format("[%s] %s %s %s%n",
                    timestamp,
                    clientIp != null ? clientIp : "UNKNOWN_IP",
                    requestedResource != null ? requestedResource : "UNKNOWN_RESOURCE",
                    httpStatus != null ? httpStatus : "UNKNOWN_STATUS");
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

    private static void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket : " + e.getMessage());
                logError("Erreur à la fermeture du socket client : " + e.getMessage());
            }
        }
    }

    private static void closeSocket(ServerSocket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erreur à la fermeture du socket serveur : " + e.getMessage());
                logError("Erreur à la fermeture du socket serveur : " + e.getMessage());
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
                    logError("Erreur lors de la fermeture d'un stream : " + e.getMessage());
                }
            }
        }
    }
}
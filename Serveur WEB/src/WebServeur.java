// WebServeur.java

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URLEncoder;

public class WebServeur {

    public static void main(String[] args) {
        // Créer une instance de WebServeurConfig pour charger et gérer la configuration
        WebServeurConfig config = new WebServeurConfig(); // CHANGEMENT ICI

        // Récupérer les valeurs de configuration
        int currentPort = config.getPort();
        String currentDocumentRoot = config.getDocumentRoot();
        String currentDirectoryListing = config.getDirectoryListing();
        List<String> currentAllowedIps = config.getAllowedIps();
        List<String> currentDeniedIps = config.getDeniedIps();

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
                    // Passer les variables de configuration directement à handleClient
                    handleClient(clientSocket, currentDocumentRoot, currentDirectoryListing,
                            currentAllowedIps, currentDeniedIps);
                } catch (IOException e) {
                    System.out.println("Erreur lors du traitement du client : " + e.getMessage());
                } finally {
                    closeSocket(clientSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur au démarrage du serveur : " + e.getMessage());
        } finally {
            closeSocket(serverSocket);
        }
    }

    private static void handleClient(Socket socket, String documentRoot, String directoryListingSetting, List<String> allowedIps, List<String> deniedIps) {
        BufferedReader in = null;
        OutputStream out = null;

        try {
            String clientIp = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();

            // Vérification des IPs
            if (deniedIps.contains(clientIp)) {
                System.out.println("Connexion refusée pour IP : " + clientIp + " (bloquée par liste de refus)");
                sendErrorResponse(out, "403 Forbidden", "Forbidden", "Your IP is blocked by the server configuration.");
                return;
            }
            if (!allowedIps.isEmpty() && !allowedIps.contains(clientIp)) {
                System.out.println("Connexion refusée pour IP : " + clientIp + " (non autorisée par liste d'autorisation)");
                sendErrorResponse(out, "403 Forbidden", "Forbidden", "You are not allowed to access this server from your IP address.");
                return;
            }

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Requête reçue de " + clientIp + " : " + requestLine);

            String[] parts = requestLine.split(" ");
            String path = parts.length >= 2 ? parts[1] : "/";
            if (path.equals("/")) path = "/index.html";

            File file = new File(documentRoot + path);

            // Gestion de l'affichage des répertoires
            if (file.isDirectory()) {
                if (directoryListingSetting.equalsIgnoreCase("on")) {
                    System.out.println("Tentative d'accès au répertoire : " + file.getPath() + " (affichage activé)");
                    sendHtmlListing(out, file, documentRoot);
                } else {
                    System.out.println("Accès au répertoire refusé : " + file.getPath() + " (affichage désactivé)");
                    sendErrorResponse(out, "403 Forbidden", "Forbidden", "Directory listing is disabled for this server.");
                }
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
            } else {
                System.out.println("Fichier non trouvé : " + file.getPath());
                sendErrorResponse(out, "404 Not Found", "Not Found", "The requested file was not found.");
            }

            out.flush();
        } catch (IOException e) {
            System.out.println("Erreur pendant le traitement de la requête : " + e.getMessage());
        } finally {
            closeStreams(in, out);
            closeSocket(socket);
        }
    }

    private static String getMimeType(String filePath) {
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(Paths.get(filePath));
        } catch (IOException e) {
            // Ignorer, on va essayer de deviner par extension
        }

        if (mimeType != null && !mimeType.isEmpty()) {
            return mimeType;
        }

        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".json")) return "application/json";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        if (filePath.endsWith(".pdf")) return "application/pdf";
        if (filePath.endsWith(".xml")) return "application/xml";
        if (filePath.endsWith(".txt")) return "text/plain";
        if (filePath.endsWith(".mp3")) return "audio/mpeg";
        if (filePath.endsWith(".wav")) return "audio/wav";
        if (filePath.endsWith(".mp4")) return "video/mp4";
        if (filePath.endsWith(".webm")) return "video/webm";
        if (filePath.endsWith(".ogg")) return "application/ogg";
        if (filePath.endsWith(".zip")) return "application/zip";
        if (filePath.endsWith(".rar")) return "application/x-rar-compressed";

        return "application/octet-stream";
    }

    private static void sendErrorResponse(OutputStream out, String statusCode, String statusText, String message) throws IOException {
        String errorMessage = "<html><body><h1>" + statusCode + " " + statusText + "</h1><p>" + message + "</p></body></html>";
        String responseHeader = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + errorMessage.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(responseHeader.getBytes());
        out.write(errorMessage.getBytes());
        out.flush();
    }

    private static void sendHtmlListing(OutputStream out, File directory, String documentRoot) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body><h1>Directory Listing for ").append(directory.getPath().replace(documentRoot, "")).append("</h1><ul>");

        if (!directory.toPath().normalize().equals(Paths.get(documentRoot).normalize())) {
            String currentRelativePath = Paths.get(documentRoot).relativize(directory.toPath()).toString();
            Path parentDirectoryPath = directory.getParentFile().toPath();
            String parentRelativePath = Paths.get(documentRoot).relativize(parentDirectoryPath).toString();

            htmlContent.append("<li><a href=\"/").append(parentRelativePath.isEmpty() ? "" : parentRelativePath + "/").append("\">.. (Parent Directory)</a></li>");
        }


        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
                String type = file.isDirectory() ? "/" : "";

                Path relativePathFromRoot = Paths.get(documentRoot).relativize(file.toPath());
                htmlContent.append("<li><a href=\"/").append(relativePathFromRoot.toString()).append(type).append("\">").append(name).append(type).append("</a></li>");
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
}
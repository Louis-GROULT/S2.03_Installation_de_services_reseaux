import java.io.*;
import java.net.*;
import java.nio.file.*;

public class HttpServer {
    private static final String FICHIER_XML = "XML/conf.xml";
    public static void main(String[] args) {
        int port;
        String nb = XmlValueExtracteur.getTagTextValue(FICHIER_XML, "port");
        if (!nb.isEmpty()) {
            port = Integer.parseInt(nb);
        }
        else {
            port = 80;
        }

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur HTTP démarré sur le port " + port);

            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
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

    private static void handleClient(Socket socket) {
        BufferedReader in = null;
        OutputStream out = null;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Requête reçue : " + requestLine);

            String[] parts = requestLine.split(" ");
            String path = parts.length >= 2 ? parts[1] : "/";
            if (path.equals("/")) path = "/index.html";

            File file = new File(BASE_PATH + path);

            if (file.exists() && !file.isDirectory()) {
                byte[] content = Files.readAllBytes(file.toPath());
                String contentType = Files.probeContentType(file.toPath());

                String responseHeader = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n\r\n";

                out.write(responseHeader.getBytes());
                out.write(content);
            } else {
                String errorMessage = "<html><body><h1>404 Not Found</h1></body></html>";
                String responseHeader = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + errorMessage.length() + "\r\n" +
                        "Connection: close\r\n\r\n";

                out.write(responseHeader.getBytes());
                out.write(errorMessage.getBytes());
            }

            out.flush();
        } catch (IOException e) {
            System.out.println("Erreur pendant le traitement de la requête : " + e.getMessage());
        } finally {
            closeStreams(in, out);
            closeSocket(socket);
        }
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
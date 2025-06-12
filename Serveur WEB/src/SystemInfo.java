// SystemInfo.java

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemInfo {

    private static final long MEGABYTE = 1024L * 1024L;

    /**
     * Génère une page HTML avec les informations système.
     * @return Une chaîne de caractères contenant le code HTML de la page d'informations système.
     */
    public static String getSystemInfoHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head><title>Informations Système</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }");
        html.append("h1 { color: #0056b3; border-bottom: 2px solid #0056b3; padding-bottom: 10px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #0056b3; color: white; }");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }");
        html.append(".section { margin-bottom: 30px; border: 1px solid #ccc; padding: 15px; border-radius: 5px; background-color: #fff; }");
        html.append(".warning { color: red; font-weight: bold; }");
        html.append("</style>");
        html.append("</head><body>\n");
        html.append("<h1>Informations Système du Serveur</h1>\n");

        // Informations Générales
        html.append("<div class='section'><h2>Informations Générales</h2><table>\n");
        html.append("<tr><th>Nom de l'hôte</th><td>").append(getHostName()).append("</td></tr>\n");
        html.append("<tr><th>Adresse IP Locale</th><td>").append(getLocalIpAddress()).append("</td></tr>\n");
        html.append("<tr><th>Heure Actuelle du Serveur</th><td>").append(getCurrentServerTime()).append("</td></tr>\n");
        html.append("<tr><th>Temps de fonctionnement du JVM</th><td>").append(getJvmUptime()).append("</td></tr>\n");
        html.append("<tr><th>Chemin d'exécution du JVM</th><td>").append(getJvmHome()).append("</td></tr>\n");
        html.append("<tr><th>Version Java</th><td>").append(System.getProperty("java.version")).append("</td></tr>\n");
        html.append("<tr><th>Architecture JVM</th><td>").append(System.getProperty("os.arch")).append("</td></tr>\n");
        html.append("</table></div>\n");

        // Informations OS
        html.append("<div class='section'><h2>Système d'exploitation</h2><table>\n");
        html.append("<tr><th>Nom du Système</th><td>").append(System.getProperty("os.name")).append("</td></tr>\n");
        html.append("<tr><th>Version du Système</th><td>").append(System.getProperty("os.version")).append("</td></tr>\n");
        html.append("<tr><th>Architecture du Système</th><td>").append(System.getProperty("os.arch")).append("</td></tr>\n");
        html.append("<tr><th>Nombre de Processus Logiques</th><td>").append(Runtime.getRuntime().availableProcessors()).append("</td></tr>\n");
        html.append("<tr><th>Charge Système Moyenne (dernière minute)</th><td>").append(getSystemLoadAverage()).append("</td></tr>\n");
        html.append("</table></div>\n");

        // Informations Mémoire (modifiées pour ne pas utiliser com.sun.management)
        html.append("<div class='section'><h2>Mémoire JVM</h2><table>\n");
        html.append("<tr><th>Mémoire JVM Totale (allouée)</th><td>").append(toMegaBytes(Runtime.getRuntime().totalMemory())).append(" MB</td></tr>\n");
        html.append("<tr><th>Mémoire JVM Libre</th><td>").append(toMegaBytes(Runtime.getRuntime().freeMemory())).append(" MB</td></tr>\n");
        html.append("<tr><th>Mémoire JVM Max (disponible)</th><td>").append(toMegaBytes(Runtime.getRuntime().maxMemory())).append(" MB</td></tr>\n");
        html.append("</table></div>\n");

        // Informations Disque
        html.append("<div class='section'><h2>Espace Disque</h2><table>\n");
        for (File root : File.listRoots()) {
            html.append("<tr><th colspan='2'>Partition : ").append(root.getAbsolutePath()).append("</th></tr>\n");
            html.append("<tr><th>Espace Total</th><td>").append(toMegaBytes(root.getTotalSpace())).append(" MB</td></tr>\n");
            html.append("<tr><th>Espace Libre</th><td>").append(toMegaBytes(root.getFreeSpace())).append(" MB</td></tr>\n");
            html.append("<tr><th>Espace Utilisable</th><td>").append(toMegaBytes(root.getUsableSpace())).append(" MB</td></tr>\n");
        }
        html.append("</table></div>\n");

        html.append("</body></html>");
        return html.toString();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Inconnu";
        }
    }

    private static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Inconnu";
        }
    }

    private static String getCurrentServerTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String getJvmUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtimeBean.getUptime();
        Duration duration = Duration.ofMillis(uptimeMillis);

        long jours = duration.toDays();
        long heures = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long secondes = duration.getSeconds() % 60;

        return String.format("%d jours, %d heures, %d minutes, %d secondes", jours, heures, minutes, secondes);
    }

    private static String getJvmHome() {
        return System.getProperty("java.home");
    }

    private static String getSystemLoadAverage() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double load = os.getSystemLoadAverage();
        if (load >= 0) {
            DecimalFormat df = new DecimalFormat("#.##");
            return df.format(load);
        }
        return "Non supporté";
    }

    private static long toMegaBytes(long bytes) {
        return bytes / MEGABYTE;
    }

    // Cette méthode n'est plus nécessaire si com.sun.management.OperatingSystemMXBean est retiré
    // private static String getPhysicalMemory(String type) {
    //     OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    //     if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
    //         com.sun.management.OperatingSystemMXBean sunOsBean =
    //                 (com.sun.management.OperatingSystemMXBean) osBean;
    //         long bytes = 0;
    //         if ("total".equals(type)) {
    //             bytes = sunOsBean.getTotalPhysicalMemorySize();
    //         } else if ("free".equals(type)) {
    //             bytes = sunOsBean.getFreePhysicalMemorySize();
    //         }
    //         return String.valueOf(toMegaBytes(bytes));
    //     }
    //     return "N/A (Non supporté par cette JVM)";
    // }
}
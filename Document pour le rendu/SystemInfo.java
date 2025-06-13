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
// Supprimer l'importation suivante car elle est spécifique à Sun et non portable
// import com.sun.management.OperatingSystemMXBean;

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
        html.append("th { background-color: #f2f2f2; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append("</style></head><body>\n");
        html.append("<h1>Informations Système du Serveur Web</h1>\n");
        html.append("<table>\n");

        // Informations Générales
        html.append("<tr><th>Propriété</th><th>Valeur</th></tr>\n");
        html.append("<tr><td>Nom d'hôte</td><td>").append(getHostName()).append("</td></tr>\n");
        html.append("<tr><td>Adresse IP locale</td><td>").append(getLocalIpAddress()).append("</td></tr>\n");
        html.append("<tr><td>Système d'exploitation</td><td>").append(System.getProperty("os.name")).append(" (").append(System.getProperty("os.arch")).append(") version ").append(System.getProperty("os.version")).append("</td></tr>\n");
        html.append("<tr><td>Version JVM</td><td>").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")</td></tr>\n");
        html.append("<tr><td>Chemin JVM Home</td><td>").append(getJvmHome()).append("</td></tr>\n");
        html.append("<tr><td>Utilisateur Courant</td><td>").append(System.getProperty("user.name")).append("</td></tr>\n");
        html.append("<tr><td>Répertoire de Travail</td><td>").append(System.getProperty("user.dir")).append("</td></tr>\n");

        // Uptime du serveur
        html.append("<tr><td>Démarrage du Serveur</td><td>").append(getProcessStartTime()).append("</td></tr>\n");
        html.append("<tr><td>Temps de Fonctionnement (Uptime)</td><td>").append(getProcessUptime()).append("</td></tr>\n");

        // Informations sur le processeur
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        html.append("<tr><td>Nombre de cœurs de processeur</td><td>").append(osBean.getAvailableProcessors()).append("</td></tr>\n");
        html.append("<tr><td>Charge Système Moyenne (1 min)</td><td>").append(getSystemLoadAverage()).append("</td></tr>\n");

        // Informations sur la mémoire
        Runtime runtime = Runtime.getRuntime();
        html.append("<tr><td>Mémoire JVM Totale (Max)</td><td>").append(toMegaBytes(runtime.maxMemory())).append(" MB</td></tr>\n");
        html.append("<tr><td>Mémoire JVM Allouée</td><td>").append(toMegaBytes(runtime.totalMemory())).append(" MB</td></tr>\n");
        html.append("<tr><td>Mémoire JVM Libre</td><td>").append(toMegaBytes(runtime.freeMemory())).append(" MB</td></tr>\n");

        // Mémoire physique (Note: nécessite com.sun.management.OperatingSystemMXBean pour des infos détaillées sur la mémoire physique,
        // qui n'est pas portable. Les méthodes standards sont limitées.)
        // Pour une approche portable, on se limite à la JVM ou à la ligne de commande/JMX avec des outils externes.
        // Si vous utilisez une JVM HotSpot, vous pourriez caster, mais ce n'est pas recommandé pour la portabilité.
        html.append("<tr><td>Mémoire Physique Totale</td><td>Non disponible (API non portable)</td></tr>\n");
        html.append("<tr><td>Mémoire Physique Libre</td><td>Non disponible (API non portable)</td></tr>\n");


        html.append("</table>\n");
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

    private static String getProcessStartTime() {
        RuntimeMXBean rmBean = ManagementFactory.getRuntimeMXBean();
        long startTimeMillis = rmBean.getStartTime();
        LocalDateTime startTime = LocalDateTime.ofEpochSecond(startTimeMillis / 1000, 0, java.time.ZoneOffset.UTC); // Ou ZoneId.systemDefault()
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String getProcessUptime() {
        RuntimeMXBean rmBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = rmBean.getUptime();
        Duration duration = Duration.ofMillis(uptimeMillis);

        long secondes = duration.getSeconds();
        long jours = secondes / (24 * 3600);
        secondes %= (24 * 3600);
        long heures = secondes / 3600;
        secondes %= 3600;
        long minutes = secondes / 60;
        secondes %= 60;

        return String.format("%d jours, %d heures, %d minutes, %d secondes", jours, heures, minutes, secondes);
    }

    private static String getJvmHome() {
        return System.getProperty("java.home");
    }

    private static String getSystemLoadAverage() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double load = os.getSystemLoadAverage();
        if (load >= 0) {
            DecimalFormat df = new DecimalFormat("0.00"); // Formater avec deux décimales
            return df.format(load);
        }
        return "Non supporté";
    }

    private static long toMegaBytes(long bytes) {
        return bytes / MEGABYTE;
    }
}
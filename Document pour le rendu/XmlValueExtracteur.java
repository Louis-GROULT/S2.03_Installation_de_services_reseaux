import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;
import java.io.File;

public class XmlValueExtracteur {
    /**
     * Méthode pour récupérer le contenu textuel d'une balise XML.
     *
     * @param filePath Chemin du fichier XML
     * @param tagName  Nom de la balise
     * @return Texte contenu dans la balise (String), ou une chaîne vide si non trouvée/erreur.
     */
    public static String getTagTextValue(String filePath, String tagName) {
        try {
            File xmlFile = new File(filePath);
            // Vérifier si le fichier existe et est lisible AVANT de tenter de le parser
            if (!xmlFile.exists() || !xmlFile.isFile() || !xmlFile.canRead()) {
                // Pas d'erreur ici, car WebServeurConfig gère l'absence du fichier.
                // Cela évite d'afficher "Erreur : Le fichier XML... introuvable" et "Erreur lors de la lecture..."
                // quand le fichier est simplement absent (ce qui est géré par WebServeurConfig).
                return "";
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                return node.getTextContent().trim();
            }
        } catch (Exception e) {
            // Afficher l'erreur si le fichier existe mais est mal formé par exemple.
            System.out.println("Erreur lors de la lecture de la balise <" + tagName + "> dans le fichier " + filePath + " : " + e.getMessage()); // System.out.println
        }
        return "";
    }
}
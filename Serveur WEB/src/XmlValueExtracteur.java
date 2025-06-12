// XmlValueExtracteur.java
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
            // Vérifier si le fichier existe et est lisible
            if (!xmlFile.exists() || !xmlFile.isFile() || !xmlFile.canRead()) {
                System.out.println("Erreur : Le fichier XML de configuration est introuvable ou illisible : " + filePath);
                return ""; // Retourne une chaîne vide si le fichier n'est pas accessible
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
            System.out.println("Erreur lors de la lecture de la balise <" + tagName + "> dans le fichier " + filePath + " : " + e.getMessage());
        }
        return "";
    }
}
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
            // Affichage avec System.out.println comme demandé
            System.out.println("Erreur lors de la lecture de la balise <" + tagName + "> dans le fichier " + filePath + " : " + e.getMessage());
        }
        return "";
    }
}
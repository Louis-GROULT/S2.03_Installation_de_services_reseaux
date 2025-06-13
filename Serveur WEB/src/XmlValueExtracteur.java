import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;
import java.io.File;

public class XmlValueExtracteur {
    /**
     * Méthode pour récupérer le contenu textuel d'une balise XML.
     *
     * @param chemin Chemin du fichier XML
     * @param nomBal  Nom de la balise
     * @return Texte contenu dans la balise (String), ou une chaîne vide si non trouvée/erreur.
     */
    public static String getTexteXml(String chemin, String nomBal) {
        try {
            File xml = new File(chemin);
            // Vérifier si le fichier existe et lisible
            if (!xml.exists() || !xml.isFile() || !xml.canRead()) {
                System.out.println("Le fichier XML de configuration est introuvable ou illisible : " + chemin);
                return ""; // Retourne une chaîne vide si le fichier n'est pas accessible
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(xml);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName(nomBal);
            if (nodeList.getLength() > 0) {
                Node noeud = nodeList.item(0);
                return noeud.getTextContent().trim();
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la lecture de la balise <" + nomBal + "> dans le fichier " + chemin + " : " + e.getMessage());
        }
        return "";
    }
}
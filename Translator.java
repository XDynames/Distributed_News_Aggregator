import java.io.InputStream;
import java.io.StringReader;
import java.io.File;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.io.StringWriter;
 
public class Translator {
    //          === PRIVATE METHOD IMPLEMENTATIONS ===
    // Adds a given field information to the document object
    static private void addInformation(Content doc, String[] content){
        String field = content[0];
        String info = content[1].trim();
        // Add information to the relevant object variable
        switch(field){
            case "title": doc.setTitle(info); break;
            case "subtitle": doc.setSubtitle(info); break;
            case "link": doc.setLink(info); break;
            case "updated": doc.setUpdated(info); break;
            case "uuid": doc.setID(info); break;
            case "author": doc.setAuthor(info); break;
            case "summary": doc.setSummary(info); break;
            default: break;
        }
    }
    // Marshall fields and information from content file into document object
    static private void marshall(Content doc, String[] content){
        for (String item: content){ addInformation(doc, item.split(":")); }
    }

    //          === PUBLIC METHOD IMPLEMENTATIONS ===
    /*
        Reads in a file as a string, removing xml characters &#xD;
        Args: - filename: name of the file to read
        Return: - file content string
    */
    static public String readFile(String filename) throws IOException{
        String fileString = new String (Files.readAllBytes(Paths.get(filename)));
        return fileString.replaceAll("&#xD;","");
    }

    /*
        Reads a raw content file, marshals it do a Content
        object then returns it as a serialised XML string
        Args: - filename: name of the raw file to read
        Return: - serialised object as XML string
    */
    static public String fileToXMLString(String filename) 
                                        throws IOException, JAXBException{
        // Creates documnet object and adds input content to its properties
        Content docObject = new Content();
        marshall(docObject, readFile(filename).split("\n"));
        return objToString(docObject);
    }

    /*
        Reads a raw feed file, marshals it do a ATOMFeed
        object then returns it as a serialised XML string
        Args: - filename: name of the raw feed info file
        Returns: - serialised object as XML string

    */
    static public ATOMFeed feedFileToObject(String filename) 
                                        throws IOException, JAXBException{
        // Creates document object and adds input content to its properties
        Content docObject = new ATOMFeed();
        marshall(docObject, readFile(filename).split("\n"));
        return (ATOMFeed) docObject;
    }


    /*
        Reads a raw content file, converting it to a serialised XML string
        which is then written to disk as an XML file
        Args: - filename: name of the raw content file
    */
    static public void contentToXMLFile(String filename) 
                                        throws IOException, JAXBException{
        // Creates document object and adds input content to its properties
        Content docObject = new Content();
        marshall(docObject, readFile(filename).split("\n"));
        // Instance an XML marshaller
        JAXBContext context = JAXBContext.newInstance(Content.class);
        Marshaller xmlMarshaller = context.createMarshaller();
        // Write XML to disk <contentFilename>.xml
        String xmlFilename = filename.substring(0,filename.length()-3)+"xml";
        File file = new File(xmlFilename);
        xmlMarshaller.marshal(docObject, file);
    }

    /*
        Converts a Content object into a serialised XML string
        Args: - obj: Content object to be serialised
        Returns: - Serialised XML string
    */
    static public String objToString(Content obj) throws IOException, JAXBException{
        // Instance an XML marshaller
        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        Marshaller xmlMarshaller = context.createMarshaller();
        // Write marshal output to a string
        java.io.StringWriter sw = new StringWriter();
        xmlMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        xmlMarshaller.marshal(obj, sw);
        return sw.toString();
    }   

    /*
        Given a serialised XML string representing a Content object the
        object is reconstructed
        Args: - xmlString: serialised object
        Returns: Reconstructed Content object
    */
    static public Content unmarshal(String xmlString) throws JAXBException{
        JAXBContext context = JAXBContext.newInstance(Content.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        // Creates a new object from the XML String
        return  (Content) unmarshaller.unmarshal(new StringReader(xmlString));
    }

    /*
        Given a serialised XML string representing a ATOMFeed object the
        object is reconstructed
        Args: - xmlString: serialised object
        Returns: Reconstructed ATOMFeed object
    */
    static public ATOMFeed stringToFeed(String xmlString) throws JAXBException{
        JAXBContext context = JAXBContext.newInstance(ATOMFeed.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        // Creates a new object from the XML String
        return  (ATOMFeed) unmarshaller.unmarshal(new StringReader(xmlString));
    }

    /*
        Prints the object to the terminal in serialised XML form
        Args: - object: Content object to print
    */
    static public void printObject(Content object) throws JAXBException{
        JAXBContext context = JAXBContext.newInstance(object.getClass());
        Marshaller xmlMarshaller = context.createMarshaller();
        xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        xmlMarshaller.marshal(object, System.out);
    }
 
}
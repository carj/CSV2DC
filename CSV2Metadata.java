/*
 * Copyright [2018] [James Carr]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Iterator;
import java.util.Properties;

/**
 *  Class to read a CSV file with headers and create an XML file
 *  for each row in the spreadsheet.
 *
 *  Rows without a dc or dcterms namespace will be ignored.
 *
 */
public class CSV2Metadata {

    private Log log = LogFactory.getLog(getClass());


    private HttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
    private CloseableHttpClient httpclient;

    private javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();

    private TransformerFactory transformerFactory = TransformerFactory.newInstance();

    private static final String XIP_NS = "http://www.tessella.com/XIP/v4";
    private static final String XIPV6_NS = "http://preservica.com/EntityAPI/v6.0";

    private Properties userDetails;

    public CSV2Metadata(Properties userDetails) {
        factory.setNamespaceAware(true);
        this.userDetails = userDetails;
    }

    /**
     *  The java Main entry point for executing the class
     *
     * @param args  command line arguments
     */
    public static void main(String[] args) {

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption( "i", "input", true, "input csv file to parse" );
        options.addOption( "c", "column", true, "the column name in the csv which contains the filename of the output xml file" );
        options.addOption( "o", "output", true, "the folder which will contain the xml documents" );
        options.addOption( "r", "root", true, "the root element of the dublin core xml, defaults to dc" );
        options.addOption( "n", "namespace", true, "the root element namespace, defaults to http://purl.org/dc/elements/1.1/" );
        options.addOption( "p", "prefix", true, "the root element namespace prefix, defaults to dc" );
        options.addOption( "u", "user", true, "the property file with Preservica username & password" );
        options.addOption( "h", "help", false, "print this message" );

        HelpFormatter formatter = new HelpFormatter();

        final String cmdLine = "csv2dc.cmd -i file.csv -o output [-c \"file name column\"] [-r root] [-p prefix] [-n namespace]";

        String DEFAULT_FILE_COLUMN = "filename";
        String DEFAULT_ROOT_ELEMENT = "dc";
        String DEFAULT_ROOT_PREFIX = "dc";
        String DEFAULT_ROOT_namespace = "http://purl.org/dc/elements/1.1/";
        String fileColumn;
        String rootElement;
        String rootPrefix;
        String rootNamespace;
        File inputFile = null;
        File outputDir = null;

        Properties userDetails = new Properties();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );

            if ( line.hasOption( "h" ) ) {
                formatter.printHelp( cmdLine, options );
                System.exit(1);
            }

            if ( line.hasOption( "r" ) ) {
                rootElement = line.getOptionValue( "r" );
            } else {
                rootElement =  DEFAULT_ROOT_ELEMENT;
            }

            if ( line.hasOption( "u" ) ) {
                String properties = line.getOptionValue( "u" );
                userDetails.load(new FileInputStream(properties));
            }

            if ( line.hasOption( "p" ) ) {
                rootPrefix = line.getOptionValue( "p" );
            } else {
                rootPrefix =  DEFAULT_ROOT_PREFIX;
            }

            if ( line.hasOption( "n" ) ) {
                rootNamespace = line.getOptionValue( "n" );
            } else {
                rootNamespace =  DEFAULT_ROOT_namespace;
            }

            if ( line.hasOption( "c" ) ) {
                fileColumn = line.getOptionValue( "c" );
            } else {
                fileColumn =  DEFAULT_FILE_COLUMN;
            }

            if ( line.hasOption( "i" ) ) {
                String inputFileName = line.getOptionValue( "i" );
                inputFile = new File(inputFileName);
                if (!inputFile.exists()) {
                    System.out.println(String.format("The input file name %s does not exist", inputFileName));
                    System.exit(1);
                }
            } else {
                formatter.printHelp( cmdLine, options );
                System.exit(1);
            }

            if ( line.hasOption( "o" ) ) {
                String outputFolder = line.getOptionValue( "o" );
                outputDir = new File(outputFolder);
                outputDir.mkdirs();
                if ( (!outputDir.exists()) ||  (!outputDir.isDirectory()) ) {
                    System.out.println(String.format("The output directory %s does not exist", outputFolder));
                    System.exit(1);
                }
            } else {
                formatter.printHelp( cmdLine, options  );
                System.exit(1);
            }



            try {
                CSV2Metadata metadata = new CSV2Metadata(userDetails);
                int files = metadata.parse(inputFile, outputDir, fileColumn, rootElement, rootPrefix, rootNamespace);
                System.out.println(String.format("Created %d XML files in %s", files, outputDir.getName()));
            } catch (Exception e) {
                formatter.printHelp( cmdLine, options  );
                System.exit(1);
            }

        }
        catch( ParseException exp ) {
            System.out.println(exp.getMessage());
            formatter.printHelp( cmdLine, options  );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  remove any attributes from the closing element
     *
     * @param element The opening tag element
     * @return String The closing tag element
     */
    private String getClosingElement(String element) {

        if (element.contains(" ")) {
            String[] elements = element.split(" ");
            return elements[0];
        } else {
            return element;
        }

    }

    /**
     *  Loop over the csv file and create xml elements for each dublin core column.
     *
     * @param csvDocument The CSV file
     * @param folder      The output folder
     * @throws Exception
     */
    private int parse(File csvDocument, File folder, String filenameColumn, String rootElement, String rootPrefix, String rootNamespace) throws Exception {

        final String DC_NS = "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"";
        final String DCTERMS_NS = "xmlns:dcterms=\"http://purl.org/dc/terms/\"";
        final String XSI_NS = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

        //Reader in = new FileReader(csvDocument);

        final URL url = csvDocument.toURL();

        final Reader reader = new InputStreamReader(new BOMInputStream(url.openStream()), "UTF-8");
        final CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL);

        //CSVParser parser = CSVFormat.RFC4180.parse(in);
        Iterator<CSVRecord> recordItor =  parser.iterator();
        CSVRecord headerRecord = recordItor.next();

        int headerCount = headerRecord.size();

        String[] headers = new String[headerCount];

        int fileNameColumnId = -1;
        boolean containsFileName = false;
        for (int i = 0; i < headerCount; i++) {
            headers[i] = headerRecord.get(i);
            if (headers[i].contains(filenameColumn)) {
                fileNameColumnId = i;
                containsFileName = true;
            }
        }

        if (!containsFileName) {
            System.out.println(String.format("The CSV file does not contain a column with the name %s", filenameColumn));
            System.exit(1);
        }

        int numFiles = 0;

        while (recordItor.hasNext()) {

            CSVRecord record = recordItor.next();
            String filename = record.get(fileNameColumnId);
            File xmlFile = new File(folder, String.format("%s.metadata", filename));

            FileOutputStream fos = new FileOutputStream(xmlFile);
            Writer osw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

            osw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            osw.write(System.getProperty("line.separator"));

            String root = String.format("%s:%s xmlns:%s=\"%s\" %s %s", rootPrefix, rootElement, rootPrefix, rootNamespace, DCTERMS_NS, XSI_NS);
            if (!root.contains(DC_NS)) {
                root = String.format("%s %s", root, DC_NS);
            }
            osw.write("<");
            osw.write(root);
            osw.write(">");
            osw.write(System.getProperty("line.separator"));

            String filerefId = null;
            String assetId = null;

            for (int i = 0; i < headerCount; i++) {
                String header = headers[i];
                boolean isDublinCore = (header.startsWith("dc:") || header.startsWith("dcterms:"));
                if (isDublinCore) {
                    String value = record.get(i).trim();
                    String closingElement = getClosingElement(header);
                    if (value == null || value.isEmpty()) {
                        osw.write(String.format("\t<%s />", header));
                    } else {
                        osw.write(String.format("\t<%s>%s</%s>", header, value, closingElement));
                    }
                    osw.write(System.getProperty("line.separator"));
                }
                if (header.toLowerCase().trim().startsWith("fileref")) {
                    filerefId = record.get(i).trim();
                }
                if (header.toLowerCase().trim().startsWith("assetid")) {
                    assetId = record.get(i).trim();
                }
            }
            osw.write(String.format("</%s:%s>", rootPrefix, rootElement));
            osw.flush();
            osw.close();
            fos.close();

            numFiles++;

            // if the entity does not have descriptive metadata with the required
            // namespace then add it.

            if (filerefId != null && (filerefId.length() > 0)) {
                if ((userDetails != null) && (!userDetails.isEmpty())) {
                    Document xipDocument = getEntityV5(filerefId);
                    if (xipDocument != null) {
                        if (!hasDublinCoreV5(xipDocument, rootNamespace)) {
                            org.w3c.dom.Document dublinCoreDocument = getDocumentFromFile(xmlFile);
                            xipDocument = addDublinCoreV5(dublinCoreDocument, xipDocument, rootNamespace);
                            updateEntityV5(xipDocument, filerefId);
                        } else {
                            System.out.println("Entity: " + filerefId + " already has Dublin Core metadata. Ignoring....");
                        }
                    } else {
                        System.out.println("Failed to find a Preservica entity with ID: " + filerefId);
                    }
                } else {
                    System.out.println("Create a preservica.properties file with username and password");
                    System.out.println("to update entries");
                }
            }

            if (assetId != null && (assetId.length() > 0)) {
                if ((userDetails != null) && (!userDetails.isEmpty())) {
                    Document xipDocument = getEntityV6(assetId);
                    if (xipDocument != null) {
                        if (!hasDublinCoreV6(xipDocument, rootNamespace)) {
                            org.w3c.dom.Document dublinCoreDocument = getDocumentFromFile(xmlFile);
                            updateEntityV6(dublinCoreDocument, assetId);
                        } else {
                            System.out.println("Asset: " + assetId + " already has Dublin Core metadata. Ignoring....");
                        }
                    } else {
                        System.out.println("Failed to find a Preservica asset with ID: " + assetId);
                    }
                }
            }
        }

        return numFiles;
    }

    /**
     *  Update the Preservica File entity with the dublin core metadata
     *
     *
     * @param document
     * @param entityRef
     */
    private void updateEntityV6(Document document, String entityRef) {
        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;
        try {

            String domain = userDetails.getProperty("preservica.domain");

            HttpPost postRequest = new HttpPost(String.format("https://%s/api/entity/information-objects/%s/metadata", domain, entityRef.trim()));
            postRequest.setHeader("Authorization", getHeader());
            postRequest.setHeader("Content-Type", "application/xml");

            document.normalize();

            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(domSource, result);

            StringEntity se = new StringEntity(writer.toString(), "UTF-8");
            postRequest.setEntity(se);
            response = client.execute(postRequest);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                log.info("Updated object: " + entityRef);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Failed to update entity");
                log.error(response.getStatusLine().toString());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return;
    }



    /**
     *  Update the Preservica File entity with the dublin core metadata
     *
     *
     * @param document
     * @param entityRef
     */
    private void updateEntityV5(Document document, String entityRef) {
        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;
        try {

            String domain = userDetails.getProperty("preservica.domain");

            HttpPut putRequest = new HttpPut(String.format("https://%s/api/entity/digitalFiles/%s", domain, entityRef.trim()));
            putRequest.setHeader("Authorization", getHeader());

            document.normalize();

            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(domSource, result);

            StringEntity se = new StringEntity(writer.toString(), "UTF-8");
            putRequest.setEntity(se);
            response = client.execute(putRequest);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                log.info("Updated object: " + entityRef);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Failed to update entity");
                log.error(response.getStatusLine().toString());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return;
    }



    /**
     * Add dublin core metadata to an existing file entity
     *
     * @param  dublinCore
     * @param  xipDocument
     * @return Document
     */
    private Document addDublinCoreV5(Document dublinCore, Document xipDocument, String namespace) {

        // Create a new Metadata element
        Element metadataElement = xipDocument.createElement("Metadata");
        metadataElement.setAttribute("schemaURI", namespace);

        // add the dublin core to it.
        Node dublinCoreNode = xipDocument.importNode(dublinCore.getDocumentElement(), true);
        metadataElement.appendChild(dublinCoreNode);

        // metadata goes after the "Directory" element;
        NodeList elements = xipDocument.getDocumentElement().getElementsByTagName("Directory");
        if (elements.getLength() == 1) {
            Element elem = (Element)elements.item(0);
            elem.getParentNode().insertBefore(metadataElement, elem.getNextSibling());
        }

        return xipDocument;
    }


    /**
     * Create a org.w3c.dom.Document from the dublin Core Metadata file
     *
     * @param  xmlFile
     * @return Document
     */
    private Document getDocumentFromFile(File xmlFile)  {
        org.w3c.dom.Document document = null;
        try {
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(xmlFile);
        } catch (Exception ex) {
            log.error(ex);
        }
        return document;
    }

    private boolean hasDublinCoreV6(Document document, String namespace) {

        NodeList metadataList = document.getElementsByTagNameNS(XIPV6_NS, "Metadata");
        for (int i = 0; i < metadataList.getLength(); i++) {
            Node metadataNode = metadataList.item(i);
            NodeList fragmentList = metadataNode.getChildNodes();
            for (int j = 0; j < fragmentList.getLength(); j++) {
                Node fragmentNode = fragmentList.item(j);
                NamedNodeMap namedNodeMap =  fragmentNode.getAttributes();
                if (namedNodeMap != null) {
                    Node attribute = namedNodeMap.getNamedItem("schema");
                    if (attribute != null) {
                        if (attribute.getNodeValue().equals(namespace)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check that the current document does not have generic metadata already
     * with the same namespace.
     * make its safe to re-run the program
     *
     * @param document
     * @param namespace
     * @return true
     */
    private boolean hasDublinCoreV5(Document document, String namespace) {

        NodeList list = document.getElementsByTagNameNS(XIP_NS, "Metadata");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            NamedNodeMap namedNodeMap =  node.getAttributes();
            if (namedNodeMap != null) {
                Node attribute = namedNodeMap.getNamedItem("schemaURI");
                if (attribute != null) {
                    if (attribute.getNodeValue().equals(namespace)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getHeader() {
        byte[] bytes = Base64.getEncoder().encode(String.format("%s:%s", userDetails.getProperty("preservica.username"), userDetails.getProperty("preservica.password")).getBytes());
        return String.format("Basic %s",  new String(bytes, Charset.forName("UTF-8")));
    }


    /**
     *  Get a Preservica v6 asset by its asset ref
     *
     * @param assetRef
     * @return org.w3c.dom Document of XIP XML
     */
    private Document getEntityV6(String assetRef) {

        String domain = userDetails.getProperty("preservica.domain");

        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(String.format("https://%s/api/entity/information-objects/%s", domain, assetRef.trim()));
            httpGet.setHeader("Authorization", getHeader());
            response = client.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return getDocument(response);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Failed to create get entity");
                log.error(response.getStatusLine().toString());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     *  Get a Preservica v5 entity by its reference
     *
     * @param entityRef
     * @return org.w3c.dom Document of XIP XML
     */
    private Document getEntityV5(String entityRef) {

        String domain = userDetails.getProperty("preservica.domain");

        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(String.format("https://%s/api/entity/entities/%s", domain, entityRef.trim()));
            httpGet.setHeader("Authorization", getHeader());
            response = client.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return getDocument(response);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Failed to create get entity");
                log.error(response.getStatusLine().toString());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * Get the http client for the REST calls.
     *
     * @return HttpClient
     */
    private CloseableHttpClient getClient() {
        if (httpclient == null) {
            httpclient = HttpClients.custom().setConnectionManager(cm).build();
        }
        return httpclient;
    }

    /**
     *  Create a document from a http response
     *
     * @param   response
     * @return  Document
     *
     * @throws Exception
     */
    private Document getDocument(CloseableHttpResponse response) throws Exception {
        org.w3c.dom.Document document;
        StringWriter sw = new StringWriter();
        IOUtils.copy(response.getEntity().getContent(), sw);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = null;
        try {
            is = new java.io.ByteArrayInputStream(sw.toString().getBytes(Charset.forName("UTF-8")));
            document = builder.parse(is);
        } finally {
            IOUtils.closeQuietly(is);
            EntityUtils.consumeQuietly(response.getEntity());
            IOUtils.closeQuietly(response);
        }
        return document;
    }

}

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

import java.io.*;
import java.util.Map;
import java.util.Set;

/**
 *  Class to read a CSV file with headers and create an XML file
 *  for each row in the spreadsheet.
 *
 *  Rows without a dc or dcterms namespace will be ignored.
 *
 */
public class CSV2Metadata {
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
                if ( (!outputDir.exists()) ||  (!outputDir.isDirectory()) ) {
                    System.out.println(String.format("The output directory %s does not exist", outputFolder));
                    System.exit(1);
                }
            } else {
                formatter.printHelp( cmdLine, options  );
                System.exit(1);
            }

            try {
                CSV2Metadata metadata = new CSV2Metadata();
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

        Reader in = new FileReader(csvDocument);
        CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        Map<String, Integer> headerMap = parser.getHeaderMap();

        boolean containsFileName = headerMap.containsKey(filenameColumn);
        if (!containsFileName) {
            System.out.println(String.format("The CSV file does not contain a column with the name %s", filenameColumn));
            System.exit(1);
        }

        int numFiles = 0;

        for (CSVRecord record : parser) {

            String filename = record.get(filenameColumn);
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
            Set<String> headers = headerMap.keySet();
            for (String element : headers) {
                boolean isDublinCore = (element.startsWith("dc:") || element.startsWith("dcterms:"));
                if (isDublinCore) {
                    String value = record.get(element).trim();
                    String closingElement = getClosingElement(element);
                    if (value == null || value.isEmpty()) {
                        osw.write(String.format("\t<%s />", element));
                    } else {
                        osw.write(String.format("\t<%s>%s</%s>", element, value, closingElement));
                    }
                    osw.write(System.getProperty("line.separator"));
                }
            }

            osw.write(String.format("</%s:%s>", rootPrefix, rootElement));
            osw.flush();
            osw.close();
            fos.close();

            numFiles++;
        }

        return numFiles;
    }
}

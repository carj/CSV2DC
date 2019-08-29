# csv2dc
Convert a spreadsheet (CSV) file into [Dublin Core](http://dublincore.org/) XML files
The XML files conform to the [Preservica](http://preservica.com/) naming convention linking them to digital files which they describe.

Use this program if you have local digital files which have not yet been ingested into Preservica.


Usage:

`csv2dc.cmd -i file.csv -o output [-c "file name column"] [-r root] [-p prefix] [-n namespace]`

The input CSV file should have header column names which start with dc: or dcterms:

Attributes are allowed in elements.


for example:

- dc:title
- dc:description
- dc:subject
- dcterms:provenance
- dc:source xsi:type="dcterms:URI"

Columns which do not have this form are ignored.

eg.

filename | dc:description | dc:identifier | dc:title | dc:subject | dcterms:provenance 
-------- | -------------  | ------------- | -------- | ----------- | -----------
LC-USZ62-20901.tiff | Picture of a plane | LC-USZ62-20901 | Photo Title | Plane | LOC
LC-USZ62-43601.tiff | Picture of a Car | LC-USZ62-43601 | Photo Title2 | Car | LOC


The XML output files are written to the `output` folder specified by the `-o` argument. The XML file name is based on a column header given by the `-c` argument. The default column name for the name of the file is `filename`.

examples:

`csv2dc.cmd -i file.csv -o output -c filename`

The output would be a file called `LC-USZ62-20901.tiff.metadata`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dc:dc xmlns:dc="http://purl.org/dc/elements/1.1/"    xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<dc:description>Picture of a plane</dc:description>
	<dc:identifier>LC-USZ62-20901</dc:identifier>
	<dc:title>Photo Title</dc:title>
	<dc:subject>Plane</dc:subject>
	<dcterms:provenance>LOC</dcterms:provenance>
</dc:dc>
```

You can configure the root element and its namespace through the `-r -p -n` options

`csv2dc.cmd -i file.csv -o output -c filename -r metadata -p ns -n http://my.namespace.com`

produces

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ns:metadata xmlns:ns="http://my.namespace.com" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:dc="http://purl.org/dc/elements/1.1/">
	<dc:description>Picture of a plane</dc:description>
	<dc:identifier>LC-USZ62-20901</dc:identifier>
	<dc:title>Photo Title</dc:title>
	<dc:subject>Plane</dc:subject>
	<dcterms:provenance>LOC</dcterms:provenance>
</ns:metadata>
```
To re-create the dublin core schema used by [OAI-PMH](https://www.openarchives.org/OAI/openarchivesprotocol.html)

```xml
<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
	 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"   xmlns:dc="http://purl.org/dc/elements/1.1/"  >
	...
	....
	</oai_dc:dc>
```

Then use the following command line arguments:

`csv2dc.cmd -i file.csv -o output -c filename -r dc -p oai_dc -n http://www.openarchives.org/OAI/2.0/oai_dc/`


# csv2preservica

This program will convert a spreadsheet (CSV) file into [Dublin Core](http://dublincore.org/) XML files
and then add these XML dublin core metadata files to the digital files within a [Preservica](http://preservica.com/) system using the API.
The Spreadsheet has the same format as above, but must contain a column containing the Preservica fileRef of the digital object 
the metadata will be attached to.

Use this program if you the digital files have already been ingested into Preservica and you now want to 
add additional descriptive metadata held in a spreadsheet.

For v5.x Preservica systems:

The additional column in the spreadsheet must be called "fileref" and contain the UUID of the Preservica digital file.

eg.

filename            | fileref  |  dc:description | dc:identifier | dc:title | dc:subject | dcterms:provenance 
--------            | -------- | -------------  | ------------- | -------- | ----------- | -----------
LC-USZ62-20901.tiff | 8283edc6-8016-4100-a94c-3db90b0e4a75         | Picture of a plane | LC-USZ62-20901 | Photo Title | Plane | LOC
LC-USZ62-43601.tiff | 9183edc6-2115-2912-b8ad-5ef3013c7b21         | Picture of a Car | LC-USZ62-43601 | Photo Title2 | Car | LOC

For v6.x Preservica systems:

The additional column in the spreadsheet must be called "assetId" and contain the UUID of the Preservica digital asset.

eg.

filename            | assetId  |  dc:description | dc:identifier | dc:title | dc:subject | dcterms:provenance 
--------            | -------- | -------------  | ------------- | -------- | ----------- | -----------
LC-USZ62-20901.tiff | 8283edc6-8016-4100-a94c-3db90b0e4a75         | Picture of a plane | LC-USZ62-20901 | Photo Title | Plane | LOC
LC-USZ62-43601.tiff | 9183edc6-2115-2912-b8ad-5ef3013c7b21         | Picture of a Car | LC-USZ62-43601 | Photo Title2 | Car | LOC


To use the web service API to update metadata in Preservica you will need to create 
a properties file called `preservica.properties` in the program directory
and set the following values.

* preservica.domain={us.preservica.com,eu.preservica.com,au.preservica.com,ca.preservica.com}
* preservica.username=jo@example.com
* preservica.password=xxxxxxx

The command line arguments for controlling the dublin core metadata are the same:

Usage:

`csv2preservica.cmd -i file.csv -o output [-c "file name column"] [-r root] [-p prefix] [-n namespace]`


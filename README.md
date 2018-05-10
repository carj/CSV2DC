# CSV2DC
Convert a spreadsheet (CSV) file into Dublin Core XML files

Usage:

`csv2dc.cmd -i file.csv -o output [-c "file name column"] [-r root] [-p prefix] [-n namespace]`

The input CSV file should have header column names which start with dc: or dcterms:

Attributes are allowed in elements.


for example:

- dc:title
- dc:description
- dc:description
- dcterms:provenance
- dc:source xsi:type="dcterms:URI"

Columns which do not have this form are ignored.

eg.

filename | dc:description | dc:identifier | dc:title | dc:subject | dcterms:provenance 
-------- | -------------  | ------------- | -------- | ----------- | -----------
LC-USZ62-20901.tiff | Picture of a plane | LC-USZ62-20901 | Photo Title | Plane | LOC
LC-USZ62-43601.tiff | Picture of a Car | LC-USZ62-43601 | Photo Title2 | Car | LOC


The XML output files are written to the `output` folder specified by the `-o` argument. The XML file name is based on a column header given by the `-c` argument.

examples:

`csv2dc.cmd -i file.csv -o output -c filename`

The output would be

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


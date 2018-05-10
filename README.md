# CSV2DC
Convert a spreadsheet (CSV) file into Dublin Core XML files

Usage:

`.\csv2dc.cmd -i .\file.csv -o output -c "filename"`

The input CSV file should have column names which start with dc: or dcterms:
attributes are allowed.

for example:

- dc:title
- dc:description
- dcterms:provenance
- dc:description
- dc:source xsi:type="dcterms:URI"

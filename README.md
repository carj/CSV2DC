# CSV2DC
Convert a spreadsheet (CSV) file into Dublin Core XML files

Usage:

`.\csv2dc.cmd -i .\file.csv -o output -c "filename"`

The input CSV file should have header column names which start with dc: or dcterms:
attributes are allowed.

for example:

- dc:title
- dc:description
- dc:description
- dcterms:provenance
- dc:source xsi:type="dcterms:URI"

Columns which do not have this form are ignored.

The XML output files are written to the `output` folder specified by the `-o` argument. The XML file name is based on a column header given by the `-c` argument.


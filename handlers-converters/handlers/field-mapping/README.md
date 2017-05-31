# Field Mapping Handler

The Field Mapping Handler renames the metadata fields of a document, according to a configurable mapping of field names.

## Policy Schema

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>mappings</td>
        <td>Map&lt;String,&nbsp;String&gt;</td>
        <td>
			<p>Defines the mapping of metadata field names that will be applied by the handler.</p>
			<p>Each entry in the map specifies the current name of a field as its key and the desired new name for the field as its value.</p>
			<p>Swapping of field names is supported - in other words, the new name for a field may be the same as the current name of another field.</p>
		</td>
    </tr>
</table>


## Example Policy Definitions

#### A simple renaming of two fields

<pre>
{
  "mappings": 
   {
      "abc": "def",
      "pqr": "xyz"
   }
}
</pre>

<br></br>

#### Renaming of three fields, two mapping to the same new field name

<pre>
{
  "mappings":
  {
    "DRETITLE": "TITLE",
    "EXTENSION": "FILE_EXTENSION",
    "IMPORTMAGICEXTENSION": "FILE_EXTENSION"
  }
}
</pre>

<br></br>

#### Swapping the names of fields

<pre>
{
  "mappings": 
   {
      "abc": "def",
      "def": "pqr",
      "pqr": "xyz",
      "xyz": "abc"
   }
}
</pre>

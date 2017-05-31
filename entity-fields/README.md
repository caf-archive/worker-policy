# Entity Fields
A group of common entity classes which represent the serialization contract for commonly used items which appear in our metadata.

Additionally we have added some utility Factory and Accessor classes for the FieldValues, to make it easier to create / encode and decode using entity-field objects.

## Reprocessing
We also represent the Data Processing Record in this module.

The important field to support reprocessing is called `policyDataProcessingRecord` and contains information about the fields which have been added, updated and deleted from the document during Data Processing. This information is used to return the document to its original state.
 
The DocumentProcessingRecord object consists of:

- `Map<String, FieldChangeRecord> metadataChanges` is a list of changes which have been made to the document metadata fields.
- `FieldChangeRecord referencedChange` is the change made to the document reference.

Each FieldChangeRecord consists of the following:

- `changeType`: enumeration FieldChangeType < Added / Updated / Deleted >
- `changeValues`(Optional): type Collection< FieldValue >, is a collection of objects, which represent the changes.  ( Only required where the field has been updated or deleted ).
	- Where a changeValue exists, each value is an FieldValue object representation which includes:
		- `value` - a string value.
		- `valueEncoding`(Optional) - enumeration of the encoding that was used for the value. 
			- Example useful encodings are:
				- "utf8" for simple strings ( Default if valueEncoding is blank / missing )
				- "base64" for a base64 encoded reference data blob.
				- "caf-storage-reference" for a caf-storage-reference.
		
In this way we can now supply a value in many different encodings, allowing for smaller blobs to represented as strings, which are easier to use for diagnostics.  
Example FieldValue representations:

	"Title" : { 
		"changeType" : "added"
	}
	
	"Content" : {
		"changeType" : "updated",
		"changeValues" : [ {
			"value" : "My Original Content Value",
			"valueEncoding" : "utf8"
		} ]
	} 
	
	"Content" : {
		"changeType" : "updated",
		"changeValues" : [ {
			"value" : "TXkgUmVkYWN0ZWQgQ29udGVudCA8cmVkYWN0ZWQ+",
			"valueEncoding" : "base64"
		} ]
	} 
	
	"Content" : {
		"changeType" : "deleted",
		"changeValues" : [ {
			"value" : "e97cfe6b899c4aeeaf96b6837b202d49/84891abf1b8a4708b8eeeca8c7058e13",
			"valueEncoding" : "caf-storage-reference"
		} ]
	} 

 
A full Example of the processing record is:

Format Example:

	{
		"policyDataProcessingRecord": {
			"metadataChanges": {
				"Content": {
					"changeType": "added",
					"changeValues": null
				},
				"Body": {
					"changeType": "updated",
					"changeValues": [{
						"value": "original value of the body field",
						"valueEncoding": "utf8"
					}]
				},
				"FileSystemLocation": {
					"changeType": "updated",
					"changeValues": [{
						"value": "TXkgUmVkYWN0ZWQgQ29udGVudCA8cmVkYWN0ZWQ+",
						"valueEncoding": "base64"
					}]
				}
			},
			"referenceChange": null
		}
	}


### CAF-Storage references:
Where CAF-Storage references are used, the references should be processed in the same way as they are in the normal "metadataReferences" map.  
Any reference which leaves Policy and flows to a node outside of Policy e.g. the Index Node, should now be stored and cleaned up in a consistent fashion whether they are within the general document `metadataReferences` or within the `policyDataProcessingRecord`.

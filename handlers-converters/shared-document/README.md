# policy-handlers-shareddocument
This is an entity representing a Document that can be output by Policy Handlers and consumed externally.

Below is the format of the SharedDocument.

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
	<tr>
        <td>metadataReferences</td>
        <td>Collection<Map.Entry<String, ReferencedData>></td>
        <td>A collection of key value map entries, mapping field names to ReferencedData objects. Suitable for values that need to be stored externally e.g. a large amount fo text such as Document Content.</td>
    </tr>
    <tr>
         <td>metadata</td>
         <td>Collection<Map.Entry<String, String>></td>
         <td>A collection of key value map entries, mapping field names to field values.</td>
     </tr>
     <tr>
         <td>childDocuments</td>
         <td>Collection<SharedDocument></td>
         <td>Any child Documents of this Document. e.g. files within a Zip archive.</td>
     </tr>
     <tr>
          <td>documentProcessingRecord</td>
          <td>DocumentProcessingRecord<SharedDocument></td>
          <td>A record of processing that has occurred on this document, such as metadata added, updated and deleted.</td>
      </tr>
</table>
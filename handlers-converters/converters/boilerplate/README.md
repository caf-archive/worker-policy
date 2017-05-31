# Boilerplate Worker Converter

The Boilerplate Worker converter will add the matched values, Boilerplate Expression Ids, Boilerplate Email Key Content and Boilerplate Extracted Email Signatures from Boilerplate Worker response message into the 
document as well as replace any fields that have had their matches replaced or removed. It will then return to the Policy worker for further evaluation.

This convert will only be invoked if the response message uses the Task Classifier of BoilerplateWorker

## Fields Added To Document

*   `BOILERPLATE_MATCH_ID`:  - The ID of the Boilerplate Expression that was matched.  
    e.g. BOILERPLATE\_MATCH\_ID:1

*   `BOILERPLATE_MATCH_$MATCHEDID_VALUE`: - The text on the document that matched the expression. Where $MATCHEDID is the ID of the expression that matched.
    e.g. BOILERPLATE\_MATCH\_1\_VALUE:matchedText

*   `BOILERPLATE_PRIMARY_CONTENT`: - The primary content extracted from the document, held as a ReferencedData object.

*   `BOILERPLATE_SECONDARY_CONTENT`: - The secondary content extracted from the document, held as a ReferencedData object.

*   `BOILERPLATE_TERTIARY_CONTENT`: - The tertiary content extracted from the document, held as a ReferencedData object.

*   `BOILERPLATE_SIGNATURE_EXTRACTION_STATUS`: - The status of signature extraction operation.  
    e.g. BOILERPLATE\_SIGNATURE\_EXTRACTION\_STATUS: SIGNATURES_EXTRACTED

*   `BOILERPLATE_EXTRACTED_SIGNATURES`: - A signature extracted from the document, held as a ReferencedData object.
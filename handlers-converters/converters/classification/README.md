# Classification Worker Converter

The Classification Worker Converter will add information about classifications that the document matched.
It will then return the document to Policy Worker for further evaluation. 

This converter will be invoked if the response message uses the Task Classifier of 'PolicyClassificationWorker' or 'PolicyClassificationWorkerElasticsearch'

## Fields Added To Document

The following fields will be added to the document for each classification match. If the document has any sub-documents which caused a classification to match the fields will be applied to the root level document and also the sub-document(s) that caused a match.

* `CAF_CLASSIFICATION_ID`: - The IDs of any classifications the document matched.
  e.g. 
  ```
    CAF_CLASSIFICATION_ID: [1, 2]
  ```
  
* `CAF_CLASSIFICATION_NAME`: - The names of any classifications the document matched.
  e.g. 
  ```
    CAF_CLASSIFICATION_NAME: ["Travel Document", "Meeting Notification"]
  ```

*   `POLICY_MATCHED_COLLECTION` : - The IDs of any collections the document matched. This is a deprecated field that is likely to be removed in future releases.
  e.g.
  ```
    POLICY\_MATCHED\_COLLECTION:[1, 4, 5]
  ```
 
* `POLICY_MATCHED_POLICYID`: - The IDs of any policies executed on collections the document matched. This is a deprecated field that is likely to be removed in future releases, CAF_CLASSIFICATION_ID should be used instead.
  e.g.
  ```
    POLICY_MATCHED_POLICYID: [1, 2]
  ```

* `POLICY_MATCHED_POLICYNAME`: The names of any policies executed on collections the document matched. This is a deprecated field that is likely to be removed in future releases, CAF_CLASSIFICATION_NAME should be used instead.
  e.g.
  ```
    POLICY_MATCHED_POLICYNAME: ['Travel Document', 'Meeting Notification']
  ```
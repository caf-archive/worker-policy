# Classification Worker Converter

The Classification Worker Converter will add the IDs of the collections and conditions the document matched from the Collection Sequence it was classified against.
It will then return the document to Policy Worker for further evaluation. 

This converter will be invoked if the response message uses the Task Classifier of 'PolicyClassificationWorker' or 'PolicyClassificationWorkerElasticsearch'

## Fields Added To Document

*   `POLICY_MATCHED_COLLECTION` : - The IDs of any collections the document matched  
 e.g. POLICY\_MATCHED\_COLLECTION:[1, 4, 5]
 
*   `POLICY_MATCHED_CONDITION_$CollectionId`: - The ids of any conditions the document matched,  where $CollectionId is the Id of the collection the condition was matched on.   
e.g. POLICY\_MATCHED\_CONDITION\_1:[1, 3]

* `POLICY_MATCHED_POLICYID`: - The IDs of any policies executed on collections the document matched.
  e.g. POLICY_MATCHED_POLICYID: [1, 2]

* `POLICY_MATCHED_POLICYNAME`: The names of any policies executed on collections the document matched.
  e.g. POLICY_MATCHED_POLICYNAME: ['Travel Document', 'Meeting Notification']
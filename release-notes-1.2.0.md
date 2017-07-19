#### Version Number
${version-number}

#### New Features
- Updated to use priority queues
Policy work has been updated to support the use of RabbitMQ priority queues, this was done to aid in the increase of throughput through the entire workflow.

[CAF-3112](https://jira.autonomy.com/browse/CAF-3112): Composite Document worker handler added.
A composite document worker handler has been added to policy worker to allow for the passing of a document to the workflow with its subdocuments on the document object as a collection of subdocuments.

[CAF-3114](https://jira.autonomy.com/browse/CAF-3114): Composite Document worker converter added.
A composite document worker converter has been added to policy worker to allow for response messages from a document worker availing of the new composite document functionality added to have its change log processed and any updates to the main document or any of its subdocuments to be carried out. 


#### Known Issues

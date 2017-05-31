## Data Processing Worker

Data Processing Worker should be used to conditionally route documents to other Workers for processing and enrichment. 
To facilitate this, Data Processing Worker will accept requests to process a document against a Workflow, where the Workflow to be enacted is identified by a Workflow Identifier or by the corresponding series of Sequence Identifiers. 

For a specific Sequence, Data Processing Worker will evaluate the document to determine which Policies are relevant to the document given the Collections matched within the Sequence.  Once the relevant Policies have been determined, the Policies will be invoked in turn.  In a typical use case, it is most likely that one relevant Policy will be identified.
 
Once the document has been evaluated against one Sequence in the Workflow, control will pass to the next Sequence in the Workflow.

A good use case of Data Processing Worker is Document Ingestion.  For example, route documents to Extraction, conditionally route to OCR, Speech… depending on customer, data source, file type …; Classify for sensitive data, remove Disclaimers … and if this is not a C-level document …, present Vertica etc….

The format of the task data and task response for this worker can be seen [here](../../worker-policy-shared).

Note that the context of the response message from this worker will include the response status of all Workers the document was sent by the workflow to. For example, `OCR_STATUS`, `KV_CODE` etc.

### Error Handling

Invalidly formatted tasks cause InvalidTaskExceptions to be thrown by the Data Processing Worker and handled by the worker framework.

Transitory errors (such as an inability to connect to the database) thrown by the core-policy API result in a Task being rejected via throwing a TaskRejectedException which is handled by the worker. This results in a re-queue of the message allowing it to be retried with the assumption the issue will be resolved after a delay.

Other exceptions result in a failure result being created and sent to the Data Processing Worker output queue.

###Logging

The log level that the worker outputs at can be controlled by passing a file 'policy.yaml' specified as shown in the example below, on the path '/mnt/mesos/sandbox/';

```
logging:
  level: INFO
  loggers:
    com.hpe.caf: DEBUG
```

###Health Check
By default, Data Processing Worker implements no further health check beyond the basic one supplied by the CAF Framework.

If Data Processing Worker is configured to use Elasticsearch, the Policy Worker health check will attempt to connect to its local Elasticsearch.

Transitory errors such as a Database connection failure will not be resolved by relaunching the Worker and so are ignored when health checking. 
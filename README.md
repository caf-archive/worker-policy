# worker-policy

The worker-policy library enables set up and configuration of workflows for orchestration of document datasets between CAF data processing microservices. By using handlers and converters the worker can send task messages to other CAF workers and apply their results as metadata on a document. 

## Workers

[Data Processing Worker](./worker-data-processing-container)
* A worker that uses the worker-policy library to build up metadata for a document by sending and then collating results from other CAF workers based on the document matching defined criteria in a workflow.

## Feature Testing

Test cases for a worker-policy worker implementation (e.g. Data Processing Worker) are defined [here](testcases)

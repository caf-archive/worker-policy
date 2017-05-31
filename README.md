# worker-policy

The worker-policy library enables set up and configuration of workflows for orchestration of document datasets between CAF data processing microservices. By using handlers and converters the worker can send task messages to other CAF workers and apply their results as metadata on a document. 

## Workers

This repository contains two workers that make use of the worker-policy library to achieve goals in data processing.

[Classification Worker](./worker-classification-container)
* A worker that uses the worker-policy library to classify documents into collections based on defined criteria.

[Data Processing Worker](./worker-data-processing-container)
* A worker that uses the worker-policy library to build up metadata for a document by sending and then collating results from other CAF workers based on the document matching defined criteria in a workflow.

## Feature Testing

Test cases for a worker-policy worker implementation (e.g. Data Processing Worker) and specific tests for the Classification Worker are defined [here](testcases)

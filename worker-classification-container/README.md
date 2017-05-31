# Classification Worker Container

## Summary

A pre-packaged policy worker Docker image intended for use in evaluating which defined collections a document belongs to. This image uses a File System based data store and includes an internal Elasticsearch instance which is started on container launch and used by the Policy Worker.

### Environment Variable Configuration

#### Container Configuration

The container environment supports the following environment variables options;

* `POLICY_ELASTICSEARCH_DISABLED` - (Optional) Indicates whether the Elasticsearch instance in the container should be started. Defaults to false causing the instance to be started by default.
* `POLICY_ELASTICSEARCH_VERIFY_ATTEMPTS` - (Optional) When starting Elasticsearch this is the number of times to check if Elasticsearch has started during launch of the container. If Elasticsearch has not started within the specified number of retries then the container will exit.
* `POLICY_LOGGING_FILE` - (Optional) Path to a logging configuration file that should be used with the worker.

Configuration options for the Core Policy engine used by the worker can be found [here](https://github.com/CAFDataProcessing/policy-server).

Configuration options for the Elasticsearch instance in the container can be found [here](https://github.com/CAFDataProcessing/policy-elasticsearch-container).

#### Worker Configuration

The worker container supports reading its CAF worker configuration solely from environment variables. To enable this mode do not pass the `CAF_APPNAME` and `CAF_CONFIG_PATH` environment variables to the worker. This will cause it to use the default configuration files embedded in the container which check for environment variables. A listing of the RabbitMQ and Storage properties is available [here](https://github.com/WorkerFramework/worker-framework/tree/v1.7.0/worker-default-configs).

The Classification Worker specific configuration that can be controlled through the default configuration file is described below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| registerHandlers  |  `CAF_CLASSIFICATION_WORKER_REGISTER_HANDLERS`                              | true  |
| resultQueue   |  `CAF_WORKER_OUTPUT_QUEUE`                                                      | worker-out  |
|              |   `CAF_WORKER_BASE_QUEUE_NAME` with '-out' appended to the value if present     |             |
|              |  `CAF_WORKER_NAME` with '-out' appended to the value if present                 |             |
|  workerIdentifier    |  `CAF_CLASSIFICATION_WORKER_WORKER_IDENTIFIER`                 |    PolicyWorker         |
|  workerThreads   |   `CAF_CLASSIFICATION_WORKER_WORKER_THREADS`                                         |   1       |

### API

The task message and task result for the worker are described [here](./docs/Classification Worker API.md).
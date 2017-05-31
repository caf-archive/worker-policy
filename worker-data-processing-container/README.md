# worker-data-processing

## Summary

A policy worker Docker container that includes the worker-policy library and also a set of handlers and converters for use in data processing. This container has Elasticsearch included but it is not enabled by default.
If Elasticsearch is required e.g. for evaluation of Text and Lexicon conditions, the environment variable `POLICY_ELASTICSEARCH_DISABLED` should be set to false on container start.
This is a filesystem storage based worker, utilizing the worker-store-fs library. Access to a Hibernate compatible database, pre-configured with Policy Database Schema is required on startup.

### Environment Variable Configuration

#### Container Configuration

The container environment supports the following environment variables options;

* `POLICY_ELASTICSEARCH_DISABLED` - (Optional) Indicates whether the Elasticsearch instance in the container should be started. Defaults to true causing the instance to not be started.
* `POLICY_ELASTICSEARCH_VERIFY_ATTEMPTS` - (Optional) When starting Elasticsearch this is the number of times to check if Elasticsearch has started during launch of the container. If Elasticsearch has not started within the specified number of retries then the container will exit.
* `POLICY_LOGGING_FILE` - (Optional) Path to a logging configuration file that should be used with the worker.

Configuration options for the Core Policy engine used by the worker can be found [here](https://github.com/CAFDataProcessing/policy-server).

Configuration options for the Elasticsearch instance in the container can be found [here](https://github.com/CAFDataProcessing/policy-elasticsearch-container).

#### Worker Configuration

The worker container supports reading its CAF worker configuration solely from environment variables. To enable this mode do not pass the `CAF_APPNAME` and `CAF_CONFIG_PATH` environment variables to the worker. This will cause it to use the default configuration files embedded in the container which check for environment variables. A listing of the RabbitMQ and Storage properties is available [here](https://github.com/WorkerFramework/worker-framework/tree/v1.7.0/worker-default-configs).

The Data Processing Worker specific configuration that can be controlled through the default configuration file is described below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| registerHandlers      |  `CAF_DATA_PROCESSING_WORKER_REGISTER_HANDLERS`                              | false  |
| resultQueue           |  `CAF_DATA_PROCESSING_WORKER_OUTPUT_QUEUE`          | worker-out  |
|                       |   `CAF_WORKER_OUTPUT_QUEUE`    |             |
|                       |   `CAF_WORKER_BASE_QUEUE_NAME` with '-out' appended to the value if present     |             |
|                       |  `CAF_WORKER_NAME` with '-out' appended to the value if present                 |             |
|  workerIdentifier     |  `CAF_DATA_PROCESSING_WORKER_WORKER_IDENTIFIER`                 |    PolicyWorker         |
|  workerThreads        |   `CAF_DATA_PROCESSING_WORKER_WORKER_THREADS`                                         |   1       |

### API

The task message and task result for the worker are described [here](./docs/Data Processing Worker API.md).

## Handlers/Converters

The following handlers and converters are included in the image.

* Binary Hash Worker
  * [Handler](../handlers-converters/handlers/binary-hash)
  * [Converter](../handlers-converters/converters/binary-hash)
  
* Boilerplate Worker
  * [Handler](../handlers-converters/handlers/boilerplate)
  * [Converter](../handlers-converters/converters/boilerplate)
  
* Document Worker
  * [Handler](../handlers-converters/handlers/document-worker)
  * [Converter](../handlers-converters/converters/document-worker)
  
* Elasticsearch Classification Worker
  * [Handler](../handlers-converters/handlers/elasticsearch-classification)
  * [Converter](../handlers-converters/converters/classification)
  
* Field Mapping Handler
  * [Handler](../handlers-converters/handlers/field-mapping)
    
* Markup Worker
  * [Handler](../handlers-converters/handlers/markup)
  * [Converter](../handlers-converters/converters/markup)
    
* Queue Output
  * [Handler](../handlers-converters/handlers/generic-queue)
    
* Stop
  * [Handler](../handlers-converters/handlers/stop)
  
### Adding additional Handlers/Converters

As with the other policy worker images, additional handlers and converters may be passed to a running version of the image by mounting the handler/converter jar files to a folder on the classpath of the worker. The following paths may be used inside the container;

* /opt/PolicyWorker/lib/
* /opt/PolicyWorker/Handlers/
* /mnt/mesos/sandbox/
* /mnt/mesos/sandbox/handlers/
* /mnt/mesos/sandbox/converters/
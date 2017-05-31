## Classification Worker Task Input and Output

The Classification Worker will take a message off its input queue and then attempt to classify the provided document in the task data of the message against predefined workflow or collection sequence that is referenced by the task data.

See the Policy Server documentation for more detailed information on classifications [here](https://github.com/CAFDataProcessing/policy-server).

The format of the task data and task response for this worker can be seen [here](../../worker-policy-shared).

### Error Handling

Invalidly formatted tasks cause InvalidTaskExceptions to be thrown by the Classification Worker and handled by the worker framework.

Transitory errors (such as an inability to connect to the database) thrown by the core-policy API result in a Task being rejected via throwing a TaskRejectedException which is handled by the worker. This results in a re-queue of the message allowing it to be retried with the assumption the issue will be resolved after a delay.

Other exceptions result in a failure result being created and sent to the Classification Worker output queue.

### Logging

The log level that the worker outputs at can be controlled by passing a file 'policy.yaml' specified as shown in the example below, on the path '/mnt/mesos/sandbox/';

```
logging:
  level: INFO
  loggers:
    com.hpe.caf: DEBUG
```

### Health Check
In addition to the CAF framework health check, the Classification Worker will attempt to connect to its local Elasticsearch instance. 

Transitory errors such as a database connection failure will not be resolved by relaunching the Worker and so are ignored when health checking. 
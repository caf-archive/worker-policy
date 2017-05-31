# Elastic Search Classification Worker Handler

This handler is intended to send tasks to the Elastic Search implementation of the Classification Worker. Policies created using this type define a workflow or collection sequence that a douocment should be evaluated against. The rseult of this evaluation showing which collections, conditions and policies were matched will be returned by the classification worker.

### Properties

These properties should be specified as environment variables.

#### elasticsearchclassificationworkerhandler.taskqueue

The queue to send the policy tasks to. An Elastic Search Classification Worker should be listening on the specified queue.

#### elasticsearchclassificationworkerhandler.diagnosticstaskqueue

A queue that a diagnostics version of the Elastic Search Classification Worker should be listening on.

#### Example Policy Definitions

#### Policy Schema

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>queueName</td>
        <td>String</td>
        <td>Optional. An Elastic Search Classification Policy Handler exists to execute Policies of type 'Elastic Search Classification Policy Type'.  Typically the target queue name is provided via Elastic Search Classification Policy Handler configuration.  If there is a need to establish multiple instances of an Elastic Search Classification Policy to target different queues, then queueName should be specified as part of the Elastic Search Classification Policy. An example usage of this feature could be to route larger files to a different queue.</td>
    </tr>
    <tr>
        <td>diagnosticsQueueName</td>
        <td>String</td>
        <td>Optional. An example usage of this feature could be to route failed files to different Classification queue for diagnostics.</td>
    </tr>
    <tr>
        <td>workflowId</td>
        <td>Integer</td>
        <td>Required if classificationSequenceId is not specified. Identifies the Workflow to be used when classifying. If this and classificationSequenceId are both specified then workflowId will take precedence.</td>
    </tr>
    <tr>
        <td>classificationSequenceId</td>
        <td>Integer</td>
        <td>Required if workflowId is not specified. Identifies the Collection Sequence to be used when classifying.</td>
    </tr>
</table>

## Example Policy Definitions

##### Classification

To send to a sequence with id 1

```
{
  "classificationSequenceId": 1
}
```

##### Workflow ID

To send to a Workflow with id 1

```
{
  "workflowId": 1
}
```

#### Queue

To specify a queue for this Policy to use rather than the environment variable or default.

```
{
    "queueName": "other-classifications",
    "workflowId": 1
}
```


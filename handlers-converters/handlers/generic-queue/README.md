# Generic Queue Handler

The Generic Queue handler will serialize the current document in the workflow into a new task message and sends that message to the specified queue. 

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
        <td>(Optional) The name of the queue to publish the document to.  A default queue name can be specified for generic queue policies via an environment property called `genericqueuehandler.taskqueue`. </td>
    </tr>
    <tr>
        <td>messageType</td>
        <td>String</td>
        <td> Optional The task classifier</td>
    </tr>
    <tr>
        <td>apiVersion</td>
        <td>Integer</td>
        <td> Optional The version of the target queue consumer</td>
    </tr>
</table>


#### Example Policy Definition

##### Send document to a queue

Publishes the document to a specified queue.

<pre>
{
    "queueName": "example-queue-name",
    "messageType": "MyMessage"
    "apiVersion": 1
}
</pre>


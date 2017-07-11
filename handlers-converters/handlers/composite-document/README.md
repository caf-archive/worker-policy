# Document Worker Handler

## Policy Schema

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>workerName</td>
        <td>String</td>
        <td>
            <p>The name of the worker.</p>
            <p>Some configuration details, such as the default queue names, will be looked up based on the name of the worker.</p>
            <p>For example, if the 'workerName' is 'mylookupworker', then the environment variable 'mylookupworker.taskqueue' should be
               configured to store the default queue name for the worker.</p>
        </td>
    </tr>
    <tr>
        <td>queueName</td>
        <td>String</td>
        <td>Optional. Typically the target queue name is provided via configuration.
            This field can be used as a means to override the queue name specified in the configuration.</td>
    </tr>
    <tr>
        <td>diagnosticsQueueName</td>
        <td>String</td>
        <td>Optional. Allows a second 'diagnostic' queue to be specified for the worker. This will be used if the worker fails.</td>
    </tr>
    <tr>
        <td>fields</td>
        <td>Set&lt;String&gt;</td>
        <td>Optional. Fields to be sent to the worker. If not specified, all fields will be sent. Matching for multiple fields can be achieved with '*' wildcard.</td>
    </tr>
    <tr>
        <td>customData</td>
        <td>Map&lt;String,&nbsp;Object&gt;</td>
        <td>
			<p>Optional. Used to send additional worker-specific data to the worker.</p>
			<p>The datatypes in which Object can be defined as and be recognised by the DocumentWorker handler is limited to String and Map. All Key-Value pairs must be of type String.</p>
			<p>Any data within customData is extracted and placed into a DocumentWorkerTask object and passed to the worker. </p>
			<p>How the data is extracted depends on how the information is stored in customData. (see examples below).</p>
		</td>
    </tr>
</table>


## Example Policy Definitions

#### Sending to a specified queue

<pre>
{
    "workerName": "MyLookupWorker",
    "queueName": "MyLookupWorkerInputQueue",
}
</pre>

<br></br>

<br></br>

#### Sending optional data to the queue specified in configuration


##### Individual Key-Value pair within customData

Key-Value pairs are extracted as any other Key-Value pair. The handler will extract the key and value and pass it through to the worker.  

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData": 
   {
      "someStringKey": "someStringValue"
   }
}
</pre>

##### Map within customData

A Key-Value pair (or Map) within customData can have another Key-Value pair nested in its value. The handler drills down into the value of the top level Map and extracts the value of the nested Map. In order for the handler to find the value, **the nested Map must have a "source" key**.

The code-blocks below show how you can pass the `dataStorePartialReference` and `projectId` fields into a Document Worker. The handler looks for `"source"` and extracts the related value. What is passed through to the worker will depend on what the source value is. For example, if `value == "dataStorePartialReference"`, then the handler will call a method to request the OutputPartialReference from `taskData`. The reference will be passed through to the worker. Similar functionality is performed when looking for the Tenant ID.

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData": 
   {
      "outputPartialReference": { "source": "dataStorePartialReference" }
   }
}
</pre>

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData": 
   {
      "tenantId": { "source": "projectId" }
   }
}
</pre>


It is also possible to supply a JSON object as a value within the nested map. In this case, **the `"source"` value must be `"inlineJson"` and the nested map must also include a `"data"` key, the value of which is the JSON object to be supplied**. This allows objects to be supplied without encoding in the policy definition, for example:

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData":
  {  
    "someSetting": {  
      "source": "inlineJson",
      "data": {  
        "object": {  
          "key":" value",
          "array": [  
            {  
              "null_value": null
            },
            {  
              "boolean": true
            },
            {  
              "integer": 1
            }
          ]
        }
      }
    }
  }
}
</pre>

The handler takes care of JSON-encoding the object. The above example is exactly equivalent to the following, which would make for a less readable policy definition:

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData":
  {
    "someSetting": "{\"object\":{\"key\":\"value\",\"array\":[{\"null_value\":null},{\"boolean\":true},{\"integer\":1}]}}"
  }
}
</pre>


##### Variations within customData

You can pass any amount of optional information within customData. The datatypes can also vary between Maps and individual Key-Value pairs, separated by a comma. The handler will traverse the map for each value and process accordingly.

<pre>
{
  "workerName": "DirectoryLookupWorker",
  "customData": 
   {
      "someStringKey": "someStringValue",
      "outputPartialReference": { "source": "dataStorePartialReference" },
      "tenantId": { "source": "projectId" }
   }
}
</pre>
 
# Binary Hash Worker Handler

## Policy Schema

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>queueName</td>
        <td>String</td>
        <td>Optional. A BinaryHash Policy Handler exists to execute Policies of type 'BinaryHash Policy Type'.  Typically the target queue name is provided via BinaryHash Policy Handler configuration.  If there is a need to establish multiple instances of a BinaryHash Policy to target different queues, then queueName should be specified as part of the BinaryHash Policy Definition, otherwise exclude this attribute.  An example usage of this feature could be to route larger files to a different queue.</td>
    </tr>
</table>

## Example Policy Definitions

#### Sending to a different queue name

<pre>
{

  "queueName" : "MyExampleQueue"
}
</pre>

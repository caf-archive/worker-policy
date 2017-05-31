# Boilerplate Worker Handler

## Policy Schema
Boilerplate Policy 

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>queueName</td>
        <td>String</td>
        <td>Optional. A Boilerplate Handler exists to execute Policies of type 'Boilerplate Policy Type'. Typically the target queue name is provided via Boilerplate Policy Handler configuration. If there is a need to establish multiple instances of a Boilerplate Policy to target different queues, then queueName should be specified as part of the Policy. An example usage of this feature could be to route larger files to a different queue.</td>
    </tr>
    <tr>
        <td>diagnosticsQueueName</td>
        <td>String</td>
        <td>Optional. An example usage of this feature could be to route failed files to different Boilerplate queue for diagnostics.</td>
    </tr>
    <tr>
        <td>redactionType</td>
        <td>Enum</td>
        <td>Optional. Indicates action to perform on values that match boilerplate expression. Defaults to 'DoNothing'.</td>
    </tr>
    <tr>
        <td>fields</td>
        <td>List&lt;String&gt;</td>
        <td>Optional. A list of fields (title, content etc.) to perform boilerplate checking against. If no fields are provided then checking will be performed Boilerplate Handler default fields.</td>
    </tr>
    <tr>
        <td>tagId</td>
        <td>Integer</td>
        <td>ID of a Tag. The Policy Item will be evaluated against the Boilerplate Expressions with this Tag.</td>
    </tr>
    <tr>
        <td>expressionIds</td>
        <td>List@lt;Integer@gt;</td>
        <td>Boilerplate Expression IDs to evaluate against. Either this or 'tagId' must be set.</td>
    </tr>
    <tr>
        <td>returnMatches</td>
        <td>Boolean</td>
        <td>Optional. Indicates whether the detail of the boilerplate expression matches should be returned such as the fragments matched. Defaults to true.</td>
    </tr>
    <tr>
        <td>emailSegregationRules</td>
        <td>EmailSegregationRules</td>
        <td>Email Segregation Rules containing required expressions for segregating primary, secondary and tertiary email content into their own respective fields. Either this, 'expressionIds' OR 'tagId' must be set.</td>
    </tr>
    <tr>
        <td>emailSignatureDetection</td>
        <td>EmailSignatureDetection</td>
        <td>Email Signature Detection containing optional field name and optional email sender properties. Either this, 'emailSegregationRules', 'expressionIds' OR 'tagId' must be set.</td>
    </tr>
</table>

Redaction Type

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>DO_NOTHING</td>
        <td>Does not perform any replacement. Will just return any matches</td>
    </tr>
    <tr>
        <td>REMOVE</td>
        <td>Removes any matches from the data.</td>
    </tr>
    <tr>
        <td>REPLACE</td>
        <td>Replaces any matches with the specified replacement text.</td>
    </tr>
</table>

Email Segregation Rules

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>primaryExpression</td>
        <td>Required. Range of emails that will be extracted as the primary email content, e.g "0" will extract out the original email in the chain.</td>
    </tr>
    <tr>
        <td>secondaryExpression</td>
        <td>Required. Range of email that will be extracted as the secondary email content, e.g "1..3" will extract out the first to third email in the chain.</td>
    </tr>
    <tr>
        <td>tertiaryExpression</td>
        <td>Required. Range of email that will be extracted as the tertiary email content, e.g "LAST" will extract out the latest email in the chain.</td>
    </tr>
    <tr>
        <td>primaryFieldName</td>
        <td>Optional. Extracted primary email content will be assigned to this custom field (Default field name: BOILERPLATE_PRIMARY_CONTENT).</td>
    </tr>
    <tr>
        <td>secondaryFieldName</td>
        <td>Optional. Extracted secondary email content will be assigned to this custom field (Default field name: BOILERPLATE_SECONDARY_CONTENT).</td>
    </tr>
    <tr>
        <td>tertiaryFieldName</td>
        <td>Optional. Extracted tertiary email content will be assigned to this custom field (Default field name: BOILERPLATE_TERTIARY_CONTENT)</td>
    </tr>
</table>

Email Signature Detection

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>extractedEmailSignatureFieldName</td>
        <td>Optional. Extracted email signatures will be assigned to this custom field (Default field name: BOILERPLATE_EXTRACTED_SIGNATURES).</td>
    </tr>
    <tr>
        <td>sender</td>
        <td>Optional. Can be specified for edge case removal of a sender's email signature.</td>
    </tr>
</table>

## Example Policy Definitions

#### Expression Ids
Passing multiple boilerplate expression IDs for document to be compared against. RedactionType will default to DoNothing and the default fields defined by the handler will be sent to the Boilerplate Worker (assuming they are on the document).
<pre>
{
  "expressionIds": [1,2,3,4,5]
}
</pre>

#### TagId
A queue name to send the task for the boilerplate worker is specified here along with the tagId to use, the fields on the document to send and the redaction type to use.

<pre>
{
  "queueName": "test-queue",
  "tagId": 1,
  "fields": ["CONTENT", "PROP_BODY"],
  "redactionType": "REMOVE"
}
</pre>

#### Email Segregation Rules

In this example required primary, secondary and tertiary expressions are specified here along with the optional custom names of the fields that each key content type will be assigned to. An optional queue name to send the task for the boilerplate worker is also specified.

<pre>
{
  "emailSegregationRules": {
    "primaryExpression": "0",
    "secondaryExpression": "1..3",
    "tertiaryExpression": "LAST",
    "primaryFieldName": "EXTRACTED_PRIMARY_EMAIL_FIELD",
    "secondaryFieldName": "EXTRACTED_SECONDARY_EMAIL_FIELD",
    "tertiaryFieldName": "EXTRACTED_TERTIARY_EMAIL_FIELD"
  },
  "queueName": "test-queue"
}
</pre>


#### Email Signature Detection

In this example the optional sender of the email and the optional custom name of the field that signatures will be extracted to are specified. An optional queue name to send the task for the boilerplate worker is also specified.

<pre>
{
  "emailSignatureDetection": {
    "extractedEmailSignaturesFieldName": "EMAIL_EXTRACTED_SIGNATURES_FIELD",
    "sender": "an.email.sender@hpe.com"
  },
  "queueName": "test-queue"
}
</pre>

In this example shows specifying email signature detection mode without any optional properties. An optional queue name to send the task for the boilerplate worker is also specified.

<pre>
{
  "emailSignatureDetection": {},
  "queueName": "test-queue"
}
</pre>


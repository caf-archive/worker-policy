# Markup Worker Handler

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
        <td>Optional. An Markup Policy Handler exists to execute Policies of type 'Markup Policy Type'.  Typically the target queue name is provided via Markup Policy Handler configuration.  If there is a need to establish multiple instances of a Markup Policy to target different queues, then queueName should be specified as part of the Markup Policy Definition, otherwise exclude this attribute.  An example usage of this feature could be to route larger files to a different queue.</td>
    </tr>
    <tr>
        <td>diagnosticsQueueName</td>
        <td>String</td>
        <td>Optional. An example usage of this feature could be to route failed files to a different Markup queue for diagnostics.</td>
    </tr>
    <tr>
        <td>fields</td>
        <td>List&lt;String&gt;</td>
        <td>Optional. Fields that will be evaluated during markup. Matching for multiple fields can be achieved with '*' wildcard.</td>
    </tr>    
    <tr>
        <td>hashConfiguration</td>
        <td>List&lt;HashConfiguration&gt;</td>
        <td>Optional. The hash configuration specifies the configuration for which fields to be included in the hash, the type of normalization to be performed per field and the hash function to be carried out on the list of fields. Multiple hash configurations can be specified.</td>
    </tr>
    <tr>
        <td>outputFields</td>
        <td>List&lt;OutputField&gt;</td>
        <td>Optional. The list of the output fields to be returned from the XML document.</td>
    </tr>
    <tr>
        <td>isEmail</td>
        <td>boolean</td>
        <td>Optional. Whether the message being passed is an e-mail.</td>
    </tr>
    
</table>

#### HashConfiguration

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>Name of the hash to be included.</td>
    </tr>
    <tr>
        <td>scope</td>
        <td>enum</td>
        <td>Enumeration determining where the hash should be performed.</td>
    </tr>
    <tr>
        <td>fields</td>
        <td>List&lt;Field&gt;</td>
        <td>List of fields to be included in the hash.</td>
    </tr>
    <tr>
        <td>hashFunctions</td>
        <td>List&lt;HashFunction&gt;</td>
        <td>A list of hash functions to use.</td>
    </tr>
</table>

#### Field

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>Name of the field / element tag in xml. Matching for multiple fields can be achieved with '*' wildcard.</td>
    </tr>
    <tr>
        <td>normalizationType</td>
        <td>enum</td>
        <td>Type of field data normalization.</td>
    </tr>
</table>

#### HashFunction

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>HashFunction</td>
        <td>enum</td>
        <td>Enumeration determining the method of hashing to be performed on the selected tag elements.</td>
    </tr>
</table>

#### OutputField

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>field</td>
        <td>string</td>
        <td>Name of document field to be returned.</td>
    </tr>
    <tr>
        <td>xPathExpression</td>
        <td>string</td>
        <td>XPath expression needed to extract the field value from the XML document.</td>
    </tr>
</table>

## Example Policy Definitions

#### Specific fields for markup evaluation

You can limit the fields sent to the Markup Worker:

<pre>
{
    "fields": [
        "CONTENT",
        "internetmessageid",
        "conversationtopic",
        "caf-mail-conversation-index",
        "caf-mail-in-reply-to",
        "CHILD_INFO_*_HASH"
    ]
}
</pre>

#### Email specific hashes suitable for constructing a conversation tree

This configuration will generate hash digests which can be used to identity related e-mails, especially replied-to and forwarded e-mails:

<pre>
{
    "hashConfiguration": [{
        "name": "Normalized",
        "scope": "EMAIL_SPECIFIC",
        "fields": [
            { "name": "To",   "normalizationType": "NAME_ONLY" },
            { "name": "From", "normalizationType": "NAME_ONLY" },
            { "name": "Body", "normalizationType": "REMOVE_WHITESPACE_AND_LINKS" }
        ],
        "hashFunctions": [
            "XXHASH64"
        ]
    }]
}
</pre>

#### Sample fields to be returned from the XML document

By default the Markup Worker will place the entire xml document into a field named `MARKUPWORKER_XML`.  The `outputFields` configuration can be used to instead return just specific nodes from the document.

This configuration, when used in combination with the hash configuration above, returns fields which can be used to identity related e-mails, especially replied-to and forwarded e-mails:

<pre>
{
    "outputFields": [
        { "field": "SECTION_ID",              "xPathExpression": "/root/email[1]/hash/digest/@value" },
        { "field": "SECTION_SORT",            "xPathExpression": "/root/email[1]/headers/Sent/@dateUtc" },
        { "field": "PARENT_ID",               "xPathExpression": "/root/email[2]/hash/digest/@value" },
        { "field": "ROOT_ID",                 "xPathExpression": "/root/email[last()]/hash/digest/@value" },
        { "field": "MESSAGE_ID",              "xPathExpression": "/root/CAF_MAIL_MESSAGE_ID/text()" },
        { "field": "IN_REPLY_TO",             "xPathExpression": "/root/CAF_MAIL_IN_REPLY_TO/text()" },
        { "field": "CONVERSATION_TOPIC",      "xPathExpression": "/root/CAF_MAIL_CONVERSATION_TOPIC/text()" },
        { "field": "CONVERSATION_INDEX_JSON", "xPathExpression": "/root/CAF_MAIL_CONVERSATION_INDEX_PARSED/text()" }
    ]
}
</pre>

#### Sending to a different queue name

<pre>
{
    "queueName" : "MyExampleQueue"
}
</pre>

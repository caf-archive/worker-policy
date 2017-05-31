# worker-policy-shared

worker-policy-shared is a shared library used within the worker-policy umbrella of components to represent items such as the Document object.

## Task Data

This is the format of the Task Data object that forms a request to worker-policy.

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
	<tr>
        <td> projectId </td>
        <td> String </td>
        <td>The unique Project Id for the user/tenant.</td>
    </tr>
    <tr>
        <td> collectionSequences</td>
        <td> List&lt;String&gt;  </td>
        <td> A list of one or more Collection Sequence Ids. </td>
    </tr>
    <tr>
        <td> document </td>
        <td> Document</td>
        <td> The Document to be evaluated. </td>
    </tr>
	<tr>
		<td>executePolicyOnClassifiedDocument</td>
		<td>boolean</td>
		<td>Specifies whether to execute the resolved Policies on the document.  This should be true.</td>
	</tr>
	<tr>
        <td> policiesToExecute</td>
        <td> List&lt;Long&gt;  </td>
        <td> A filtered list of Policy ids to execute.  This should be ignored for this worker.</td>
    </tr>
	<tr>
        <td> outputPartialReference</td>
        <td> String </td>
        <td> The partial reference for the data store</td>
    </tr>
	<tr>
		<td> workflowId </td>
		<td> String </td>
		<td> The Workflow Id of the Workflow. This will override collectionSequences if both are provided. </td>
	</tr>
</table>

### Document

This is the format of the Document Object that is part of the Task Data Object.

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
	<tr>
        <td> reference </td>
        <td> String </td>
        <td> An identifier for the document. </td>
    </tr>
    <tr>
        <td> metadata </td>
        <td> CaseInsensitiveMultimap&lt;String&gt;  </td>
        <td> A String to String multimap of the documents meta data. </td>
    </tr>
    <tr>
        <td> metadataReferences </td>
        <td> HashMultimap&lt;String, ReferencedData&gt; </td>
        <td> A String to ReferencedData multimap that allows meta data to be references to data in Storage. </td>
    </tr>
    <tr>
        <td> documents </td>
        <td> Collection&lt;Document&gt; </td>
        <td> A collection of any child documents. </td>
    </tr>
	<tr>
		<td>policyDataProcessingRecord</td>
		<td>DocumentProcessingRecord</td>
		<td>A record of changes made to a document as it is processed.</td>
	</tr>
</table>


## Task Response

This is the format of the Task Response object that a worker-policy worker will return after the document has finished flowing through the system (unless a different format of message is constructed using a handler).

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td> classifiedDocuments</td>
        <td> List&lt;ClassifyDocumentResult&gt;  </td>
        <td> A list of ClassifyDocumentResults. This includes the list of matched collections and conditions and the list of resolved policies. </td>
    </tr>
</table>

### ClassifyDocumentResult

This is the format of the ClassifyDocumentResults object that is part of the TaskResponse.

<table>
    <tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td> reference </td>
        <td> String </td>
        <td> The Document Reference this result applies to. </td>
    </tr>
	<tr>
        <td> unevaluatedConditions </td>	
        <td> Collection&lt;UnevaluatedCondition&gt; </td>
        <td> A collection of Conditions that were not evaluated.</td>
    </tr>
	<tr>
        <td> matchedCollections </td>
        <td> Collection&lt;MatchedCollection&gt;</td>
        <td> A collection MatchedCollections. </td>
    </tr>
	<tr>
        <td> collectionIdAssignedByDefault </td>
        <td> Long </td>
        <td> The Id of the default Collection on the CollectionSequence evaluated against. </td>
    </tr>
	<tr>
        <td> incompleteCollections </td>
        <td> Collection&lt;Long&gt; </td>
        <td> A collection of Ids of Collections the document did not evaluate against due to unevaluated Conditions.</td>
    </tr>
	<tr>
        <td> resolvedPolicies </td>
        <td> Collection&lt;Long&gt; </td>
        <td> A collection of Ids of the Policies that were applied to the document. </td>
    </tr>
</table>

#### UnevaluatedCondition
Object that represents Conditions that couldn't be evaluated.
<table>
	<tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
	<tr>
        <td> reason </td>
        <td> Enum </td>
        <td> Reason why the Condition wasn't evaluated. Can be either MISSING_FIELD (Field did not exist on the document) or MISSING_SERVICE (An external service required for the evaluation was not found).</td>
    </tr>
	<tr>
		<td>id</td>
		<td>Long</td>
		<td>The Id of the unevaluated Condition.</td>
	</tr>
	<tr>
		<td>name</td>
		<td>String</td>
		<td>The name of the unevaluated Condition.</td>
	</tr>
	<tr>
		<td>conditionType</td>
		<td>Enum</td>
		<td>The type of the unevaluated Condition.</td>
	</tr>
	<tr>
		<td>field</td>
		<td>String</td>
		<td>The field the unevaluated Condition applies to. If the condition does not apply to a field this will be null.</td>
	</tr>
</table>

#### MatchedCollection
Object that represents Collections the document matched.
<table>
	<tr>
        <td><b>Name</b></td>
        <td><b>Type</b></td>
        <td><b>Description</b></td>
    </tr>
	<tr>
		<td>id</td>
		<td>Long</td>
		<td>The Id of the matched Collection.</td>
	</tr>
	<tr>
		<td>name</td>
		<td>String</td>
		<td>The name of the matched Collection.</td>
	</tr>
	<tr>
        <td> matchedConditions </td>
        <td> Collection&lt;MatchedCondition&lt; </td>
        <td> A collection of Conditions the document matched.</td>
    </tr>
	<tr>
        <td> policies </td>
        <td> Collection&lt;Policy&gt; </td>
        <td> A collection of the Policies that were assigned to this Collection.</td>
    </tr>
</table>
# Markup Worker Converter

The Markup Worker Converter will add the fields returned by the Markup Worker onto the document metadata. The fields which are returned
are determined by the list of output fields specified in the Policy definition.

This converter will only be invoked if the response message uses the Task Classifier of "MarkupWorker"

## Fields added to the document

The fields that are added to the document are specified in the Policy configuration.

The following fields are added by default:

* `SECTION_ID`: - The hash value of the first email in the email chain.

* `SECTION_SORT`: - The sent time of the first email in the email chain.

* `PARENT_ID`: - The hash value of the second email in the email chain.

* `ROOT_ID`: - The hash value of the last email in the email chain.

* `MESSAGE_ID`: - The ID of the message.

* `CONVERSATION_TOPIC`: - The conversation topic of the email chain.

* `CONVERSATION_INDEX_JSON`: - The parsed details of the mail conversation.

* `IN_REPLY_TO`: - The recipient this email is in reply to.

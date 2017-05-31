# worker-policy

A CAF worker that supports comparing provided documents against a defined collection of criteria and reporting which collections match, along with updating metadata on the document as it is checked. Through [handlers and converters](../handlers-converters) this worker can also send tasks to other workers and apply their results as metadata on the document.
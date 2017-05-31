## CAF_9083 - Invalid Storage Reference sent to Classification Worker ##

Verify that a task sent to Classification worker with an invalid storage reference is placed in the rejected queue after 10 retries.

**Test Steps**

1. Set up system to perform Classification and send a task message to the worker that contains a invalid storage reference
2. Examine the output

**Test Data**

Plain text files

**Expected Result**

The output message is placed into the rejected queue after 10 retries

**JIRA Link** - [CAF-1388](https://jira.autonomy.com/browse/CAF-1388)





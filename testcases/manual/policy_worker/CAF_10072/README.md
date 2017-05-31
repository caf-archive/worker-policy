## CAF_10072 - Invalid Collection Sequence Id sent to Policy Worker ##

Verify that a task sent to Policy worker with an invalid Collection Sequence id is returned as an INVALID_TASK

**Test Steps**

1. Set up system to perform Policy and send a task message to the worker that contains a Collection Sequence id that does not exist or is otherwise invalid
2. Examine the output

**Test Data**

Plain text files

**Expected Result**

The output message is returned with a status of INVALID_TASK

**JIRA Link** - [CAF-1388](https://jira.autonomy.com/browse/CAF-1388)





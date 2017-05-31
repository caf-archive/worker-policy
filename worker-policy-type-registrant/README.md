# worker-policy-type-registrant

## Summary

This is an application that registers policy types in a policy database based on the handlers found on the application classpath. Any handlers that have policy types not currently present in the policy back-end will be added for use by any user of that policy system.

## Running Application

The jar file built by this project contains the necessary java libraries to run the application. An example run command is shown below;

```
java -cp "C:\Pol_Git\PolicyTypeRegistrant\target\*;C:\policy-handlers\handlers\*" com.github.cafdataprocessing.worker.policy.typeregistrant.PolicyRegistrant
```

Here we identify the starting point for the application and set the classpath to include the path to the application itself and another folder which contains the policy handler jar files.

## Configuration

Communication with the policy back-end can be configured either in either direct or web mode. The relevant properties that should be passed to the application as environment variables are detailed below;

### API Properties

* api.mode - configures the mode of the API. Should be set to either 'direct' if communicating directly with a database or 'web' if communicating with a web service instance of the policy API.

If running in 'direct' mode set the following property.

* api.direct.repository - specifies the type of repository to be used in direct mode. The default uses Hibernate. e.g. 'hibernate'

If running in 'web' mode set the following property.

* api.webservice.url - the URL of the policy web service to contact.

### Hibernate Properties

If running in 'direct' mode the folllowing properties should be specified.

* hibernate.connectionstring - details which database to connect to e.g. `jdbc:postgresql://localhost:5432/<dbname>?characterEncoding=UTF8&rewriteBatchedStatements=true"`  "dbname" will be substituted with the databasename property
* hibernate.databasename - specifies the name of the database
* hibernate.user - username to use when connecting to the database
* hibernate.password - password to use when connecting to database
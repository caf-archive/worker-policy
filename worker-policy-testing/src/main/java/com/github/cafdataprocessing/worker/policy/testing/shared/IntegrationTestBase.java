/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.github.cafdataprocessing.corepolicy.api.ApiProvider;
import com.github.cafdataprocessing.corepolicy.common.*;
import com.github.cafdataprocessing.corepolicy.common.dto.DocumentCollection;
import com.github.cafdataprocessing.corepolicy.common.dto.Lexicon;
import com.github.cafdataprocessing.corepolicy.common.dto.LexiconExpression;
import com.github.cafdataprocessing.corepolicy.common.dto.LexiconExpressionType;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.LexiconCondition;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Base class for use with policy worker integration tests.
 */
public abstract class IntegrationTestBase
{

    private final static Logger logger = LoggerFactory.getLogger(IntegrationTestBase.class);

    protected ClassificationApi classificationApi;

    private UserContext userContext;

    protected ApiProperties apiProperties;

    public static CorePolicyApplicationContext genericApplicationContext;

    public static CorePolicyApplicationContext getGenericApplicationContext()
    {
        return genericApplicationContext;
    }

    public UserContext getUserContext()
    {
        return userContext;
    }

    public IntegrationTestBase()
    {
        // in certain cases we now have to shutdown the main ApiApplicationInstance as once a profile has been activated
        // it doesn't remove itself, it has to be rebuilt.  As such a quick request here will ensure we have a new context.
        // if it has been removed.
        if (genericApplicationContext == null) {
            genericApplicationContext = new CorePolicyApplicationContext();
            genericApplicationContext.refresh();
        }

        apiProperties = genericApplicationContext.getBean(ApiProperties.class);

        try {
            // let our deployment type setup the usercontext correctly. depending on iod/web
            switch (apiProperties.getMode()) {
                case "direct":
                case "web": {
                    // set the real user context now!
                    userContext = genericApplicationContext.getBean(UserContext.class);
                    userContext.setProjectId(UUID.randomUUID().toString());
                    break;
                }
            }
        } catch (Exception ex) {
            getLogger().trace("Exception in IntegrationTestBase... ", ex);
            throw new RuntimeException(ex);
        }

        classificationApi = genericApplicationContext.getBean(ClassificationApi.class);
    }

    public static Logger getLogger()
    {
        return logger;
    }

    @Before
    public void before()
    {

    }

    @After
    public void after()
    {
        if (userContext == null || !apiProperties.isInApiMode(ApiProperties.ApiMode.direct) || !apiProperties.isInRepository(ApiProperties.ApiDirectRepository.mysql)) {
            return;
        }
        cleanupDatabase();
    }

    protected void cleanupDatabase()
    {
        String projectId = userContext.getProjectId();
        try {
            try (Connection connection = getConnectionToClearDown()) {
                if (connection != null) {
                    CallableStatement callableStatement = connection.prepareCall("CALL sp_clear_down_tables_v3_2(?)");
                    callableStatement.setString("projectId", projectId);
                    callableStatement.execute();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getProjectId()
    {
        return userContext != null ? userContext.getProjectId() : null;
    }

    protected abstract Connection getConnectionToClearDown();

    protected ClassificationApi getClassificationApi()
    {
        return classificationApi;
    }

    public static boolean isStoreFSEnabled()
    {
        return Boolean.parseBoolean(getConfigurationSetting("store-fs-enabled", "false"));
    }

    protected static String getConfigurationSetting(final String name, final String def)
    {
        return System.getenv(name) != null ? System.getenv(name) : System.getProperty(name, def);
    }

    // New utility methods, which are useful in all of our tests.
    protected Lexicon createNewLexicon(String name, String expressionRegex)
    {
        Lexicon lexicon = new Lexicon();
        lexicon.name = name;

        {
            LexiconExpression expression = new LexiconExpression();
            expression.type = LexiconExpressionType.REGEX;
            expression.expression = expressionRegex;

            lexicon.lexiconExpressions = new LinkedList<>();
            lexicon.lexiconExpressions.add(expression);
        }

        return classificationApi.create(lexicon);
    }

    protected LexiconCondition createLexiconCondition(Lexicon created, String field, String name)
    {
        return createLexiconCondition(created, field, name, false);
    }

    protected InputStream getResourceAsStream(String resourceName)
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }

    protected String getResourceAsString(String resourceName)
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        try {
            return IOUtils.toString(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected LexiconCondition createLexiconCondition(Lexicon created, String field, String name, Boolean dontCreateNow)
    {
        LexiconCondition lexiconCondition = new LexiconCondition();
        lexiconCondition.name = name;
        lexiconCondition.field = field;
        lexiconCondition.language = "eng";
        lexiconCondition.value = created.id;

        // if someone wants to attach, or hold off creation, allow this.
        if (dontCreateNow) {
            return lexiconCondition;
        }

        return classificationApi.create(lexiconCondition);
    }

    protected DocumentCollection createUniqueDocumentCollection(String name)
    {
        DocumentCollection documentCollection = new DocumentCollection();

        if (Strings.isNullOrEmpty(name)) {
            documentCollection.name = ("ClassificationApiCollectionSequenceIt unique docCollection_");
        } else {
            documentCollection.name = name;
        }
        documentCollection.description = "used as unique doc collection to lookup by...";

        return classificationApi.create(documentCollection);
    }

    /**
     * @return the policyApi
     */
    protected PolicyApi getPolicyApi()
    {
        ApiProvider apiProvider = new ApiProvider(genericApplicationContext);
        return apiProvider.getPolicyApi();
    }

    /**
     * @return the workflowApi
     */
    protected WorkflowApi getWorkflowApi()
    {
        ApiProvider apiProvider = new ApiProvider(genericApplicationContext);
        return apiProvider.getWorkflowApi();
    }
}

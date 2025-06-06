/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.resourceaccesssecurity;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>ResourceAccessGate</code> defines a service API which might be used
 * to make some restrictions to accessing resources.
 *
 * Implementations of this service interface must be registered like
 * ResourceProvider with a path (like provider.roots). If different
 * ResourceAccessGateService services match a path, not only the
 * ResourceAccessGateService with the longest path will be called, but all of
 * them, that's in contrast to the ResourceProvider, but in this case more
 * logical (and secure!). The gates will be called in the order of the
 * service ranking.
 * If one of the gates grants access for a given operation access will be granted.
 *
 * service properties:
 * <ul>
 * <li><b>path</b>: regexp to define on which paths the service should be called
 * (default .*)</li>
 * <li><b>operations</b>: set of operations on which the service should be
 * called ("read,create,update,delete,execute", default all of them)</li>
 * <li><b>finaloperations</b>: set of operations on which the service answer is
 * final and no further service should be called (default none of them), except
 * the GateResult is {@link GateResult#CANT_DECIDE}</li>
 * </ul>
 *
 * The resource access gate can either have the context {@link #PROVIDER_CONTEXT},
 * in this case the gate is only applied to resource providers requesting the
 * security checks. Or the context can be {@link #APPLICATION_CONTEXT}. In this
 * case the access gate is invoked for the whole resource tree.
 * This is indicated by the required service property {@link #CONTEXT}. If the
 * property is missing or invalid, the service is ignored.
 */
@ConsumerType
public interface ResourceAccessGate {

    /**
     * The service name to use when registering implementations of this
     * interface as services (value is
     * "org.apache.sling.api.resource.ResourceAccessGate").
     */
    String SERVICE_NAME = ResourceAccessGate.class.getName();

    /**
     * The name of the service registration property containing the context
     * of this service. Allowed values are {@link #APPLICATION_CONTEXT} and
     * {@link #PROVIDER_CONTEXT}.
     * This property is required and has no default value.
     * (value is "access.context")
     */
    String CONTEXT = "access.context";

    /**
     * Allowed value for the {@link #CONTEXT} service registration property.
     * Services marked with this context are applied to all resources.
     */
    String APPLICATION_CONTEXT = "application";

    /**
     * Allowed value for the {@link #CONTEXT} service registration property.
     * Services marked with this context are only applied to resource
     * providers which indicate the additional checks with the
     * {@link org.apache.sling.api.resource.ResourceProvider#USE_RESOURCE_ACCESS_SECURITY}
     * property.
     */
    String PROVIDER_CONTEXT = "provider";

    /**
     * The name of the service registration property containing the path as a
     * regular expression for which the service should be called (value is
     * "path").
     */
    String PATH = "path";

    /**
     * The name of the (multi-value) service registration property containing the operations
     * for which the service should be called, defaults to all the operations
     * (value is "operations").
     */
    String OPERATIONS = "operations";

    /**
     * The name of the (multi-value) service registration property containing the operations
     * for which the service should be called and no further service should be
     * called after this, except the services returns DONTCARE as result,
     * default is empty (none of them are final) (value is "finaloperations").
     */
    String FINALOPERATIONS = "finaloperations";

    /**
     * <code>GateResult</code> defines 3 possible states which can be returned
     * by the different canXXX methods of this interface.
     * <ul>
     * <li>GRANTED: means no restrictions</li>
     * <li>DENIED: means no permission for the requested action</li>
     * <li>CANT_DECIDE: means that the implementation of the service has no
     * information or can't decide and therefore neither can't grant or deny
     * access</li>
     * </ul>
     */
    public enum GateResult {
        GRANTED,
        DENIED,
        CANT_DECIDE
    };

    public enum Operation {
        READ("read"),
        CREATE("create"),
        UPDATE("update"),
        DELETE("delete"),
        EXECUTE("execute"),
        ORDER_CHILDREN("order-children");

        private String text;

        Operation(String text) {
            this.text = text;
        }

        public static Operation fromString(String opAsString) {
            Operation returnValue = null;

            for (Operation op : Operation.values()) {
                if (opAsString.equals(op.getText())) {
                    returnValue = op;
                    break;
                }
            }

            return returnValue;
        }

        public String getText() {
            return this.text;
        }
    }

    public GateResult canRead(Resource resource);

    public GateResult canCreate(String absPathName, ResourceResolver resourceResolver);

    public default GateResult canOrderChildren(Resource resource) {
        return GateResult.CANT_DECIDE;
    }

    public GateResult canUpdate(Resource resource);

    public GateResult canDelete(Resource resource);

    public GateResult canExecute(Resource resource);

    public GateResult canReadValue(Resource resource, String valueName);

    public GateResult canCreateValue(Resource resource, String valueName);

    public GateResult canUpdateValue(Resource resource, String valueName);

    public GateResult canDeleteValue(Resource resource, String valueName);

    /**
     * Allows to transform the query based on the current user's credentials.
     * Can be used to narrow down queries to omit results that the current user
     * is not allowed to see anyway, speeding up downstream access control.
     *
     * Query transformations are not critical with respect to access control as
     * results are checked using the canRead.. methods anyway.
     *
     * @param query
     *            the query
     * @param language
     *            the language in which the query is expressed
     * @param resourceResolver
     *            the resource resolver which resolves the query
     * @return the transformed query or the original query if no tranformation
     *      took place. This method should never return <code>null</code>
     * @throws AccessSecurityException
     */
    public String transformQuery(String query, String language, ResourceResolver resourceResolver)
            throws AccessSecurityException;

    /* for convenience (and performance) */
    public boolean hasReadRestrictions(ResourceResolver resourceResolver);

    public boolean hasCreateRestrictions(ResourceResolver resourceResolver);

    public default boolean hasOrderChildrenRestrictions(ResourceResolver resourceResolver) {
        return true;
    }

    public boolean hasUpdateRestrictions(ResourceResolver resourceResolver);

    public boolean hasDeleteRestrictions(ResourceResolver resourceResolver);

    public boolean hasExecuteRestrictions(ResourceResolver resourceResolver);

    public boolean canReadAllValues(Resource resource);

    public boolean canCreateAllValues(Resource resource);

    public boolean canUpdateAllValues(Resource resource);

    public boolean canDeleteAllValues(Resource resource);
}

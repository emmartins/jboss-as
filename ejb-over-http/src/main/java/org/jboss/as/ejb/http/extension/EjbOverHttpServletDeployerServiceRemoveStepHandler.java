/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb.http.extension;

import org.apache.catalina.Context;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author sfcoy
 */
public class EjbOverHttpServletDeployerServiceRemoveStepHandler extends AbstractRemoveStepHandler {

    static final EjbOverHttpServletDeployerServiceRemoveStepHandler INSTANCE
            = new EjbOverHttpServletDeployerServiceRemoveStepHandler();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        ModelNode virtualHostModel
                = ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.resolveModelAttribute(context, model);
        final String virtualHost = virtualHostModel.asString();

        ModelNode contextModel
                = ConnectorResourceDefinition.CONTEXT_ATTR.resolveModelAttribute(context, model);
        final String webContext = "/" + contextModel.asString();

        final ServiceName serviceName = EjbOverHttpServletDeployerService.SERVICE_NAME.append(webContext);
        EjbOverHttpLogger.LOGGER.infof("Removing %s", serviceName);
        context.removeService(EjbOverHttpServletDeployerService.SERVICE_NAME.append(webContext));
    }
}

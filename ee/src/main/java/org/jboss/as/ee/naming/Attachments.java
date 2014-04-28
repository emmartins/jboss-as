/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.naming;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * EE related attachments.
 *
 * @author John Bailey
 * @author Eduardo Martins
 */
public class Attachments {

    public static final AttachmentKey<SharedContextBindings> EE_JNDI_SHARED_CONTEXTS_BINDINGS = AttachmentKey.create(SharedContextBindings.class);

    public static final AttachmentKey<ApplicationBinder> EE_JNDI_APP_CONTEXT_BINDER = AttachmentKey.create(ApplicationBinder.class);
    public static final AttachmentKey<ModuleBinder> EE_JNDI_MODULE_CONTEXT_BINDER = AttachmentKey.create(ModuleBinder.class);

    public static final AttachmentKey<JavaNamespaceSetup> JAVA_NAMESPACE_SETUP_ACTION = AttachmentKey.create(JavaNamespaceSetup.class);

}

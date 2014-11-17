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

package org.jboss.as.naming.service;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;

/**
 * Service responsible for binding and unbinding an entry into a naming context.  This service can be used as a dependency for
 * any service that needs to retrieve this entry from the context.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class BinderService implements Service<ManagedReferenceFactory> {

    protected final InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();
    protected final String name;
    protected final InjectedValue<ManagedReferenceFactory> managedReferenceFactory = new InjectedValue<ManagedReferenceFactory>();
    protected final Object source;
    protected ServiceController<?> controller;

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding. May be either an absolute or relative name
     * @param source
     */
    public BinderService(final String name, Object source) {
        if (name.startsWith("java:")) {
            //this is an absolute reference
            this.name = name.substring(name.indexOf('/') + 1);
        } else {
            this.name = name;
        }
        this.source = source;
    }

    public BinderService(final String name) {
        this(name, null);
    }

    public Object getSource() {
        return source;
    }

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entity can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        controller = context.getController();
        final ServiceName serviceName = controller.getName();
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        namingStore.add(serviceName);
        ROOT_LOGGER.tracef("Bound resource %s into naming store %s (service name %s)", name, namingStore, serviceName);
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final ServiceName serviceName = context.getController().getName();
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        namingStore.remove(serviceName);
        controller = null;
        ROOT_LOGGER.tracef("Unbound resource %s from naming store %s (service name %s)", name, namingStore, serviceName);
    }

    /**
     * Forces the binder service stop.
     */
    public synchronized void stopNow() {
        if (controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
            controller = null;
        }
    }

    /**
     * Get the value from the injected context.
     *
     * @return The value of the named entry
     * @throws IllegalStateException
     */
    public ManagedReferenceFactory getValue() throws IllegalStateException {
        return managedReferenceFactory.getValue();
    }

    /**
     * Get the injector for the item to be bound.
     *
     * @return the injector
     */
    public InjectedValue<ManagedReferenceFactory> getManagedObjectInjector() {
        return managedReferenceFactory;
    }

    /**
     * Get the naming store injector.
     *
     * @return the injector
     */
    public InjectedValue<ServiceBasedNamingStore> getNamingStoreInjector() {
        return namingStoreValue;
    }

    @Override
    public String toString() {
        return new StringBuilder("BinderService[name=").append(name).append(", source=").append(source).append(']').toString();
    }

}

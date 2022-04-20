/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 *
 * Copyright (c) 2008 - 2019 LSC Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2022 LSC Project
 *           Quan Tran Hong <hqtran@linagora.com>
 ****************************************************************************
 */
package org.lsc.plugins.connectors.james;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.PluginConnectionType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.plugins.connectors.james.beans.Contact;
import org.lsc.plugins.connectors.james.beans.User;
import org.lsc.plugins.connectors.james.generated.JamesService;
import org.lsc.service.IWritableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JamesContactDstService implements IWritableService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JamesContactDstService.class);

    private final JamesDao jamesDao;
    private final JamesService service;
    private final Class<IBean> beanClass;

    public JamesContactDstService(final TaskType task) throws LscServiceConfigurationException {
        try {
            if (task.getPluginDestinationService().getAny() == null
                || task.getPluginDestinationService().getAny().size() != 1
                || !((task.getPluginDestinationService().getAny().get(0) instanceof JamesContactDstService))) {
                throw new LscServiceConfigurationException("Unable to identify the James service configuration inside the plugin source node of the task: " + task.getName());
            } else {
                this.service = (JamesService) task.getPluginDestinationService().getAny().get(0);
                this.beanClass = (Class<IBean>) Class.forName(task.getBean());
                LOGGER.debug("Task bean is: " + task.getBean());
                PluginConnectionType connection = (PluginConnectionType) service.getConnection().getReference();
                this.jamesDao = new JamesDao(connection.getUrl(), connection.getPassword(), task);
            }
        } catch (ClassNotFoundException e) {
            throw new LscServiceConfigurationException(e);
        }
    }

    @Override
    public boolean apply(LscModifications lscModifications) throws LscServiceException {
        if (lscModifications.getMainIdentifier() == null) {
            LOGGER.error("MainIdentifier is needed to update");
            return false;
        }
        User user = new User(lscModifications.getMainIdentifier());
        LOGGER.debug("User: {}, Operation: {}", user.email, lscModifications.getOperation());

        try {
            switch (lscModifications.getOperation()) {
                case CREATE_OBJECT:
                    return jamesDao.addDomainContact(extractContact(lscModifications));
                case UPDATE_OBJECT:
                    // TODO call API to update domain contactx
                    return true;
                case DELETE_OBJECT:
                    // TODO call API to delete domain contact
                    return jamesDao.removeUser(user);
                default:
                    LOGGER.debug("{} operation, ignored.", lscModifications.getOperation());
                    return true;
            }
        } catch (ProcessingException exception) {
            LOGGER.error(String.format("ProcessingException while writing (%s)", exception));
            LOGGER.debug(exception.toString(), exception);
            return false;
        }
    }

    @Override
    public List<String> getWriteDatasetIds() {
        return service.getWritableAttributes().getString();
    }

    @Override
    public IBean getBean(String s, LscDatasets lscDatasets, boolean b) throws LscServiceException {
        // TODO impl
        return null;
    }

    @Override
    public Map<String, LscDatasets> getListPivots() throws LscServiceException {
        try {
            List<User> userList = jamesDao.getUsersListViaDomainContacts();
            Map<String, LscDatasets> listPivots = new HashMap<>();
            for (User user : userList) {
                listPivots.put(user.email, user.toDatasets());
            }
            return listPivots;
        } catch (ProcessingException e) {
            LOGGER.error(String.format("ProcessingException while getting pivot list (%s)", e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceCommunicationException(e);
        } catch (WebApplicationException e) {
            LOGGER.error(String.format("WebApplicationException while getting pivot list (%s)", e));
            LOGGER.debug(e.toString(), e);
            throw new LscServiceException(e);
        }
    }

    private Contact extractContact(LscModifications lscModifications) {
        return new Contact(lscModifications.getMainIdentifier(), extractFirstname(lscModifications), extractSurname(lscModifications));
    }

    private Optional<String> extractFirstname(LscModifications lscModifications) {
        return Optional.ofNullable(lscModifications.getModificationsItemsByHash().get("givenName").get(0)).map(String.class::cast);
    }

    private Optional<String> extractSurname(LscModifications lscModifications) {
        return Optional.ofNullable(lscModifications.getModificationsItemsByHash().get("sn").get(0)).map(String.class::cast);
    }
}

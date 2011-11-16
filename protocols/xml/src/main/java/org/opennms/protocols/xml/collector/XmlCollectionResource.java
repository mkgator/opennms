/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.protocols.xml.collector;

import org.opennms.netmgt.collectd.AbstractCollectionResource;
import org.opennms.netmgt.collectd.CollectionAgent;
import org.opennms.netmgt.config.collector.CollectionAttributeType;
import org.opennms.netmgt.config.collector.ServiceParameters;

/**
 * The abstract Class XmlCollectionResource.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public abstract class XmlCollectionResource extends AbstractCollectionResource {

    /**
     * Instantiates a new XML collection resource.
     *
     * @param agent the agent
     */
    public XmlCollectionResource(CollectionAgent agent) {
        super(agent);
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.AbstractCollectionResource#shouldPersist(org.opennms.netmgt.config.collector.ServiceParameters)
     */
    public boolean shouldPersist(ServiceParameters params) {
        return true;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.AbstractCollectionResource#rescanNeeded()
     */
    public boolean rescanNeeded() {
        // A rescan is never needed for the XmlCollector, at least on resources
        return false;
    }

    /**
     * Sets the attribute value.
     *
     * @param type the type
     * @param value the value
     */
    public void setAttributeValue(CollectionAttributeType type, String value) {
        XmlCollectionAttribute attr = new XmlCollectionAttribute(this, type, type.getName(), value);
        addAttribute(attr);
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.collectd.AbstractCollectionResource#getType()
     */
    public int getType() {
        return -1; // Is this right?
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.config.collector.CollectionResource#getResourceTypeName()
     */
    public abstract String getResourceTypeName();

    /* (non-Javadoc)
     * @see org.opennms.netmgt.config.collector.CollectionResource#getInstance()
     */
    public abstract String getInstance();

    /* (non-Javadoc)
     * @see org.opennms.netmgt.config.collector.CollectionResource#getParent()
     */
    public String getParent() {
        return Integer.toString(m_agent.getNodeId());
    }

}
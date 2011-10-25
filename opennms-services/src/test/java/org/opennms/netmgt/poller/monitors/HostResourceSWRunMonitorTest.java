/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2011 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.poller.monitors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.utils.LogUtils;
import org.opennms.mock.snmp.JUnitSnmpAgent;
import org.opennms.mock.snmp.JUnitSnmpAgentExecutionListener;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.mock.MockMonitoredService;
import org.opennms.netmgt.model.PollStatus;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.test.mock.MockLogAppender;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Test class for HostResourceSWRunMonitorTest.
 *
 * @author <A HREF="mailto:agalue@opennms.org">Alejandro Galue</A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
    JUnitSnmpAgentExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-proxy-snmp.xml"
})
public class HostResourceSWRunMonitorTest {
    private Level m_defaultLogLevel = Level.WARN;

    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        FileSystemResource r = new FileSystemResource("src/test/resources/org/opennms/netmgt/config/snmp-config.xml");
        SnmpPeerFactory.setInstance(new SnmpPeerFactory(r.getInputStream()));
    }

    @After
    public void tearDown() throws Exception {
        MockLogAppender.assertNotGreaterOrEqual(m_defaultLogLevel);
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testUnknownService() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("service-name", "this service does not exist!");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        log(status.getReason());
        Assert.assertFalse(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testMonitorWithRegex() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testMonitorWithoutRegex() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("service-name", "eclipse");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testMinServices() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "2");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testInvalidMinServices() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "5");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        log(status.getReason());
        Assert.assertFalse(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testMaxServices() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("max-services", "5");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testInvalidMaxServices() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("max-services", "3");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        log(status.getReason());
        Assert.assertFalse(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testServicesRange() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "2");
        parameters.put("max-services", "5");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testInvalidRange() throws Exception {
        m_defaultLogLevel = Level.ERROR;
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "8");
        parameters.put("max-services", "5");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        log(status.getReason());
        Assert.assertFalse(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testServicesRangeWithoutMatchAll() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "1");
        parameters.put("max-services", "3");
        parameters.put("match-all", "false");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        Assert.assertTrue(status.isAvailable());
    }

    @Test
    @JUnitSnmpAgent(resource = "/org/opennms/netmgt/snmp/snmpTestData1.properties")
    public void testInvalidServicesRange() throws Exception {
        HostResourceSwRunMonitor monitor = new HostResourceSwRunMonitor();
        Map<String, Object> parameters = createBasicParams();
        parameters.put("min-services", "1");
        parameters.put("max-services", "3");
        PollStatus status = monitor.poll(createMonitor(), parameters);
        log(status.getReason());
        Assert.assertFalse(status.isAvailable());
    }

    private Map<String, Object> createBasicParams() {
        Map<String, Object> parameters = new HashMap<String,Object>();
        parameters.put("port", "9161");
        parameters.put("service-name", "~^(auto|sh).*");
        parameters.put("match-all", "true");
        return parameters;
    }

    private MonitoredService createMonitor() throws UnknownHostException {
        MonitoredService svc = new MockMonitoredService(1, "test-server", InetAddress.getLocalHost().getHostAddress(), "SWRUN-TEST");
        return svc;
    }

    private void log(String message) {
        LogUtils.debugf(this, message);
    }

}
/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2008 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 * OpenNMS Licensing       <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */
package org.opennms.netmgt.provision.service.lifecycle;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.provision.service.lifecycle.annotations.Activity;
import org.opennms.netmgt.provision.service.lifecycle.annotations.ActivityProvider;



/**
 * LifecycleTest
 *
 * @author brozow
 */
public class LifeCycleTest {
    
    // define a lifecyle
    
    // how do we build scanners when we need to run them
    
    // a better name for a scanner (mojo?)
    
    // how do we find the scanners
    
    // lifecycles should be defined in configuration
    
    // how do we pass data into the scanner methods
    
    // scanners should be dependency injected
    
    // resource scanners will take the 'resource' in the scan method
    
    // node scanners will take the 'node' in the scan method
    
    // import scanner can take the foreign source.... how do I get the URL?
    
    // persist lifecycles that are in progress?
    
    /*
     *  A run of a lifecycle has a 'trigger' of some kind....
     *  1.  newSuspectEvent
     *  2.  forceRescanEvent
     *  3.  'scheduled' rescan
     *  4.  'import' event
     *  5.  'scheduled' import
     *  6.  import triggers a node scan
     *  7   node scan triggers a service scan
     *  
     *  We can pass a trigger object into each
     *  
     *  Need to use generics in some way so that I end up with the correct type for triggers and/or other parameters
     *  
     *  I could pass the Lifecycle object into each phase method... and I could set attributes on the lifecycle object
     *  
     *  I would need to define a set of attributes that could be set for each lifecycle
     *  
     *  I could use annotations to pass those attributes as methods
     *  
     *  Maybe 'resourceFactories' could be annotated as well and implement a simple interface
     *  
     *  
     *   
     */
    
    private static final String PHASE_DATA = "phaseData";
    public static final String NESTED_DATA = "nestedData";
    public static final String NEST_LEVEL = "nestLevel";
    public static final String MAX_DEPTH = "maxDepth";
    
    private static LifeCycleFactory m_lifeCycleFactory;
    
    public static class DefaultLifeCycleFactory implements LifeCycleFactory {
        
        private Map<String, LifeCycleDefinition> m_definition = new HashMap<String, LifeCycleDefinition>();

        public LifeCycle createLifeCycle(String lifeCycleName) {
            LifeCycleDefinition defn = m_definition.get(lifeCycleName);
            if (defn == null) {
                throw new IllegalArgumentException("Unable to find a definition for lifecycle "+lifeCycleName);
            }
            
            return defn.build();
        }
        
        public void addDefinition(LifeCycleDefinition definition) {
            m_definition.put(definition.getLifeCycleName(), definition);
        }
        
    }
    
    @Before
    public void setUp() {
        
        DefaultLifeCycleFactory factory = new DefaultLifeCycleFactory();
        
        NestedLifeCycleActivites nested = new NestedLifeCycleActivites();
        nested.setLifeCycleDefinition(factory);
        
        LifeCycleDefinition lifeCycleDefinition = new LifeCycleDefinition("sample")
        .addPhases("phase1", "phase2", "phase3")
        .addProviders(nested, new PhaseTestActivities());
        
        factory.addDefinition(lifeCycleDefinition);
        
        m_lifeCycleFactory = factory;

        
    }
    

    @ActivityProvider
    public static class PhaseTestActivities extends ActivityProviderSupport {
        
        private void appendPhase(LifeCycle lifecycle, final String value) {
            appendToStringAttribute(lifecycle, PHASE_DATA, value);
        }

        // this should be called first
        @Activity(phase="phase1", lifecycle="sample")
        public void doPhaseOne(LifeCycle lifecycle) {

            appendPhase(lifecycle, "phase1 ");

        }

        // this should be called last
        @Activity(phase="phase3", lifecycle = "sample")
        public void doPhaseThree(LifeCycle lifecycle) {

            appendPhase(lifecycle, "phase3");

        }

        // this should be called in the middle
        @Activity(phase="phase2", lifecycle = "sample")
        public void doPhaseTwo(LifeCycle lifecycle) {

            appendPhase(lifecycle, "phase2 ");

        }
        
        // this should not be called
        @Activity(phase="phase3", lifecycle = "invalidLifecycle")
        public void doPhaseInvalidLifeCycle(LifeCycle lifecycle) {

            appendPhase(lifecycle, " invalidLifecycle");

        }

        // this should not be called
        @Activity(phase="invalidPhase", lifecycle = "sample")
        public void doPhaseInvalid(LifeCycle lifecycle) {

            appendPhase(lifecycle, " invalidPhase");

        }

    }
    
    // waitFor should throw an exception if its not been triggered
    
    // if we don't call trigger then the getAttribute should return ""
    
    
    @Test
    public void testLifeCycleAttributes() {
        LifeCycle lifecycle = m_lifeCycleFactory.createLifeCycle("sample");
        
        lifecycle.setAttribute(PHASE_DATA, "phase1 phase2 phase3");

        assertEquals("phase1 phase2 phase3", lifecycle.getAttribute(PHASE_DATA, String.class));
    }
    
    @Test
    public void testTriggerLifeCycle() {
        LifeCycle lifecycle = m_lifeCycleFactory.createLifeCycle("sample");
        
        lifecycle.trigger();
        
        lifecycle.waitFor();
        
        assertEquals("phase1 phase2 phase3", lifecycle.getAttribute(PHASE_DATA, String.class));
    }

    @ActivityProvider
    public static class NestedLifeCycleActivites extends ActivityProviderSupport {
        
        private LifeCycleFactory m_lifeCycleFactory;
        
        public void setLifeCycleDefinition(DefaultLifeCycleFactory factory) {
            m_lifeCycleFactory = factory;
        }

        private void appendPhase(LifeCycle lifecycle, final String phase) {
            appendToStringAttribute(lifecycle, NESTED_DATA, phase);
        }

        // this should be called first
        @Activity(phase="phase1", lifecycle="sample")
        public void doPhaseOne(LifeCycle lifecycle) {

            appendPhase(lifecycle, getPrefix(lifecycle)+"phase1 ");

        }

        // this should be called last
        @Activity(phase="phase3", lifecycle = "sample")
        public void doPhaseThree(LifeCycle lifecycle) {

            appendPhase(lifecycle, getPrefix(lifecycle)+"phase3 ");

        }

        // this should be called in the middle
        @Activity(phase="phase2", lifecycle = "sample")
        public LifeCycle doPhaseTwo(LifeCycle lifecycle) {

            appendPhase(lifecycle, getPrefix(lifecycle)+"phase2start ");
            
            LifeCycle nested = null;
            
            int nestLevel = lifecycle.getAttribute(NEST_LEVEL, 0);
            int maxDepth = lifecycle.getAttribute(MAX_DEPTH, 0);
            if (nestLevel < maxDepth) {
                nested = m_lifeCycleFactory.createLifeCycle("sample");
                nested.setAttribute(MAX_DEPTH, maxDepth);
                nested.setAttribute(NEST_LEVEL, nestLevel+1);
                
                nested.trigger();
                
                nested.waitFor();
                
                appendPhase(lifecycle, nested.getAttribute(NESTED_DATA, String.class));
            }

            appendPhase(lifecycle, getPrefix(lifecycle)+"phase2end ");

            return nested;
        }

        private String getPrefix(LifeCycle lifecycle) {
            int nestLevel = lifecycle.getAttribute(NEST_LEVEL, 0);
            return buildPrefix(nestLevel);
        }
        
        private String buildPrefix(int nestLevel) {
            StringBuilder buf = new StringBuilder();
            buildPrefixHelper(nestLevel, buf);
            return buf.toString();

        }
        
        private void buildPrefixHelper(int nestLevel, StringBuilder buf) {
            if (nestLevel == 0) {
                return;
            } else {
                buildPrefixHelper(nestLevel-1, buf);
                buf.append("level").append(nestLevel).append('.');
            }
        }
        
        

    }
    
    @Test
    public void testNestedLifeCycle() {

        LifeCycle lifecycle = m_lifeCycleFactory.createLifeCycle("sample");
        lifecycle.setAttribute(MAX_DEPTH, 2);

        lifecycle.trigger();
        
        lifecycle.waitFor();
        
        assertEquals("phase1 phase2start level1.phase1 level1.phase2start level1.phase2end level1.phase3 phase2end phase3 ", lifecycle.getAttribute(NESTED_DATA, String.class));

    }
    
    
    public static class ActivityProviderSupport {

        protected void appendToStringAttribute(LifeCycle lifecycle, String key, String value) {
                    lifecycle.setAttribute(key, lifecycle.getAttribute(key, "")+value);
                }
        
    }
    
}

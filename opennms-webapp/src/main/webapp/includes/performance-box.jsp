<%--

//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

--%>

<%--
  This page is included by other JSPs to create a box containing an
  entry to the performance reporting system.
  
  It expects that a <base> tag has been set in the including page
  that directs all URLs to be relative to the servlet context.
--%>

<%@page language="java"
        contentType="text/html"
        session="true"
        import="
        java.util.List,
        org.opennms.netmgt.dao.NodeDao,
        org.opennms.netmgt.model.OnmsNode,
        org.opennms.web.Util,
		org.springframework.web.context.WebApplicationContext,
        org.springframework.web.context.support.WebApplicationContextUtils"
%>

<%!
    public NodeDao m_nodeDao = null;


	public void init() throws ServletException {
	    WebApplicationContext m_webAppContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        m_nodeDao = (NodeDao) m_webAppContext.getBean("nodeDao", NodeDao.class);
    }
%>

<%
    List<OnmsNode> nodes = m_nodeDao.findAll();
%>


<script type="text/javascript">
  function resetPerformanceBoxSelected() {
    document.performanceBoxNodeList.parentResource[0].selected = true;
  }
  
  function validatePerformanceBoxNodeChosen() {
    var selectedParentResource = false
    
    for (i = 0; i < document.performanceBoxNodeList.parentResource.length; i++) {
      // make sure something is checked before proceeding
      if (document.performanceBoxNodeList.parentResource[i].selected
          && document.performanceBoxNodeList.parentResource[i].value != "") {
        selectedParentResource = document.performanceBoxNodeList.parentResource[i].value;
        break;
      }
    }
    
    return selectedParentResource;
  }
  
  function goPerformanceBoxChange() {
    var nodeChosen = validatePerformanceBoxNodeChosen();
    if (nodeChosen != false) {
      document.performanceBoxForm.parentResource.value = nodeChosen;
      document.performanceBoxForm.submit();
      /*
       * We reset the selection after submitting the form so if the user
       * uses the back button to get back to this page, it will be set at
       * the "choose a node" option.  Without this, they wouldn't be able
       * to proceed forward to the same node because won't trigger the
       * onChange action on the <select/> element.  We also do the submit
       * in a separate form after we copy the chosen value over, just to
       * ensure that no problems happen by resetting the selection
       * immediately after calling submit().
       */
      resetPerformanceBoxSelected();
    }
  }
  
</script>

<h3><a href="performance/index.jsp">Resource Graphs</a></h3>
<div class="boxWrapper">

<%  if( nodes != null && nodes.size() > 0 ) { %>
      <form method="get" name="performanceBoxForm" action="graph/chooseresource.htm" >
        <input type="hidden" name="parentResourceType" value="node" />
        <input type="hidden" name="reports" value="all"/>
        <input type="hidden" name="relativetime" value="lastday" />
        <input type="hidden" name="parentResource" value="" />
      </form>
      
      <form name="performanceBoxNodeList">
              <p>Choose a <label for="node">node to query</label>:</p>
              <select style="width: 100%;" name="parentResource" id="node" onchange="goPerformanceBoxChange();">
                <option value="">-- Choose a node --</option>
                <% for (OnmsNode node : nodes) { %>
                  <option value="<%=node.getId()%>"><%=Util.htmlify(node.getLabel())%></option>
                <% } %>
              </select>
      </form>
      
      <script type="text/javascript">
        resetPerformanceBoxSelected();
      </script>
      
<% } else { %>
      <p>No nodes are in the database</p>
<% }  %>
</div>

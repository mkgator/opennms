package org.opennms.dashboard.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Category;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.dashboard.client.Alarm;
import org.opennms.dashboard.client.Notification;
import org.opennms.dashboard.client.SurveillanceData;
import org.opennms.dashboard.client.SurveillanceGroup;
import org.opennms.dashboard.client.SurveillanceService;
import org.opennms.dashboard.client.SurveillanceSet;
import org.opennms.netmgt.config.GroupFactory;
import org.opennms.netmgt.config.GroupManager;
import org.opennms.netmgt.config.groups.Group;
import org.opennms.netmgt.config.surveillanceViews.View;
import org.opennms.netmgt.dao.AlarmDao;
import org.opennms.netmgt.dao.CategoryDao;
import org.opennms.netmgt.dao.GraphDao;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.NotificationDao;
import org.opennms.netmgt.dao.ResourceDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNotification;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.web.svclayer.ProgressMonitor;
import org.opennms.web.svclayer.SimpleWebTable;
import org.opennms.web.svclayer.SimpleWebTable.Cell;
import org.opennms.web.svclayer.dao.SurveillanceViewConfigDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional(readOnly = true)
public class DefaultSurveillanceService implements SurveillanceService, InitializingBean {

    private NodeDao m_nodeDao;
    private ResourceDao m_resourceDao;
    private GraphDao m_graphDao;
    private NotificationDao m_notificationDao;
    private org.opennms.web.svclayer.SurveillanceService m_webSurveillanceService;
    private SurveillanceViewConfigDao m_surveillanceViewConfigDao;
    private CategoryDao m_categoryDao;
    private AlarmDao m_alarmDao;
    private GroupManager m_groupManager;
    
    public SurveillanceData getSurveillanceData() {
        SurveillanceData data = new SurveillanceData();

        SimpleWebTable table = m_webSurveillanceService.createSurveillanceTable(getView().getName(), new ProgressMonitor());
        
        data.setName(getView().getName());
        
        List<SurveillanceGroup> columnGroups = new ArrayList<SurveillanceGroup>();
        for (Cell columnHeader : table.getColumnHeaders().subList(1, table.getColumnHeaders().size())) {
            SurveillanceGroup columnGroup = new SurveillanceGroup();
            columnGroup.setId(columnHeader.getContent().toString());
            columnGroup.setLabel(columnHeader.getContent().toString());
            columnGroup.setColumn(true);
            columnGroups.add(columnGroup);
        }
        data.setColumnGroups(columnGroups.toArray(new SurveillanceGroup[columnGroups.size()]));
        
        List<SurveillanceGroup> rowGroups = new ArrayList<SurveillanceGroup>();
        for (List<Cell> row : table.getRows()) {
            Cell rowHeader = row.get(0);
            
            SurveillanceGroup rowGroup = new SurveillanceGroup();
            rowGroup.setId(rowHeader.getContent().toString());
            rowGroup.setLabel(rowHeader.getContent().toString());
            rowGroups.add(rowGroup);
        }
        data.setRowGroups(rowGroups.toArray(new SurveillanceGroup[rowGroups.size()]));

        int rowIndex = 0;
        for (List<Cell> row : table.getRows()) {
            int columnIndex = 0;
            for (Cell cell : row.subList(1, row.size())) {
                data.setCell(rowIndex, columnIndex, cell.getContent().toString(), cell.getStyleClass());
                
                columnIndex++;
            }
            rowIndex++;
        }
        
        data.setComplete(true);

        return data;
    }
    
    
    /*
    private int m_count = 0;
    private Timer m_timer = new Timer();
    private Random m_random = new Random();
    private SurveillanceData m_data;

    public SurveillanceData getSurveillanceData() {
        
        System.err.println("Request made!");
        
        if (m_data == null) {
            System.err.println("Creating new data");
            final SurveillanceData data = new SurveillanceData();
            m_data = data;
            
            SurveillanceGroup[] columnGroups = new SurveillanceGroup[] {
                    new SurveillanceGroup("prod", "Production"), 
                    new SurveillanceGroup("test", "Test"), 
                    new SurveillanceGroup("dev", "Developement")
            };
            
            SurveillanceGroup[] rowGroups = new SurveillanceGroup[] {
                    new SurveillanceGroup("ibm", "IBM"),
                    new SurveillanceGroup("hp", "HP"),
                    new SurveillanceGroup("duke", "Duke Hospital"),
                    new SurveillanceGroup("unc", "UNC Hospitals")
            };
            
            data.setColumnGroups(columnGroups);
            data.setRowGroups(rowGroups);
            
            m_timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    
                    System.err.println("Updating data");
                    data.setCell(m_count / data.getColumnCount(), m_count % data.getColumnCount(), ""+m_count);
                    
                    m_count++;
                    
                    if (m_count < data.getColumnCount()*data.getRowCount()) {
                        data.setComplete(false);
                    } else {
                        this.cancel();
                        data.setComplete(true);
                        m_count = 0;
                    }

                }
                
            }, 3000, 2000);
        } else if (m_data.isComplete()) {
            SurveillanceData data = m_data;
            m_data = null;
            return data;
        }
        
        return m_data;

        
    }

*/

    public Alarm[] getAlarmsForSet(SurveillanceSet set) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsAlarm.class, "alarm");
        OnmsCriteria nodeCriteria = criteria.createCriteria("node");
        addCriteriaForSurveillanceSet(nodeCriteria, set);
        nodeCriteria.add(Restrictions.ne("type", "D"));
        criteria.addOrder(Order.desc("alarm.severity"));
        
        List<OnmsAlarm> alarms = m_alarmDao.findMatching(criteria);

        Alarm[] alarmArray = new Alarm[alarms.size()];
        
        int index = 0;
        for (OnmsAlarm alarm : alarms) {
            alarmArray[index] = new Alarm(getSeverityString(alarm.getSeverity()), alarm.getNode().getLabel(), alarm.getDescription(), alarm.getCounter());
            index++;
        }
        
        return alarmArray;
    }
    
    private String getSeverityString(Integer severity) {
        switch(severity) {
        case 1: return "Indeterminate";
        case 2: return "Cleared";
        case 3: return "Normal";
        case 4: return "Warning";
        case 5: return "Minor";
        case 6: return "Major";
        case 7: return "Critical";
        default: return "Unknown";
        }
    }

    public String[] getNodeNames(SurveillanceSet set) {

        List<OnmsNode> nodes = m_nodeDao.findAll();

        List<String> labels = new ArrayList<String>(nodes.size());
        for (OnmsNode node : nodes) {
            labels.add(node.getLabel());
        }

        return labels.toArray(new String[labels.size()]);

    }

    public String[][] getResources(SurveillanceSet set) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsNode.class, "node");
        addCriteriaForSurveillanceSet(criteria, set);
        criteria.add(Restrictions.ne("node.type", "D"));
        criteria.addOrder(Order.asc("node.label"));
        
        List<OnmsNode> nodes = m_nodeDao.findMatching(criteria);
        
        List<OnmsResource> resources = new ArrayList<OnmsResource>();
        for (OnmsNode node : nodes) {
            OnmsResource resource = m_resourceDao.getResourceForNode(node);
            if (resource != null && (resource.getAttributes().size() > 0 || resource.getChildResources().size() > 0)) {
                resources.add(resource);
            }
        }
        
        List<String[]> labels = new ArrayList<String[]>(resources.size());
        for (OnmsResource resource : resources) {
            labels.add(new String[] { resource.getId(), resource.getResourceType().getLabel() + ": " + resource.getLabel() });
        }
        
        return labels.toArray(new String[labels.size()][]);
    }
    

    private void addCriteriaForSurveillanceSet(OnmsCriteria criteria, SurveillanceSet set) {
        CriteriaAddingVisitor visitor = new CriteriaAddingVisitor(criteria);
        visitor.setView(getView());
        visitor.setCategoryDao(m_categoryDao);
        visitor.afterPropertiesSet();

        set.visit(visitor);
    }

    private View getView() {
        String user = getUsername();
        log().debug("Looking for surveillance view that matches user '" + user + "'");
        
        View userView = m_surveillanceViewConfigDao.getView(user);
        if (userView != null) {
            log().debug("Found surveillance view '" + userView.getName() + "' matching user name '" + user + "'");
            return userView;
        }
        
        List<Group> groups = GroupFactory.getInstance().findGroupsForUser(user);
        for (Group group : groups) {
            View groupView = m_surveillanceViewConfigDao.getView(group.getName());
            if (groupView != null) {
                log().debug("Found surveillance view '" + groupView.getName() + "' matching group '" + group.getName() + "' name for user '" + user + "'");
                return groupView;
            }
        }
        
        log().debug("Did not find a surveillance view matching the user's user name or one of their group names.  Using the default view for user '" + user + "'");
        return m_surveillanceViewConfigDao.getDefaultView();
    }
    
    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }


    protected String getUsername() {
        /*
         * This should never be null, as the strategy should create a
         * SecurityContext if one doesn't exist, but let's check anyway.
         */
        SecurityContext context = SecurityContextHolder.getContext();
        Assert.state(context != null, "No security context found when calling SecurityContextHolder.getContext()");
        
        Authentication auth = context.getAuthentication();
        Assert.state(auth != null, "No Authentication object found when calling getAuthentication on our SecurityContext object");
        
        Object obj = auth.getPrincipal();
        Assert.state(obj != null, "No principal object found when calling getPrinticpal on our Authentication object");
        
        
        if (obj instanceof UserDetails) { 
            return ((UserDetails)obj).getUsername(); 
        } else { 
            return obj.toString(); 
        }
    }


    public String[][] getChildResources(String id) {
        OnmsResource parentResource = m_resourceDao.getResourceById(id);
        if (parentResource == null) {
            return null;
        }
        
        List<OnmsResource> resources = parentResource.getChildResources();
        
        List<String[]> labels = new ArrayList<String[]>(resources.size());
        for (OnmsResource resource : resources) {
            labels.add(new String[] { resource.getId(), resource.getResourceType().getLabel() + ": " + resource.getLabel() });
        }
        
        return labels.toArray(new String[labels.size()][]);
    }

    public String[][] getPrefabGraphs(String id) {
        OnmsResource resource = m_resourceDao.getResourceById(id);
        if (resource == null) {
            return null;
        }
        
        PrefabGraph[] graphs = m_graphDao.getPrefabGraphsForResource(resource);
        
        List<String[]> labels = new ArrayList<String[]>(graphs.length);
        for (PrefabGraph graph : graphs) {
            labels.add(new String[] { graph.getName(), graph.getName() });
        }

        return labels.toArray(new String[labels.size()][]);
    }

    public void afterPropertiesSet() throws Exception {
        Assert.state(m_nodeDao != null, "nodeDao property must be set and cannot be null");
        Assert.state(m_resourceDao != null, "resourceDao property must be set and cannot be null");
        Assert.state(m_graphDao != null, "graphDao property must be set and cannot be null");
        Assert.state(m_webSurveillanceService != null, "webSurveillanceService property must be set and cannot be null");
        Assert.state(m_surveillanceViewConfigDao != null, "surveillanceViewConfigDao property must be set and cannot be null");
        Assert.state(m_categoryDao != null, "categoryDao property must be set and cannot be null");
        Assert.state(m_alarmDao != null, "alarmDao property must be set and cannot be null");
        Assert.state(m_notificationDao != null, "notificationDao property must be set and cannot be null");
        Assert.state(m_groupManager != null, "groupManager property must be set and cannot be null");
    }

    public void setNodeDao(NodeDao nodeDao) {
        m_nodeDao = nodeDao;
    }
    
    public void setNotificationDao(NotificationDao notifDao) {
        m_notificationDao = notifDao;
    }
    
    public void setResourceDao(ResourceDao resourceDao) {
        m_resourceDao = resourceDao;
    }

    public void setGraphDao(GraphDao graphDao) {
        m_graphDao = graphDao;
    }

    public org.opennms.web.svclayer.SurveillanceService getWebSurveillanceService() {
        return m_webSurveillanceService;
    }

    public void setWebSurveillanceService(org.opennms.web.svclayer.SurveillanceService webSurveillanceService) {
        m_webSurveillanceService = webSurveillanceService;
    }

    public SurveillanceViewConfigDao getSurveillanceViewConfigDao() {
        return m_surveillanceViewConfigDao;
    }

    public void setSurveillanceViewConfigDao(SurveillanceViewConfigDao surveillanceViewConfigDao) {
        m_surveillanceViewConfigDao = surveillanceViewConfigDao;
    }

    public CategoryDao getCategoryDao() {
        return m_categoryDao;
    }

    public void setCategoryDao(CategoryDao categoryDao) {
        m_categoryDao = categoryDao;
    }

    public AlarmDao getAlarmDao() {
        return m_alarmDao;
    }

    public void setAlarmDao(AlarmDao alarmDao) {
        m_alarmDao = alarmDao;
    }
    
    public GroupManager getGroupManager() {
        return m_groupManager;
    }
    
    public void setGroupManager(GroupManager groupManager) {
        m_groupManager = groupManager;
    }


    /*
    public Notification[] getNotificationsForSet(SurveillanceSet set) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsNotification.class, "notification");
        OnmsCriteria nodeCriteria = criteria.createCriteria("node");
        addCriteriaForSurveillanceSet(nodeCriteria, set);
        nodeCriteria.add(Restrictions.ne("type", "D"));
        criteria.addOrder(Order.desc("notification.respondTime"));
        criteria.addOrder(Order.desc("notification.pageTime"));
        
        List<OnmsNotification> notifications = m_notificationDao.findMatching(criteria);

        Notification[] notifArray = new Notification[notifications.size()];
        
        int index = 0;
        for (OnmsNotification notification : notifications) {
            notifArray[index++] = createNotification(notification);
        }
        
        return notifArray;
    }
    
     private Notification createNotification(OnmsNotification onmsNotif) {
        Notification notif = new Notification();
        notif.setNodeLabel(onmsNotif.getNode().getLabel());
        notif.setResponder(onmsNotif.getAnsweredBy());
        notif.setRespondTime(onmsNotif.getRespondTime() == null ? null : new Date(onmsNotif.getRespondTime().getTime()));
        notif.setSentTime(onmsNotif.getPageTime() == null ? null : new Date(onmsNotif.getPageTime().getTime()));
        notif.setServiceName(onmsNotif.getServiceType() == null ? "" : onmsNotif.getServiceType().getName());

        if (onmsNotif.getRespondTime() == null) {
            if (onmsNotif.getPageTime().before(new Date(System.currentTimeMillis() - (15 * 60 * 1000)))) {
                notif.setSeverity("Critical");
            } else {
                notif.setSeverity("Minor");
            }
        } else {
            notif.setSeverity("Normal");
        }
        
        return notif;
    }
    */
    
    public Notification[] getNotificationsForSet(SurveillanceSet set) {
        List<Notification> notifications = new ArrayList<Notification>();
        
        Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - (15 * 60 * 1000));
        Date oneWeekAgo = new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000));
        
        notifications.addAll(convertOnmsNotificationToNotification(getNotificationsWithCriterion(set, Order.desc("notification.pageTime"), Restrictions.isNull("notification.respondTime"), Restrictions.le("notification.pageTime", fifteenMinutesAgo)), "Critical"));
        notifications.addAll(convertOnmsNotificationToNotification(getNotificationsWithCriterion(set, Order.desc("notification.pageTime"), Restrictions.isNull("notification.respondTime"), Restrictions.gt("notification.pageTime", fifteenMinutesAgo)), "Minor"));
        notifications.addAll(convertOnmsNotificationToNotification(getNotificationsWithCriterion(set, Order.desc("notification.pageTime"), Restrictions.isNotNull("notification.respondTime"), Restrictions.gt("notification.pageTime", oneWeekAgo)), "Normal"));

        
        return notifications.toArray(new Notification[notifications.size()]);
    }
    
    public List<OnmsNotification> getNotificationsWithCriterion(SurveillanceSet set, Order order, Criterion... criterions) {
        OnmsCriteria criteria = new OnmsCriteria(OnmsNotification.class, "notification");
        OnmsCriteria nodeCriteria = criteria.createCriteria("node");
        addCriteriaForSurveillanceSet(nodeCriteria, set);
        nodeCriteria.add(Restrictions.ne("type", "D"));
        for (Criterion criterion : criterions) {
            criteria.add(criterion);
        }
        criteria.addOrder(order);
        
        return m_notificationDao.findMatching(criteria);
    }
    
    public List<Notification> convertOnmsNotificationToNotification(List<OnmsNotification> notifications, String severity) {
        List<Notification> notifs = new ArrayList<Notification>(notifications.size());
        
        for (OnmsNotification notification : notifications) {
            notifs.add(createNotification(notification, severity));
        }
        
        return notifs;
    }


    private Notification createNotification(OnmsNotification onmsNotif, String severity) {
        Notification notif = new Notification();
        notif.setNodeLabel(onmsNotif.getNode().getLabel());
        notif.setResponder(onmsNotif.getAnsweredBy());
        notif.setRespondTime(onmsNotif.getRespondTime() == null ? null : new Date(onmsNotif.getRespondTime().getTime()));
        notif.setSentTime(onmsNotif.getPageTime() == null ? null : new Date(onmsNotif.getPageTime().getTime()));
        notif.setServiceName(onmsNotif.getServiceType() == null ? "" : onmsNotif.getServiceType().getName());
        notif.setSeverity(severity);
        
        return notif;
    }

}

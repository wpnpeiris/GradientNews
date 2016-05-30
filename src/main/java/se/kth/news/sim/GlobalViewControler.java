/**
 * 
 */
package se.kth.news.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import se.sics.kompics.simulator.util.GlobalView;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author pradeeppeiris
 *
 */
public class GlobalViewControler {
	private static GlobalViewControler instance;
	
	private GlobalViewControler() {
		
	}
	
	public static GlobalViewControler getInstance() {
		if(instance == null) {
			instance = new GlobalViewControler();
		}
		
		return instance;
	}
	
	public void setupGlobalView(GlobalView gv) {
		gv.setValue("simulation.leader", null);
    	gv.setValue("simulation.leader_id", "-1");
    	gv.setValue("simulation.leader_selected", false);
    	
    	gv.setValue("simulation.converge_node_count", 0);
		gv.setValue("simulation.converge_rounds", new HashMap<String, Integer>());
		
		gv.setValue("simulation.num_pushes", 0);
    	gv.setValue("simulation.num_pulls", 0);
		gv.setValue("simulation.leader_coverage", new HashSet<String>());
		
		
    	gv.setValue("simulation.num_messages", 0);
		gv.setValue("simulation.news_coverage", new HashSet<String>());
		gv.setValue("simulation.node_knowlege", new HashMap<String, Integer>());
		
	}
	
	public void updateLeaderSection(GlobalView gv, KAddress leader) {
		gv.setValue("simulation.leader_selected", true);
    	gv.setValue("simulation.leader_id", leader.getId().toString());
    	gv.setValue("simulation.leader", leader);
	}
	
	public void updateGlobalOverlayConvergeView(GlobalView gv, String nodeId) {
    	Map<String, Integer> data = gv.getValue("simulation.converge_rounds", HashMap.class) ;
    	if(data.containsKey(nodeId)){
    		data.put(nodeId, data.get(nodeId) + 1);
    	} else {
    		data.put(nodeId, 1);
    	}
    	
        gv.setValue("simulation.converge_rounds", data);
    }
	
	public void updateGlobalConvergeNodeCount(GlobalView gv){
    	gv.setValue("simulation.converge_node_count", gv.getValue("simulation.converge_node_count", Integer.class) + 1);
    	
    }
	
	public void updateGlobalLeaderPullNotificationView(GlobalView gv){
		gv.setValue("simulation.num_pulls", gv.getValue("simulation.num_pulls", Integer.class) + 1);
	}
	
	public void updateGlobalLeaderPushNotificationView(GlobalView gv){
		gv.setValue("simulation.num_pushes", gv.getValue("simulation.num_pushes", Integer.class) + 1);
	}
	
	public void updateGlobalLeaderDisseminationView(GlobalView gv, String nodeId) {
		Set<String> data = gv.getValue("simulation.leader_coverage", HashSet.class);
		data.add(nodeId);
		gv.setValue("simulation.leader_coverage", data);
	}
	
	public void updateGlobalNumMessagesView(GlobalView gv) {
		gv.setValue("simulation.num_messages", gv.getValue("simulation.num_messages", Integer.class) + 1) ;
	}
		
	public void updateGlobalNewsCoverageView(GlobalView gv, String nodeId) {
    	Set<String> data = gv.getValue("simulation.news_coverage", HashSet.class) ;
        data.add(nodeId);
        gv.setValue("simulation.news_coverage", data);
    }
	
	public void updateGlobalNodeKnowlegeView(GlobalView gv, String nodeId, int size) {
    	Map<String, Integer> data = gv.getValue("simulation.node_knowlege", HashMap.class) ;
    	data.put(nodeId, size);
        gv.setValue("simulation.node_knowlege", data);
    }
}

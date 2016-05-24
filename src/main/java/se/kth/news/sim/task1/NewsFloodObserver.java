/**
 * 
 */
package se.kth.news.sim.task1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * @author pradeeppeiris
 *
 */
public class NewsFloodObserver extends ComponentDefinition {
	private static final Logger LOG = LoggerFactory.getLogger(NewsFloodObserver.class);
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	private UUID timerId;
	 
	public NewsFloodObserver(Init init) {
		 subscribe(handleStart, control);
	     subscribe(handleCheck, timer);
	}
	
	Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            schedulePeriodicCheck();
        }
    };
    
    private void schedulePeriodicCheck() {
        long period = config().getValue("newsflood.simulation.checktimeout", Long.class);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        CheckTimeout timeout = new CheckTimeout(spt);
        spt.setTimeoutEvent(timeout);
        trigger(spt, timer);
        timerId = timeout.getTimeoutId();
    }
    
    Handler<CheckTimeout> handleCheck = new Handler<CheckTimeout>() {
    	@Override
        public void handle(CheckTimeout event) {
    		showNewsCoverage();
    		calculateNodeKnowledge();
    	}    		
    };
    
    private void showNewsCoverage() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
		int newsCoverage = gv.getValue("simulation.news_coverage", HashSet.class).size();
		int numNodes = gv.getValue("simulation.num_nodes", Integer.class);
		double percentage = ((newsCoverage * 1.0) / numNodes) * 100;
		LOG.info("Node coverage " + gv.getValue("simulation.news_coverage", HashSet.class).size() + 
				" , Percentage: " + percentage + " , Total Messages: " + gv.getValue("simulation.num_messages", Integer.class));
    }
    
    private void calculateNodeKnowledge() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	int[] nodeKnwowlge = new int[NewsFloodScenario.NUM_MESSAGES + 1];
    	Map<String, Integer> data = gv.getValue("simulation.node_knowlege", HashMap.class);
    	for(int numMsgs : data.values()) {
    		nodeKnwowlge[numMsgs] ++;
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	for(int i = 0; i < nodeKnwowlge.length; i++) {
    		sb.append(i).append(" Messages Node:").append(nodeKnwowlge[i]).append(" | ");
    	}
    	
    	LOG.info("Node knowlwge (" + sb.toString() + " )");
    	
    }
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
    
    public static class Init extends se.sics.kompics.Init<NewsFloodObserver> {

    	public final boolean flag;

        public Init(boolean flag) {
        	this.flag = flag;
        }
    }
}


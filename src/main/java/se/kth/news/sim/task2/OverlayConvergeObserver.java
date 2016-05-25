/**
 * 
 */
package se.kth.news.sim.task2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.sim.task1.NewsFloodScenario;
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
public class OverlayConvergeObserver extends ComponentDefinition {
	private static final Logger LOG = LoggerFactory.getLogger(OverlayConvergeObserver.class);
	private final int GRADIENT_CONSECUTIVE_ROUNDS = 20;
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	private UUID timerId;
	 
	public OverlayConvergeObserver(Init init) {
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
    		GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    		if(gv.getValue("simulation.converge_node_count", Integer.class) == OverlayConvergeScenario.NUM_NODES) {
    			showOverlayConvergeStat();
    			gv.terminate();
    		}
    	}    		
    };
    
    private void showOverlayConvergeStat() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Map<String, Integer> data = gv.getValue("simulation.converge_rounds", HashMap.class);
    	
    	StringBuilder sb = new StringBuilder();
    	int total = 0;
    	for(String nodeId: data.keySet()) {
    		sb.append(nodeId).append(" : ").append(data.get(nodeId)).append(" | ");
    		total += (data.get(nodeId) - GRADIENT_CONSECUTIVE_ROUNDS);
    	}
    	
    	double average = 0;
    	if(data.size() > 0) {
    		average = (total * 1.0) / data.size();
    	}
    	LOG.info("Overlay Converge Rounds (" + sb.toString() + " )");
    	LOG.info("Total (in all nodes) Converge Rounds (" + total + " )");
    	LOG.info("Average Converge Rounds (" + average + " )");
    	
    }
    
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
    
    public static class Init extends se.sics.kompics.Init<OverlayConvergeObserver> {

    	public final boolean flag;

        public Init(boolean flag) {
        	this.flag = flag;
        }
    }
}


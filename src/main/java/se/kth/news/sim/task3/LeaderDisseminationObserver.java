/**
 * 
 */
package se.kth.news.sim.task3;

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
public class LeaderDisseminationObserver extends ComponentDefinition {
	private static final Logger LOG = LoggerFactory.getLogger(LeaderDisseminationObserver.class);
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	private UUID timerId;
	 
	public LeaderDisseminationObserver(Init init) {
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
    		showLeaderCoverage();
    	}    		
    };
    
    private void showLeaderCoverage() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
		int leaderCoverage = gv.getValue("simulation.leader_coverage", HashSet.class).size();
		int numNodes = gv.getValue("simulation.num_nodes", Integer.class);
		double percentage = ((leaderCoverage * 1.0) / numNodes) * 100;
		LOG.info("Leader coverage " + gv.getValue("simulation.leader_coverage", HashSet.class).size() + 
				" , Percentage: " + percentage );
    }
    
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
    
    public static class Init extends se.sics.kompics.Init<LeaderDisseminationObserver> {

    	public final boolean flag;

        public Init(boolean flag) {
        	this.flag = flag;
        }
    }
}


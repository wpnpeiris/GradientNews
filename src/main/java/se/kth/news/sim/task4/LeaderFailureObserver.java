/**
 * 
 */
package se.kth.news.sim.task4;

import java.util.HashSet;
import java.util.Set;
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
public class LeaderFailureObserver extends ComponentDefinition {
	private static final Logger LOG = LoggerFactory.getLogger(LeaderFailureObserver.class);
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	private UUID timerId;
	 
	public LeaderFailureObserver(Init init) {
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
    		if(gv.getValue("simulation.leader_detected", Boolean.class) == true) {
    			Set<String> eligibleLeaders = gv.getValue("simulation.eligible_leaders", HashSet.class);
    			Set<String> detected = gv.getValue("simulation.detected_by", HashSet.class);
    			
    			LOG.info("{} is detected at {}", gv.getValue("simulation.leader_id", String.class), detected);
    			if(detected.containsAll(eligibleLeaders)) {
    				LOG.info("All eligible leaders detect the leader in  {} rounds", gv.getValue("simulation.leader_detected_cycles", Integer.class));
    				gv.terminate();
    			}

    		}
    	}    		
    };
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
    
    public static class Init extends se.sics.kompics.Init<LeaderFailureObserver> {

    	public final boolean flag;

        public Init(boolean flag) {
        	this.flag = flag;
        }
    }
}


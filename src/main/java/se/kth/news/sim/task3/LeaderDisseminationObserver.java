/**
 * 
 */
package se.kth.news.sim.task3;

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
public class LeaderDisseminationObserver extends ComponentDefinition {
	public static final int PUSH_MODE = 1;
	public static final int PULL_MODE = 2;
	private static final Logger LOG = LoggerFactory.getLogger(LeaderDisseminationObserver.class);
	
	
	Positive<Timer> timer = requires(Timer.class);
	Positive<Network> network = requires(Network.class);
	
	private UUID timerId;
	
	private int mode;
	 
	public LeaderDisseminationObserver(Init init) {
		mode = init.mode;
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
		
		
		if(mode == PULL_MODE) {
			if(leaderCoverage == LeaderDisseminationScenario.NUM_NODES) {
				LOG.info("Leader coverage  in {} messages",  gv.getValue("simulation.num_pulls", Integer.class));
				gv.terminate();
			}
		} else if(mode == PUSH_MODE) {
			if(leaderCoverage >= (LeaderDisseminationScenario.NUM_NODES * 0.6)) {
				LOG.info("Leader coverage  in {} messages",  gv.getValue("simulation.num_pushes", Integer.class));
				gv.terminate();
			}
		}
		
    }
    
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
    
    public static class Init extends se.sics.kompics.Init<LeaderDisseminationObserver> {

    	public final int mode;

        public Init(int mode) {
        	this.mode = mode;
        }
    }
}


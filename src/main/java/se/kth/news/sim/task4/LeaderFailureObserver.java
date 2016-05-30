/**
 * 
 */
package se.kth.news.sim.task4;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.network.KAddress;

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
    		if(gv.getValue("simulation.leader_selected", Boolean.class) == true) {
    			LOG.info("{} is selected as leader", gv.getValue("simulation.leader_id", String.class));
    			KAddress leaderNode = gv.getValue("simulation.leader", KAddress.class);
    			System.out.println(">>>>>>>>>> " + leaderNode.getId());
//    			gv.terminate();
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


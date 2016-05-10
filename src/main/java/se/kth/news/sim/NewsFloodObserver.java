/**
 * 
 */
package se.kth.news.sim;

import java.util.HashMap;
import java.util.HashSet;
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
	
	private final int minPings;
    private final int minDeadNodes;
	
    private UUID timerId;
    
	public NewsFloodObserver(Init init) {
		minPings = init.minPings;
        minDeadNodes = init.minDeadNodes;

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
            
            LOG.info(">>>>>>>>>>> " + gv.getValue("simulation.news", HashSet.class).size());
            
//            if(gv.getValue("simulation.pongs", Integer.class) > minPings) {
//                LOG.info("Terminating simulation as the minimum pings:{} is achieved", minPings);
//                gv.terminate();
//            }
//            if(gv.getDeadNodes().size() > minDeadNodes) {
//                LOG.info("Terminating simulation as the min dead nodes:{} is achieved", minDeadNodes);
//                gv.terminate();
//            }
        }
    };

    public static class Init extends se.sics.kompics.Init<NewsFloodObserver> {

    	public final int minPings;
        public final int minDeadNodes;

        public Init(int minPings, int minDeadNodes) {
        	this.minPings = minPings;
            this.minDeadNodes = minDeadNodes;
        }
    }
    
    public static class CheckTimeout extends Timeout {
        public CheckTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
}

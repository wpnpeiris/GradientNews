/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.core.leader;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderSelectComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderSelectComp.class);
    private String logPrefix = " ";
    
    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Negative<LeaderSelectPort> leaderUpdate = provides(LeaderSelectPort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private Comparator viewComparator;
    
    private final int GRADIENT_STABLE_ROUND = 10;
    private int currentRound = 0;
    private boolean gradientNotStablized = true;
    
    
    private INewsItemDAO newItemDAO;
    private NewsView localNewsView;
    private Set<ElectionAck> acks = new HashSet<ElectionAck>();
    
//    private KAddress leaderSelected;
    private boolean callElection = false;
    
    public LeaderSelectComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);
        
        viewComparator = viewComparator;
        newItemDAO = init.newItemDAO;
        updateLocalNewsView();

        subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
        subscribe(handleElection, networkPort);
        subscribe(handleElectionAck, networkPort);
//        subscribe(handleLeaderUpdate, networkPort);
        subscribe(handleInitElection, networkPort);
        subscribe(timeoutHandler, timerPort);
        
    }

    private void updateLocalNewsView() {
    	int newsCount = newItemDAO.size();
        localNewsView = new NewsView(selfAdr.getId(), newsCount);
    }
    
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    
    
    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
        	if(callElection) {
        		callElection = false;
        		callLeaderElection(sample);
        	}
        }
    };
    
    
    
    private void callLeaderElection(TGradientSample sample) {
    	Container<KAddress, NewsView> highUtilNeighbour = getHighUtilNeighbour(sample);
		int neighbourUtilVal = highUtilNeighbour.getContent().localNewsCount;
		if(localNewsView.localNewsCount >= neighbourUtilVal) {
			LOG.info("{}  Node: " + selfAdr.getId() +  " is eligable for leader amoung: {}", logPrefix, sample.getGradientNeighbours());
			for(Object obj: sample.getGradientNeighbours()) {
				Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
				KAddress neighbourAddr = neighbour.getSource();
				KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
				KContentMsg msg = new BasicContentMsg(header, new Election(localNewsView.localNewsCount));
				trigger(msg, networkPort);
			}
			
			startElectionTimeoutTimer();
		}
    }
    private void startElectionTimeoutTimer() {
    	ScheduleTimeout spt = new ScheduleTimeout(5000);
		ElectionTimeout timeout = new ElectionTimeout(spt);
		spt.setTimeoutEvent(timeout);
		trigger(spt, timerPort);
    }

    Handler<ElectionTimeout> timeoutHandler = new Handler<ElectionTimeout>() {
		public void handle(ElectionTimeout event) {
			if(acks.size() == 0) {
				LOG.info("{} Leader is selected as:{} ", logPrefix, selfAdr);
//				leaderSelected = selfAdr;
	            trigger(new LeaderUpdate(), leaderUpdate);
			}
		}
	};
	
    private Container<KAddress, NewsView> getHighUtilNeighbour(TGradientSample sample) {
    	return (Container<KAddress, NewsView>) sample.gradientNeighbours.get(sample.gradientNeighbours.size() - 1);
    }
    
    ClassMatchedHandler handleElection = new ClassMatchedHandler<Election, KContentMsg<?, ?, Election>>() {

        @Override
        public void handle(Election content, KContentMsg<?, ?, Election> container) {
            LOG.debug("{} received Election message at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            if(Integer.valueOf(selfAdr.getId().toString()) >= Integer.valueOf(container.getHeader().getSource().getId().toString())){
	            if(localNewsView.localNewsCount >= content.getUtility()) {
	            		trigger(container.answer(new ElectionAck()), networkPort);		
	            } 
            } 
        }
    };
    
    ClassMatchedHandler handleInitElection = new ClassMatchedHandler<InitElection, KContentMsg<?, ?, InitElection>>() {

        @Override
        public void handle(InitElection content, KContentMsg<?, ?, InitElection> container) {
            LOG.debug("{} received InitElection at:{} ", logPrefix, selfAdr);
            callElection = true;
        }
    };
    
    ClassMatchedHandler handleElectionAck = new ClassMatchedHandler<ElectionAck, KContentMsg<?, ?, ElectionAck>>() {

        @Override
        public void handle(ElectionAck content, KContentMsg<?, ?, ElectionAck> container) {
            LOG.debug("{} received LeaderAck at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            acks.add(content);
        }
    };
    
    public static class Init extends se.sics.kompics.Init<LeaderSelectComp> {

        public final KAddress selfAdr;
        public final Comparator viewComparator;
        public final INewsItemDAO newItemDAO;

        public Init(KAddress selfAdr, Comparator viewComparator, INewsItemDAO newItemDAO) {
            this.selfAdr = selfAdr;
            this.viewComparator = viewComparator;
            this.newItemDAO = newItemDAO;
        }
    }
    
    public static class ElectionTimeout extends Timeout {
		public ElectionTimeout(ScheduleTimeout spt) {
			super(spt);
		}
	}
}

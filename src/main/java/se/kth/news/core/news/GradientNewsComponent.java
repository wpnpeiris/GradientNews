/**
 * 
 */
package se.kth.news.core.news;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.leader.InitElection;
import se.kth.news.core.leader.LeaderInfo;
import se.kth.news.core.leader.LeaderPullNotification;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.sim.GlobalViewControler;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Handler;
import se.sics.kompics.network.Transport;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

/**
 * @author pradeeppeiris
 *
 */
public class GradientNewsComponent extends NewsComp {
	
	private static final Logger LOG = LoggerFactory.getLogger(CroupierNewsComp.class);
    private String logPrefix = " ";
    
    private final int GRADIENT_CONSECUTIVE_ROUNDS = 20;
    private String neighbourIdList = "EMPTY";
    private int gradientRoundFlag = 0;
    private boolean gradientStable = false;
    
    public GradientNewsComponent(Init init) {
    	super(new NewsComp.Init(init.selfAdr, init.gradientOId, init.newItemDAO));
    	
    	subscribe(handleGradientSample, gradientPort);
    	subscribe(handleLeader, leaderPort);
    	subscribe(handleLeaderPullNotification, networkPort);
    	subscribe(handleLeaderInfo, networkPort);
    }
    
    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {   
    		if(!gradientStable) {
    			GlobalViewControler.getInstance().updateGlobalOverlayConvergeView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
    			checkGradientStablity(sample);
    		} else {
    			pullLeaderInfo(sample);
    		}
    		
//    		if(leader != null) {
//    			Push method
//        		disseminateLeaderInfo(sample);	
//        	} 
        }
    };
    
    
    
    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
        	LOG.info("{} Elected Leader {}", logPrefix, selfAdr);
        	GlobalViewControler.getInstance().updateLeaderSection(config().getValue("simulation.globalview", GlobalView.class),  selfAdr.getId().toString());
    
        	leader = selfAdr;
        }
    };
    
    private void checkGradientStablity(TGradientSample sample) {
    	String latestNeighbourIdList = createNeighbourIdList(sample);
		if(latestNeighbourIdList.equals(neighbourIdList)) {
    		gradientRoundFlag++;
    	} else {
    		gradientRoundFlag = 0;
    		neighbourIdList = latestNeighbourIdList;
    	}
		
    	if(gradientRoundFlag == GRADIENT_CONSECUTIVE_ROUNDS) {
    		gradientStable = true;
    		
    		GlobalViewControler.getInstance().updateGlobalConvergeNodeCount(config().getValue("simulation.globalview", GlobalView.class));
    		
    		triggerInitElection();
    	}
    }
    
    
    
    private String createNeighbourIdList(TGradientSample sample) {
    	StringBuilder sb = new StringBuilder();
    	for(Object obj: sample.getGradientNeighbours()) {
    		Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
    		KAddress neighbourAddr = neighbour.getSource();
    		sb.append(neighbourAddr.getId().toString() + "_");
    	}
    	
    	return sb.toString();
    }
    
    private void triggerInitElection() {
    	KHeader header = new BasicHeader(selfAdr, selfAdr, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, new InitElection());
		trigger(msg, networkPort);
    }
    
    private void pullLeaderInfo(TGradientSample sample) {
    	for(Object obj: sample.getGradientNeighbours()) {
    		Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
    		KAddress neighbourAddr = neighbour.getSource();
    		KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
    		KContentMsg msg = new BasicContentMsg(header, new LeaderPullNotification());
    		trigger(msg, networkPort);
    	}
    }
    
	ClassMatchedHandler handleLeaderPullNotification = new ClassMatchedHandler<LeaderPullNotification, KContentMsg<?, ?, LeaderPullNotification>>() {

        @Override
        public void handle(LeaderPullNotification content, KContentMsg<?, ?, LeaderPullNotification> container) {
        	GlobalViewControler.getInstance().updateGlobalLeaderPullNotificationView(config().getValue("simulation.globalview", GlobalView.class));
        	if(leader != null) {
        		LOG.info("{} received LeaderPullNotification at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            	trigger(container.answer(new LeaderInfo(leader)), networkPort);
        	}
        }
    };
    
    ClassMatchedHandler handleLeaderInfo = new ClassMatchedHandler<LeaderInfo, KContentMsg<?, ?, LeaderInfo>>() {
        @Override
        public void handle(LeaderInfo leaderInfo, KContentMsg<?, ?, LeaderInfo> container) {
            LOG.info("{} received LeaderInfo :{} ", logPrefix, leaderInfo.leaderAdr);
            leader = leaderInfo.leaderAdr;
            GlobalViewControler.getInstance().updateGlobalLeaderDisseminationView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
        }
    };
    
	public static class Init extends se.sics.kompics.Init<GradientNewsComponent> {

        public final KAddress selfAdr;
        public final Identifier gradientOId;
        public final INewsItemDAO newItemDAO;

        public Init(KAddress selfAdr, Identifier gradientOId, INewsItemDAO newItemDAO) {
            this.selfAdr = selfAdr;
            this.gradientOId = gradientOId;
            this.newItemDAO = newItemDAO;
        }
    }
}

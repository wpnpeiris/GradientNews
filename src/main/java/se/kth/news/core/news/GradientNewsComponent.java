/**
 * 
 */
package se.kth.news.core.news;

import java.util.List;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.leader.HearbeatRequest;
import se.kth.news.core.leader.HearbeatResponse;
import se.kth.news.core.leader.InitElection;
import se.kth.news.core.leader.LeaderEligable;
import se.kth.news.core.leader.LeaderInfo;
import se.kth.news.core.leader.LeaderPullNotification;
import se.kth.news.core.leader.LeaderPushNotification;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.leader.NewsItemInfo;
import se.kth.news.core.leader.NewsPullNotification;
import se.kth.news.core.leader.ShutdownNode;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.sim.GlobalViewControler;
import se.sics.kompics.Channel;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Start;
import se.sics.kompics.network.Transport;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;

/**
 * @author pradeeppeiris
 *
 */
public class GradientNewsComponent extends NewsComp {
	
	private static final Logger LOG = LoggerFactory.getLogger(GradientNewsComponent.class);
    private String logPrefix = " ";
    
    private final int GRADIENT_CONSECUTIVE_ROUNDS = 20;
    private String neighbourIdList = "EMPTY";
    private int gradientRoundFlag = 0;
    private boolean gradientStable = false;
    
    protected KAddress leader;
    private boolean isLeader = false;
    private boolean eligableForLeader = false;
    private boolean leaderLive = false;
    private boolean leaderAck = false;
    
    public GradientNewsComponent(Init init) {
    	super(new NewsComp.Init(init.selfAdr, init.gradientOId, init.newItemDAO));
    	
    	subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
    	subscribe(handleLeader, leaderPort);
    	subscribe(handleLeaderEligable, leaderEligablePort);
    	subscribe(handleLeaderPullNotification, networkPort);
    	subscribe(handleLeaderPushNotification, networkPort);
    	subscribe(handleLeaderInfo, networkPort);
    	subscribe(handleNewsItem, networkPort);
    	subscribe(handleNewsPullNotification, networkPort);
    	subscribe(newsItemInfo, networkPort);
    	
    	subscribe(handleHearbeatRequest, networkPort);
    	subscribe(handleHearbeatResponse, networkPort);
    	subscribe(handleShutdown, networkPort);
    	
    	subscribe(leaderDetectorHandler, timerPort);
    	subscribe(shutdownTimeoutHandler, timerPort);	
    	
    }
    
    public void tearDown() {
    	 LOG.info("{} Tear down node {} ", logPrefix, selfAdr);
    }
    
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            updateLocalNewsView();
            
        }
    };
    
    private void updateLocalNewsView() {
    	int newsCount = newItemDAO.size();
    	
        localNewsView = new NewsView(selfAdr.getId(), newsCount);
        LOG.debug("{}informing overlays of new view", logPrefix);
        trigger(new OverlayViewUpdate.Indication<>(gradientOId, false, localNewsView.copy()), viewUpdatePort);
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
    		
    		manageNews(sample);
    		
//    		Leader Dissemination in Push mode
//    		This is not in use, continue with pull mecahnism of leader dissemination
//    		if(leader != null) {
//        		pushLeaderInfo(sample);	
//        	} 
        }
    };
    
    private void pushLeaderInfo(TGradientSample sample) {
		for(Object obj: sample.gradientFingers) {
			GlobalViewControler.getInstance().updateGlobalLeaderPushNotificationView(config().getValue("simulation.globalview", GlobalView.class));
			Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
			KAddress neighbourAddr = neighbour.getSource();
			KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
			KContentMsg msg = new BasicContentMsg(header, new LeaderPushNotification(leader));
			trigger(msg, networkPort);
		}
	}
    
    private void manageNews(TGradientSample sample) {
    	handleStageNews();
    	pullNews(sample);	
    }
    
    private void pullNews(TGradientSample sample) {
    	for(Object obj: sample.getGradientNeighbours()) {
    		Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
    		KAddress neighbourAddr = neighbour.getSource();
    		KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
    		KContentMsg msg = new BasicContentMsg(header, new NewsPullNotification());
    		trigger(msg, networkPort);
    	}
    }
    
    private void handleStageNews() {
    	
    	if(leader != null && isNotLeader()) {
    		LOG.debug("{} Node {} Write stage news on Leader {} ", logPrefix, selfAdr, leader);
    		
    		for(NewsItem newsItem : newItemDAO.getAll()) {
    			if(newsItem.isStage()) {
    				newsItem.setStage(false);
    				KHeader header = new BasicHeader(selfAdr, leader, Transport.UDP);
        			KContentMsg msg = new BasicContentMsg(header, newsItem);
        			trigger(msg, networkPort);
    			}
    			
    		}
    	}
    }
    
    private boolean isNotLeader() {
    	if(leader != null && leader.getId().toString().equals(selfAdr.getId().toString())) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }

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
    		
    		triggerGradientStablized();
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
    
    private void triggerGradientStablized() {
    	KHeader header = new BasicHeader(selfAdr, selfAdr, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, new InitElection());
		trigger(msg, networkPort);
    }
    
    private void pullLeaderInfo(TGradientSample sample) {
    	if(leader == null) {
        	GlobalViewControler.getInstance().updateGlobalLeaderPullNotificationView(config().getValue("simulation.globalview", GlobalView.class));
        }
    	
    	for(Object obj: sample.getGradientNeighbours()) {
    		Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
    		KAddress neighbourAddr = neighbour.getSource();
    		KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
    		KContentMsg msg = new BasicContentMsg(header, new LeaderPullNotification());
    		trigger(msg, networkPort);
    	}
    }
    
    ClassMatchedHandler handleLeaderPushNotification = new ClassMatchedHandler<LeaderPushNotification, KContentMsg<?, ?, LeaderPushNotification>>() {

        @Override
        public void handle(LeaderPushNotification content, KContentMsg<?, ?, LeaderPushNotification> container) {
           LOG.info("{} received LeaderUpdate at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
           leader = content.leaderAdr;
           GlobalViewControler.getInstance().updateGlobalLeaderDisseminationView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
        }
    };
    
    ClassMatchedHandler handleNewsPullNotification = new ClassMatchedHandler<NewsPullNotification, KContentMsg<?, ?, NewsPullNotification>>() {

        @Override
        public void handle(NewsPullNotification content, KContentMsg<?, ?, NewsPullNotification> container) {

        	if(leader != null) {
        		LOG.debug("{} received NewsPullNotification at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
        		List<NewsItem> news = newItemDAO.getAll();
        		if(news.size() > 0) {
        			trigger(container.answer(new NewsItemInfo(news)), networkPort);
        		}          	
        	}
        }
    };
    
    ClassMatchedHandler newsItemInfo = new ClassMatchedHandler<NewsItemInfo, KContentMsg<?, ?, NewsItemInfo>>() {
        @Override
        public void handle(NewsItemInfo newsItemInfo, KContentMsg<?, ?, NewsItemInfo> container) {
            LOG.debug("{} received NewsItemInfo :{} from{}  at{}", logPrefix, newsItemInfo.news, container.getHeader().getSource(), selfAdr);
            GlobalViewControler.getInstance().updateGlobalNumMessagesView(config().getValue("simulation.globalview", GlobalView.class));
            for(NewsItem newsItem : newsItemInfo.news) {
            	newItemDAO.save(newsItem);
            }
            GlobalViewControler.getInstance().updateGlobalNodeKnowlegeView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString(), newItemDAO.getDataSize());
            GlobalViewControler.getInstance().updateGlobalNewsCoverageView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());   
        }
    };
    
    ClassMatchedHandler handleNewsItem = new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {
		@Override
		public void handle(NewsItem newsItem, KContentMsg<?, ?, NewsItem> container) {
			LOG.info("{} received NewsItem at{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
			GlobalViewControler.getInstance().updateGlobalNumMessagesView(config().getValue("simulation.globalview", GlobalView.class));
			if (newItemDAO.cotains(newsItem)) {
				LOG.debug("{} received {} already exists", logPrefix, newsItem);
			} else {
				newsItem.setStage(true);
				newItemDAO.save(newsItem);
				GlobalViewControler.getInstance().updateGlobalNewsCoverageView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
				GlobalViewControler.getInstance().updateGlobalNodeKnowlegeView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString(), newItemDAO.getDataSize());
			}

		}
	};
	
	ClassMatchedHandler handleLeaderPullNotification = new ClassMatchedHandler<LeaderPullNotification, KContentMsg<?, ?, LeaderPullNotification>>() {

        @Override
        public void handle(LeaderPullNotification content, KContentMsg<?, ?, LeaderPullNotification> container) {

        	if(leader != null) {
        		LOG.debug("{} received LeaderPullNotification at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            	trigger(container.answer(new LeaderInfo(leader)), networkPort);
        	}
        }
    };
    
    ClassMatchedHandler handleLeaderInfo = new ClassMatchedHandler<LeaderInfo, KContentMsg<?, ?, LeaderInfo>>() {
        @Override
        public void handle(LeaderInfo leaderInfo, KContentMsg<?, ?, LeaderInfo> container) {
            LOG.debug("{} received LeaderInfo :{} ", logPrefix, leaderInfo.leaderAdr);
            leader = leaderInfo.leaderAdr;
            leaderLive = true;
            
            GlobalViewControler.getInstance().updateGlobalLeaderDisseminationView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
        }
    };
    
    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
        	LOG.info("{} Elected Leader {}", logPrefix, selfAdr);
        	GlobalViewControler.getInstance().updateLeaderSection(config().getValue("simulation.globalview", GlobalView.class),  selfAdr);
        	GlobalViewControler.getInstance().removeLeaderFromEligibleList(config().getValue("simulation.globalview", GlobalView.class),  selfAdr.getId().toString());
        	leader = selfAdr;
        	isLeader = true;
        }
    };
    
    Handler handleLeaderEligable = new Handler<LeaderEligable>() {
        @Override
        public void handle(LeaderEligable event) {
        	LOG.debug("{} Eligable Leader {}", logPrefix, selfAdr);
        	eligableForLeader = true;
        	GlobalViewControler.getInstance().updateEligibleLeaders(config().getValue("simulation.globalview", GlobalView.class),  selfAdr.getId().toString());
        	startLeaderDetectorTimer();
        }
    };
    
    private void startLeaderDetectorTimer() {
    	ScheduleTimeout spt = new ScheduleTimeout(5000);
    	LeaderDetectorTimeout timeout = new LeaderDetectorTimeout(spt);
		spt.setTimeoutEvent(timeout);
		trigger(spt, timerPort);
    }
    
    Handler<LeaderDetectorTimeout> leaderDetectorHandler = new Handler<LeaderDetectorTimeout>() {
		public void handle(LeaderDetectorTimeout event) {
			
			if(leader == null) {
				startLeaderDetectorTimer();
				return;
			} 
			
			if(isNotLeader()) {
				if(leaderAck) {
					leaderAck = false;
					LOG.debug("{} Send HearbeatRequest to leader :{} from:{}", logPrefix, leader, selfAdr);
					KHeader header = new BasicHeader(selfAdr, leader, Transport.UDP);
		    		KContentMsg msg = new BasicContentMsg(header, new HearbeatRequest());
		    		trigger(msg, networkPort);
		    		
		    		
		    		LOG.debug("{} Start leader detection timer :{}", logPrefix, selfAdr);
		    		startLeaderDetectorTimer();
				} else {
					LOG.info("{} Detect leader failure at :{} and initiate election", logPrefix, selfAdr);
					gradientStable = false;
					GlobalViewControler.getInstance().updateLeaderDetection(config().getValue("simulation.globalview", GlobalView.class),  selfAdr.getId().toString());
				}
			}
			
			GlobalViewControler.getInstance().updateLeaderDetectionCycles(config().getValue("simulation.globalview", GlobalView.class),  selfAdr.getId().toString());
		}
	};
	
    ClassMatchedHandler handleHearbeatRequest = new ClassMatchedHandler<HearbeatRequest, KContentMsg<?, ?, HearbeatRequest>>() {

        @Override
        public void handle(HearbeatRequest content, KContentMsg<?, ?, HearbeatRequest> container) {
            LOG.debug("{} received HearbeatRequest message at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            trigger(container.answer(new HearbeatResponse()), networkPort);		
        }
    };
    
    ClassMatchedHandler handleHearbeatResponse = new ClassMatchedHandler<HearbeatResponse, KContentMsg<?, ?, HearbeatResponse>>() {

        @Override
        public void handle(HearbeatResponse content, KContentMsg<?, ?, HearbeatResponse> container) {
            LOG.debug("{} received HearbeatResponse message at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            leaderAck = true;	
        }
    };
    
    
    ClassMatchedHandler handleShutdown = new ClassMatchedHandler<ShutdownNode, KContentMsg<?, ?, ShutdownNode>>() {

        @Override
        public void handle(ShutdownNode content, KContentMsg<?, ?, ShutdownNode> container) {
            LOG.info("{}  received ShutdownNode message at:{} ", logPrefix, selfAdr);
            startShutdownNodeTimer();
        }
    };
	
    private void startShutdownNodeTimer() {
    	ScheduleTimeout spt = new ScheduleTimeout(10000);
    	ShutdownTimeout timeout = new ShutdownTimeout(spt);
		spt.setTimeoutEvent(timeout);
		trigger(spt, timerPort);
    }
    
    Handler<ShutdownTimeout> shutdownTimeoutHandler = new Handler<ShutdownTimeout>() {
		public void handle(ShutdownTimeout event) {
			LOG.info("{} Shutdown node :{} ", logPrefix, selfAdr);
			
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
	
	public static class LeaderDetectorTimeout extends Timeout {
		public LeaderDetectorTimeout(ScheduleTimeout spt) {
			super(spt);
		}
	}
	
	public static class ShutdownTimeout extends Timeout {
		public ShutdownTimeout(ScheduleTimeout spt) {
			super(spt);
		}
	}
}

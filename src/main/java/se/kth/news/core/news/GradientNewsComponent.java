/**
 * 
 */
package se.kth.news.core.news;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.leader.InitElection;
import se.kth.news.core.leader.LeaderInfo;
import se.kth.news.core.leader.LeaderPullNotification;
import se.kth.news.core.leader.LeaderPushNotification;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.leader.NewsItemInfo;
import se.kth.news.core.leader.NewsPullNotification;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
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
	
	private static final Logger LOG = LoggerFactory.getLogger(GradientNewsComponent.class);
    private String logPrefix = " ";
    
    private final int GRADIENT_CONSECUTIVE_ROUNDS = 20;
    private String neighbourIdList = "EMPTY";
    private int gradientRoundFlag = 0;
    private boolean gradientStable = false;
    
    private List<NewsItem> stagNewsItems = new ArrayList<NewsItem>();
    
    public GradientNewsComponent(Init init) {
    	super(new NewsComp.Init(init.selfAdr, init.gradientOId, init.newItemDAO));
    	
    	
    	subscribe(handleGradientSample, gradientPort);
    	subscribe(handleLeader, leaderPort);
    	subscribe(handleLeaderPullNotification, networkPort);
    	subscribe(handleLeaderPushNotification, networkPort);
    	subscribe(handleLeaderInfo, networkPort);
    	subscribe(handleNewsItem, networkPort);
    	subscribe(handleNewsPullNotification, networkPort);
    	subscribe(newsItemInfo, networkPort);
    	
    	
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
    		
    		handleStageNews();
    		pullNews(sample);
    		
//    		Leader Dissemination in Push mode
//    		This is not in use, continue with pull mecahnism of leader dissemination
//    		if(leader != null) {
//        		pushLeaderInfo(sample);	
//        	} 
        }
    };
    
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
    	
    	if(stagNewsItems.size() > 0 && leader != null && isNotLeader()) {
    		LOG.info("{} Node {} Write stage news on Leader {} ", logPrefix, selfAdr, leader);
    		for(NewsItem newsItem : stagNewsItems) {
    			KHeader header = new BasicHeader(selfAdr, leader, Transport.UDP);
    			KContentMsg msg = new BasicContentMsg(header, newsItem);
    			trigger(msg, networkPort);
    		}
    		
    		stagNewsItems.clear();
    	}
    }
    
    private boolean isNotLeader() {
    	if(leader != null && leader.getId().toString().equals(selfAdr.getId().toString())) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }
    
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
            LOG.info("{} XXXXXXXXX received NewsItemInfo :{} from{}  at{}", logPrefix, newsItemInfo.news, container.getHeader().getSource(), selfAdr);
            for(NewsItem newsItem : newsItemInfo.news) {
            	newItemDAO.save(newsItem);
            }
//            leader = leaderInfo.leaderAdr;
//            GlobalViewControler.getInstance().updateGlobalLeaderDisseminationView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
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
            GlobalViewControler.getInstance().updateGlobalLeaderDisseminationView(config().getValue("simulation.globalview", GlobalView.class), selfAdr.getId().toString());
        }
    };
    
    ClassMatchedHandler handleNewsItem = new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {

		@Override
		public void handle(NewsItem newsItem, KContentMsg<?, ?, NewsItem> container) {
			LOG.info("{} received NewsItem at{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
//			updateGlobalNumMessagesView();
			if (newItemDAO.cotains(newsItem)) {
				LOG.debug("{} received {} already exists", logPrefix, newsItem);
			} else {
				newItemDAO.save(newsItem);
				stagNewsItems.add(newsItem);
//				updateGlobalNewsCoverageView();
//				updateGlobalNodeKnowlegeView();
			}

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

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
package se.kth.news.core.news;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.leader.InitElection;
import se.kth.news.core.leader.LeaderNotification;
import se.kth.news.core.leader.LeaderSelectPort;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.play.Ping;
import se.kth.news.play.Pong;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NewsComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NewsComp.class);
    private String logPrefix = " ";
    
    private final int GRADIENT_CONSECUTIVE_ROUNDS = 20;
    private String neighbourIdList = "EMPTY";
    private int gradientRoundFlag = 0;
    private boolean gradientStable = false;
    
    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Positive<LeaderSelectPort> leaderPort = requires(LeaderSelectPort.class);
    Negative<OverlayViewUpdatePort> viewUpdatePort = provides(OverlayViewUpdatePort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress leader;

    private KAddress selfAdr;
    private Identifier gradientOId;
    private INewsItemDAO newItemDAO;
    //*******************************INTERNAL_STATE*****************************
    private NewsView localNewsView;

    public NewsComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        gradientOId = init.gradientOId;
        newItemDAO = init.newItemDAO;

        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleGradientSample, gradientPort);
        subscribe(handleLeader, leaderPort);
        subscribe(handlePing, networkPort);
        subscribe(handlePong, networkPort);
        subscribe(handleNewsItem, networkPort);
        subscribe(handleLeaderNotification, networkPort);
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

    Handler handleCroupierSample = new Handler<CroupierSample<NewsView>>() {
        @Override
        public void handle(CroupierSample<NewsView> castSample) {
        	if (castSample.publicSample.isEmpty()) {
                return;
            }
        	
        	floodNewsItems(castSample);
        }
    };
        				
    private void floodNewsItems(CroupierSample<NewsView> castSample) {
    	Set<Identifier> croupierSet = castSample.publicSample.keySet();
    	
    	List<NewsItem> newsItems = newItemDAO.getAll();
		for(NewsItem newsItem : newsItems) {
			newsItem.reduceTtl();
			if(newsItem.getTtl() > 0) {
				for(Identifier key: croupierSet) {
					disseminateNewsItem(castSample.publicSample.get(key).getSource(), newsItem.copy());
				}
			}
		}
    }
    
    private void disseminateNewsItem(KAddress neighbour, NewsItem newsItem) {
    	KHeader header = new BasicHeader(selfAdr, neighbour, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, newsItem);
		trigger(msg, networkPort);
    }
    
    
    
    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {   
    		if(!gradientStable) {
//    			updateGlobalOverlayConvergeView();
    			checkGradientStablity(sample);
    		} 
    		
    		if(leader != null) {
        		disseminateLeaderInfo(sample);
        	} 
        }
    };
    
    private void disseminateLeaderInfo(TGradientSample sample) {
    	StringBuffer sb = new StringBuffer();
    	for(Object obj: sample.getGradientNeighbours()) {
			Container<KAddress, NewsView> neighbour = (Container<KAddress, NewsView>) obj;
			KAddress neighbourAddr = neighbour.getSource();
			KHeader header = new BasicHeader(selfAdr, neighbourAddr, Transport.UDP);
    		KContentMsg msg = new BasicContentMsg(header, new LeaderNotification(leader));
    		trigger(msg, networkPort);
    		sb.append(neighbourAddr.getId()).append(", ");
		}

    }
    
    private void updateGlobalOverlayConvergeView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Map<String, Integer> data = gv.getValue("simulation.converge_rounds", HashMap.class) ;
    	String nodeId = selfAdr.getId().toString();
    	if(data.containsKey(nodeId)){
    		data.put(nodeId, data.get(nodeId) + 1);
    	} else {
    		data.put(nodeId, 1);
    	}
    	
        gv.setValue("simulation.converge_rounds", data);
    }
    
    private void updateGlobalConvergeNodeCount(){
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	gv.setValue("simulation.converge_node_count", gv.getValue("simulation.converge_node_count", Integer.class) + 1);
    	
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
//    		updateGlobalConvergeNodeCount();
    		
    		triggerInitElection();
    	}
    }
    
    private void triggerInitElection() {
    	KHeader header = new BasicHeader(selfAdr, selfAdr, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, new InitElection());
		trigger(msg, networkPort);
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
    
    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
        	LOG.info("{} Elected Leader", logPrefix);
        	leader = selfAdr;
        }
    };
    
    ClassMatchedHandler handleNewsItem
    	= new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {

        @Override
        public void handle(NewsItem newsItem, KContentMsg<?, ?, NewsItem> container) {
            LOG.info("{} received {} from:{}", logPrefix, newsItem, container.getHeader().getSource());
            updateGlobalNumMessagesView();
            if(newItemDAO.cotains(newsItem)) {
            	LOG.debug("{} received {} already exists", logPrefix, newsItem);
            } else {
            	newItemDAO.save(newsItem);
            	updateGlobalNewsCoverageView();
            	updateGlobalNodeKnowlegeView();
            }
            
        }
    };
    
    ClassMatchedHandler handleLeaderNotification = new ClassMatchedHandler<LeaderNotification, KContentMsg<?, ?, LeaderNotification>>() {

        @Override
        public void handle(LeaderNotification content, KContentMsg<?, ?, LeaderNotification> container) {
            if(leader == null) {
            	LOG.info("{} XXXX received LeaderUpdate at:{} from:{}", logPrefix, selfAdr, container.getHeader().getSource());
            	leader = content.leaderAdr;
            	updateGlobalLeaderDisseminationView();
            }
        }
    };
    
    private void updateGlobalLeaderDisseminationView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Set<String> data = gv.getValue("simulation.leader_coverage", HashSet.class) ;
        data.add(selfAdr.getId().toString());
        gv.setValue("simulation.leader_coverage", data);
    }
    
    private void updateGlobalNumMessagesView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	gv.setValue("simulation.num_messages", gv.getValue("simulation.num_messages", Integer.class) + 1) ;
    }
    
    private void updateGlobalNewsCoverageView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Set<String> data = gv.getValue("simulation.news_coverage", HashSet.class) ;
        data.add(selfAdr.getId().toString());
        gv.setValue("simulation.news_coverage", data);
    }
    
    private void updateGlobalNodeKnowlegeView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Map<String, Integer> data = gv.getValue("simulation.node_knowlege", HashMap.class) ;
    	data.put(selfAdr.getId().toString(), newItemDAO.size());
        gv.setValue("simulation.node_knowlege", data);
    }
    
    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<Ping, KContentMsg<?, ?, Ping>>() {

                @Override
                public void handle(Ping content, KContentMsg<?, ?, Ping> container) {
                    LOG.info("{}received ping from:{}", logPrefix, container.getHeader().getSource());
                    trigger(container.answer(new Pong()), networkPort);
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, KContentMsg<?, KHeader<?>, Pong>>() {

                @Override
                public void handle(Pong content, KContentMsg<?, KHeader<?>, Pong> container) {
                    LOG.info("{}received pong from:{}", logPrefix, container.getHeader().getSource());
                }
            };

    public static class Init extends se.sics.kompics.Init<NewsComp> {

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

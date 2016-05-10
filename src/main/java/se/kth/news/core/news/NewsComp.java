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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.leader.LeaderSelectPort;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.core.news.data.NewsItemDAO;
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
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NewsComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NewsComp.class);
    private String logPrefix = " ";

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Positive<LeaderSelectPort> leaderPort = requires(LeaderSelectPort.class);
    Negative<OverlayViewUpdatePort> viewUpdatePort = provides(OverlayViewUpdatePort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    private Identifier gradientOId;
    //*******************************INTERNAL_STATE*****************************
    private NewsView localNewsView;
    
    private boolean newsFloodLeader;
    
    public NewsComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        gradientOId = init.gradientOId;
        newsFloodLeader = init.newsFloodLeader;

        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleGradientSample, gradientPort);
        subscribe(handleLeader, leaderPort);
        subscribe(handleNewsItem, networkPort);
        subscribe(handlePing, networkPort);
        subscribe(handlePong, networkPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            updateLocalNewsView();
        }
    };

    private void updateLocalNewsView() {
        localNewsView = new NewsView(selfAdr.getId(), 0);
        LOG.debug("{}informing overlays of new view", logPrefix);
        trigger(new OverlayViewUpdate.Indication<>(gradientOId, false, localNewsView.copy()), viewUpdatePort);
    }

    Handler handleCroupierSample = new Handler<CroupierSample<NewsView>>() {
        @Override
        public void handle(CroupierSample<NewsView> castSample) {
            if (castSample.publicSample.isEmpty()) {
                return;
            }

            if(newsFloodLeader && NewsItemDAO.getInstance().isEmpty()) {
            	LOG.info("Generate News Item...");
            	NewsItem newsItem = new NewsItem("NEWS001", "TEST NEWS");
            	NewsItemDAO.getInstance().add(newsItem);
            }
            
            Iterator<Identifier> it = castSample.publicSample.keySet().iterator();
            KAddress partner = castSample.publicSample.get(it.next()).getSource();
            
            
            if(!NewsItemDAO.getInstance().isEmpty()) {
            	NewsItem newsItem = NewsItemDAO.getInstance().get("NEWS001");
            	KHeader header = new BasicHeader(selfAdr, partner, Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, newsItem);
                trigger(msg, networkPort);
            }
            
            
        }
    };

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
        }
    };

    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
        }
    };

    ClassMatchedHandler handleNewsItem = new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {
    	@Override
		public void handle(NewsItem content, KContentMsg<?, ?, NewsItem> container) {
			LOG.info("{} received NewsItem from:{}", logPrefix, container.getHeader().getSource());
			NewsItemDAO.getInstance().add(content);
			
          GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
          Set<String> data = gv.getValue("simulation.news", HashSet.class) ;
          data.add(selfAdr.getId().toString());
          gv.setValue("simulation.news", data);
          
          
//			trigger(container.answer(new Pong()), networkPort);
		}
    };
    
	ClassMatchedHandler handlePing = new ClassMatchedHandler<Ping, KContentMsg<?, ?, Ping>>() {

		@Override
		public void handle(Ping content, KContentMsg<?, ?, Ping> container) {
			LOG.info("{}received ping from:{}", logPrefix, container.getHeader().getSource());
			trigger(container.answer(new Pong()), networkPort);
		}
	};

	ClassMatchedHandler handlePong = new ClassMatchedHandler<Pong, KContentMsg<?, KHeader<?>, Pong>>() {

		@Override
		public void handle(Pong content, KContentMsg<?, KHeader<?>, Pong> container) {
			LOG.info("{}received pong from:{}", logPrefix, container.getHeader().getSource());
		}
	};

    public static class Init extends se.sics.kompics.Init<NewsComp> {

        public final KAddress selfAdr;
        public final Identifier gradientOId;
        public final boolean newsFloodLeader;

        public Init(KAddress selfAdr, Identifier gradientOId, boolean newsFloodLeader) {
            this.selfAdr = selfAdr;
            this.gradientOId = gradientOId;
            this.newsFloodLeader = newsFloodLeader;
        }
    }
}


Gradient News
============
*This is a project assignment for the course Distributed Computing, Peer-to-Peer and GRIDS (ID2210) in KTH.  The project’s goal is to build a reliable data(News) dissemination service on Gradient Topology using Kompics framework.[3]*

Introduction
------------------
The coordination of decentralized services is a vital requirement in any distributed system. In this project, a leader selection protocol on Gradient[1] overlay network is used in coordination of the news dissemination service. The properties of Gradient overlay network are considered for an optimized news dissemination protocol.

The project is carried out in tasks. In task 1, the dissemination of news is evaluated over an unstructured network, using flood/epidemic approach. In task 2, a leader selection protocol is build based on gradient overlay network. That is, once the gradient overlay is stabilized, a set of nodes selected as eligible for leader from center of the network. A revised model of Bully algorithm is applied on these eligible leader nodes to agree on a single leader.

Once the leader node is selected, the protocol for leader and news dis- semination are implemented in task 3.
Finally in task 4, leader failures are handled for more reliable news dissemination service.

System Overview
------------------------
Figure 1 depicts the overall system design. It is based on the project skeleton given in https://github.com/Decentrify/id2210-vt16.git.  As it is in the diagram, the base core components are Gradient and Croupier modules come with KompicsToolbox (https://github.com/Decentrify/KompicsToolbox.git).

![System Overview](https://lh3.googleusercontent.com/-A3Clr1k_nIQNtu9h1uH4mp0I3C1_mrN2uFuujcUU2xzgJw8aFgY2cCIaRznCjlDwwf1xH_n=s0 "System Overview" )

*NewsComp* (se.kth.news.core.news.NewsComp) is enhanced with interface type *INewsItemDAO* (se.kth.news.core.news.data.INewsItemDAO) and object type *NewsItem*(se.kth.news.core.news.data.NewsItem). The basic idea is to model a Fail Recovery abstraction for NewsComp. That is, NewsComp uses INewsItemDAO to read/write NewsItem objects from some reliable local storage. The implementation of INewsItemDAO can be a local file system or a database system, which will be provided in a separated component that will be hosted within *HostMngr*.

The implementation of news persistence component that adhere to *INewsItemDAO*, is not considered in this project. But this separation of data handler with *INewsItemDAO* has other important advantages. It allows to switch in between different mock implementation of *INewsItemDAO* on different Simulation scenarios. For example, Simulation for Gradient Overlay Convergence is provided with an implementation of *INewsItemDAO* that mocks the number of news items a node has.


Also NewsComp is extended into two separate component as *CroupierNewsComp*(se.kth.news.core.news.CroupierNewsComp) and *GradientNewsComponent*(se.kth.news.core.news.GradientNewsComponent) to evaluate unstructured and structured network overlays separately.

Each given tasks in the project are evaluated in separate Simulation Scenarios/Observers. To simplify the use of *GlobalView (se.sics.kompics.simulator.util.GlobalView ) in Observers, a centralized global view variable handler is introduced as a singalton object, *GlobalViewControler* (se.kth.news.sim.GlobalViewControler).

System Abstractions and Implementation
----------------------------------------------------------
## News Flood in unstructured network
We first evaluate the News dissemination on an unstructured network, which is based on *Croupier* [2] peer sampling service. It is similar to Gnutella that uses query flood approach.

Similar to Gnutela, *NewsItems* are disseminated with a TTL value that determines how many hops (nodes) a NewsItem can be disseminated before it expires. Algorithm 1 depicts the algorithm for news dissemination over unstructured network where neighbors are provided with croupier peer sampling service.

![enter image description here](https://lh3.googleusercontent.com/-xjcqGv5vmsM/V8ASEAunA9I/AAAAAAAABWs/dI_yvNBP1C0INYLW7MH_YJ-Ypa_gLNv1QCLcB/s0/1.png "1.png")
As it is in Algorithm 1, upon each CroupierSamples, CroupierNewsComp reads its all non-expired news items and forward to all neigbours given in the CroupierSample.
Two experiments are carried out within the simulation scenario *NewsFloodScenario*(se.kth.news.sim.task1.NewsFloodScenario) to analyze the influence of network size and TTL on news coverage and node knowledge.
Figure 2 shows the simulation object model of the experiment. (The same simulation object model is applied in all the other simulation scenarios in this project). NewsFloodClientComp (se.kth.news.sim.task1.NewsFloodClientComp) is introduced as a helper component to generate NewsItem in simulation sce- narios.

![enter image description here](https://lh3.googleusercontent.com/UwzUZBO9wQW0sNgf1xsRDwyiUwjDgRygxGhs19iahotxsC-y6OQ2bjj_Yd9bmUbDGybLqCux=s0 "News Flood Simulation Component Overview")

## News Coverage
News Coverage determines the percentage of nodes that received each of the NewsItems. NewsFloodScenario is used to analyze the behaviour of News Coverage on different Network Sizes and TTLs.

In NewsFloodScenario, a single NewsItem is spawned at NewsFloodClientComp. Then NewsItem is handled and disseminated according to Algorithm 1. GlobalView variables simulation.news coverage and simulation.num messages are updated in each croupier cycle for every nodes. NewsFloodObserver periodically checks for these variable log out the result.

The experiment is carried out with different TTL and different size of networks. Figure 4 (a) shows the result of news coverage over different TTL values.  

![enter image description here](https://lh3.googleusercontent.com/jDmFVd5jiaS-uHRpirhH18wdN6wuVVpoKyOp6LnfxU94UDluSkGA9HkzOjgb5RNEnXUenuRe=s450 "News Coverage over TTL")

As it can be clearly seen in the diagram, increasing TTL value has similar behavior on any size of networks. Changing TTL value from 4 to 5 achieves 40% of improvements in news coverage. TTL value over 6 provides almost 90% of network coverage on any size of network. Increasing TTL value higher than 6 reaches to 100% news coverage slowly but has a trade- off with number of messages it generated. This is clearly seen in Figure 4 (b). 

![enter image description here](https://lh3.googleusercontent.com/M5XhbOKWkQ0JosaHpp3a0ywoC6CXIRUSERGDa2KxaYCjPtgNOqpP2YypGFZjOqR-IGtD3OX-=s450 "News coverage vs Number of messages over TTL")

According to the diagram, an exponential growth in number of messages can be seen in incremental of TTL. Therefore, setting up TTL value 6-7 provides a more optimized news coverage in croupier news dissemination service.

##Node Knowledge
Node knowledge, the percentage of messages (NewsItem) that reach at each nodes is also analyzed on an unstructured network. The same Simulator scenario NewsFloodScenario is used with multiple messages spawned from NewsFloodClientComp. NewsFloodObserver simply captures the percentage of NewsItem received at each node with use of simulation.node knowlege GlobalView variable. Figure 4 shows the result with network size of 100 and 500 respectively. 

![enter image description here](https://lh3.googleusercontent.com/SKW0jbzC_dyaw-jaQYsFhyjPgwWa-uFuLbV_vAdOJvn_jtw8qgIp_SSbD2IsQEeL-WRbue7P=s450 "100 Nodes")

![enter image description here](https://lh3.googleusercontent.com/FKJ-GSCdoBtUPndMIO0w0eEryNC8gh0nFdYaDcqXM3sd3tJCgBT08nGf6sEtvsvWWrokFjit=s450 "500 Nodes")
As it can be seen in the diagram both network size has a similar behavior of node knowledge over different TTL values. It shows a binomial like distribution of node knowledge with lower TTL value. But leads to 100% of node knowledge as TTL is increased.

Notes: Two experiments above are carried out with the following Croupier Peer Sampling settings;

> 
selectionPolicy=RANDOM
shuffleSpeedFactor=1
viewSize=10
shuffleSize=5
shufflePeriod=2000
shuffleTimeout=1000
softMax=true
softMaxTemperature=500

Leader Selection
-----------------------
In previous task we evaluated the reliability of news dissemination on unstructured network. In this task, Performance of news dissemination is analyzed on Gradient overlay network. The basic ideas is to use properties of the Gradient overlay network for news dissemination. In Gradient topology the nodes with higher utilities are clustered in the center. The utility in our case is considered as number of NewsItems a node has. Therefore nodes closer to the center of the gradient has more NewsItems. And gradient provides a natural flow of data from center to edges. So, more reliable news dissemination can be achieved with publishing news from center of the gradient.

The challenge of this task is to determine a particular node as the leader from center of the network. This is achived in four steps;

- Gradient stabilization: *Determine when gradient overlay is stabilized.*
- Localize agreement problem: *Determine a group of nodes eligible for the leader.*
- Perform agreement protocol: *Select a leader node among eligible nodes.*
- Disseminate the leader information: *Disseminate the selected leader information across the network.*

##Gradient stabilization
The Leader selection is initiated when the Gradient overlay network is sta- bilized. That is, when nodes with high utilities are grouped center in the network. The Algorithm 2 presents a simple protocol to determine when the gradient is stabilized. 

![enter image description here](https://lh3.googleusercontent.com/-r1Bxf25Zkzg/V8ASM2iFXpI/AAAAAAAABW0/wzCEPLCpm607HzX-WSkPb554V6IffqEjACLcB/s0/2.png "2.png")

As it in Algorithm 2, each nodes (GradientNewsCom- ponent) compare its Gradient sample with previous rounds, and if it was not changed for last consecutive rounds (Number of consecutive rounds are configurable), it will consider the gradient has stabilized and InitElection message is triggered. InitElection message is handled in LeaderSelectComp, which will be explained later.

![enter image description here](https://lh3.googleusercontent.com/TYaVh3X_WP8qoEmF2vH_cr4HEzbl0AFhA1QPQpt4xTq8IZDRfi8lwz3z0-aOMuZx7tOlRl95=s550 "Avg. gossip round for overlay converge")

Figure 5 shows average gradient sample rounds taken at each node for the Gradient converge. The simulation scenario, OverlayConvergeScenario (se.kth.news.sim.task2.OverlayConvergeScenario) collects gradient round in simulation.converge rounds global variable at each node and OverlayConver- geObserver (se.kth.news.sim.task2.OverlayConvergeObserver) evaluates the average round per node for the convergence. As it depicts in the figure 5, re- gardless of the network size, each nodes take average 40 to 50 sample rounds for the gradient to be stabilized.

![enter image description here](https://lh3.googleusercontent.com/TYaVh3X_WP8qoEmF2vH_cr4HEzbl0AFhA1QPQpt4xTq8IZDRfi8lwz3z0-aOMuZx7tOlRl95=s0 "Avg. gossip round for overlay converge")

> Notes: The following Gradient settings are used in the experiments;
> 
oldThreshold=100
viewSize=10
shuffleSize=5
shufflePeriod=2000
shuffleTimeout=1000
softMaxTemperature=500

##Localize agreement problem

Once the network overlay is stabilized, a group of nodes need to be selected for the leader selection protocol.


Once the network overlay is stabilized, a group of nodes need to be selected for the leader selection protocol.
When overlay is stabilized each node trigger InitElection message as it is in Algorithm 2. This message initializes the leader selection protocol at LeaderSelectComp. When gradient overlay is stabilized, nodes with high utility converge to the center. According to gradient properties, a node’s utility value is less than its gradient neighbors. But at the center, there should be at least one or more nodes whose utility value is equal or higher than it’s gradient neighbors. This property is considered in localize agreement problem in Algorithm 3. 

![enter image description here](https://lh3.googleusercontent.com/-9fSEhwjJA-w/V8ASblqxIYI/AAAAAAAABW8/WXomSLziZ80jSP6CDzat-XLYXUzB54VYwCLcB/s0/3.png "3.png")

As it is given in the algorithm if node’s utility value is higher than any of its gradient neighbor’s utility value, then it is marked itself as ’Eligible for Leader’ in eligableForLeader variable. Then it triggers LeaderEligibaleNotification on its gradient neighbors to flag them- selves as also ’Eligible for Leader’. In this way, Algorithm 3 collects a set of nodes as ’Eligible for Leader’ from center of the gradient.

Having flag neighbors of highly utilized node as ’Eligible for Leader’ ensures a group of nodes will always be selected as ’Eligible for Leader’ rather than a single one.


##Leader Selection Protocol

According to Algorithm 3, it always guarantee a set of nodes in the middle of Gradient network are marked as ’Eligible for Leader’. Then leader selection protocol is build among these nodes as it is given in Algorithm 4. 

![enter image description here](https://lh3.googleusercontent.com/-42Fu1SjgQV0/V8ASrTx1WrI/AAAAAAAABXI/KeycD3K2g-ovjizB7HpofLZqQ5c5jj3FwCLcB/s0/4.png "4.png")

It is a refined model of Bully Algorithm. Our algorithm fully depends on the properties of Gradient overlay network. That is, nodes flagged as ’Eligible for Leader’ do not know each other. And it does not use another message level of communication to build this knowledge. Instead, it depends on its own gradient neighbor set. That is, if a node is ’Eligible for Leader’ it triggers Election message on its gradient neighbors. This guarantees, the Election message is received at all nodes flagged as ’Eligible for Leader’.

Once Election message is triggered on node’s gradient neighbors , Elec- tionTimeout timer is started. Upon ElectionTimeout, it checks for the num- ber of ElectionAck has received. A node is selected as the leader if no ElectionAck received from others. That is, upon Election message a node acknowledges it with ElectionAck message only when its id is higher than the node id given in the Election message. This is possible as all node has an unique id and as it is comparable with each others.

![enter image description here](https://lh3.googleusercontent.com/pd3u1BuyZOT1UUG4_vy0M684gjNXkmJ2ADC0TxmlYcu_uVpAjuP2gs6fXPR4RnvBhz6rUL6s=s650 "Leader Selection with Bully Algorithm")
Figure 6 shows an example of standard bully algorithm, where message complexity is O(N2). In our modified algorithm, the election is carried out only with few nodes in the gradient center and it always guarantee a single leader is selected. This is verified in LeaderSelectionScenario and LeaderSelectionObserver

Leader Dissemination
-------------------------------
Once the leader is selected, leader information should be disseminated for all the other nodes. There are two mechanism to achieve this, Push and Pull mechanism.

Both pull and push mechanism are implemented in GradientNewsComponent, and evaluated for their performance.

##Push Mechanism

Once the leader is selected, Leader have to disseminate its information. In gradient topology, all nodes are connected with neighbors with high utility than itself. Leader cannot simply push its information to its neighbors as it would converge to itself. Fingers given in Gradient sample is used as the the first place for leader to push its information. Algorithm 5 depicts Leader dissemination algorithm in push model.

![enter image description here](https://lh3.googleusercontent.com/-MjNpiwC0mX0/V8ASyMcV1hI/AAAAAAAABXQ/hAf9jEy9vR0_8dBpOIZNRnDjM7IQpkl4QCLcB/s0/5.png "5.png")

It is experienced with the simulation LeaderDisseminationScenario that leader coverage vary on random fingers and does not guarantee 100% news coverage always.


##Pull Mechanism

In pull mechanism, each nodes ask for leader info from it’s neighbors at each gradient cycle. This mechanism guarantee the pull request reaches the leader as with nature of Gradient topology. The Algorithm 6 depicts the protocol for push mechanism.

![enter image description here](https://lh3.googleusercontent.com/-Nfnr31IfuGw/V8AS8lCPxDI/AAAAAAAABXY/BBCHbMP4aIsZ2Q9SSgPDmfH37RTeBQmeACLcB/s0/6.png "6.png")

LeaderDisseminationScenario/LeaderDisseminationObserver analyzes num- ber of pull round that covers the network with leader information. The figure
7 presents the average pull round that reaches 100% leader coverage at dif- ferent network sizes. It shows a slight exponential growth of pull rounds with respect to the network size in leader dissemination.

![enter image description here](https://lh3.googleusercontent.com/K7aK-D3yS-cIdkgKBr9Ot9pwR6zQLptyzO54IRs9IoLrKlt7Khq6mSYh9yiV7QEzDIXSeDIn=s450 "Avg. pull round in Leader dissemination")

News Dissemination
----------------------------
In task 1, news dissemination on an unstructured network was evaluated. In this task, properties of Gradient topology is used for the news dissemination. That is, news are written at the leader, and other nodes periodically pull from its neighbors. Algorithm 7 shows the news dissemination protocol.

![enter image description here](https://lh3.googleusercontent.com/-TNYycraNo3s/V8ATBmllwcI/AAAAAAAABXk/Vvnl-dWDOrIyiNLKB_7dtyARNzIKeewngCLcB/s0/7.png "7.png")

As it is given in the algorithm, NewsItems are forwarded to the leader node. Upon gradient samples, news are pulled from its gradient neighbors with NewsPullNotification message. As it is in task 1, News coverage and Node knowledge are evaluated in News Dissemination protocol (Pull mechanism). Both experiments are handled in NewsDissemination- Scenario /NewsDisseminationObserver.

##News Coverage

News Coverage determines the percentage of nodes that received each of the NewsItems. It was observed in task 1, 100% news coverage is not always possible on unstructured network. But news dissemination on Gradient overlay (pull mechanism) always guarantee 100% news coverage.

![enter image description here](https://lh3.googleusercontent.com/_ao_PNt0dP-66A6SKJV6Z9LSrvZyKNpR4ym20o_ASw0Ka2VkXlZlPPXb-B9xdtJBo6MMAl3k=s450 "Avg. pull round for News dissemination")

Figure 8 shows total number of pull round issued to achieve 100% news coverage. As it can see in the digram, it shows a linear growth of pull rounds with number of nodes in the network. This is evaluated in NewsDissemina-tionObserver, where it captures individual nodes that receives the NewsItem in simulation.news coverage global variable.

##Node Knowledge

Node knowledge, the percentage of messages (NewsItem) that reach at each nodes is also experimented on gradient overlay network. In similar way as in task 1, NewsGradientClientComp writes multiple messages at differ- ent nodes. NewsDisseminationObserver captures number of pull round re- quired to achieve 100% node knowledge in simulation.node knowlege global variable.

![enter image description here](https://lh3.googleusercontent.com/-Nu689H6z2Nc/V8APKS_TA9I/AAAAAAAABWc/a0_IQmByF-AS1NakCRtZQGgkzHZBtq9WwCLcB/s450/pull_node_knowledge.png "Total pulls vs No. of Messages")

Figure 9 shows the experiment carried out on a network size of 200 nodes. As it depicts in the diagram the number of messages has no impact on the total pulls required to achieve 100% node knowledge. This is possible as in each pull rounds a node collects (pulls) all messages from its neighbors.


Leader Failure
--------------------
All the previous tasks were implemented with assumption that no node failures and links are perfect.  In real world, nodes can be continuously join/leave the system. In this task GradientNewsComponent is improved to detect the leader failures. In Algorithm 3, set of nodes were identified as ’Eligible for Leader’. LeaderEligablePort is included in GradientNewsCom- ponent to listen on LeaderEligable messages. As it is given in Algorithm 8, upon LeaderEligable message GradientNewsComponent starts detecting the leader by exchanging heartbeat messages. That is, all ’Eligible for Leader’ nodes identified in Algorithm 3 start monitoring the leader once it is elected.

![enter image description here](https://lh3.googleusercontent.com/-Gs_2rs37Uo0/V8ATPgL417I/AAAAAAAABXs/--mt5bCkCzQa1mbyhFF6rdykh24gWTyxQCLcB/s0/8.png "8.png")

##Detect Leader node
Algorithm 8 mainly detects the failure of a leader node. As it depicts in Algorithm 8, upon LeaderEligable message GradientNewsComponent sets LeaderDetectorTimeoutTimer to trigger in predefined time period. When LeaderDetectorTimeout event is triggered, it first checks for leader availability. If leader is not available yet, LeaderDetectorTimeout is set for another round again. If leader is available it sends a HearbeatRequest to the leader. The leader will be responded with HearbeatResponse message, which set leaderAck to true. In failure of a leader, leaderAck will be remained false, and it will be detected in next LeaderDetectorTimeout. That is, the algo- rithm continuously check the liveness of leader exchanging heartbeat mes- sages, upon a leader failure the algorithm causes GradientNewsComponent to check the gradient stability and initiate a new leader selection process.

##Replace faulty leader with new leader

There is no changes in the Leader dissemination protocol, as it is in Al- gorithm 6, each node keeps pulling leader information from its gradient neighbors. Upon a new leader it will be simply over written with new leader given in LeaderInfo message. This guarantees, the new leader information eventually available on all nodes.
The simulation scenario LeaderFailureScenario is implemented to ana-
lyze behavior on leader failure. LeaderFailureScenario is modified (imple- mentation of INewsItemDAO) to select node with higher Id to be selected as the leader. Then KillNodeEvent is issued on the leader node. According to LeaderFailureObserver, it was observered that all ’Eligible for Leader’ nodes detects the failure of Leader.
Note: A leader failure was simulated with use of KillNodeEvent from LeaderFailureScenario. Our algorithm is fully depended on the gradient properties. According to the algorithm the new leader selection starts with checking gradient stability. But, it was experienced the faulty nodes never get removed from the gradient samples. Therefore new leader selection was evaluated assuming the faulty nodes as a newly join node, even though it has same id from previous leader.

Enhance News Dissemination
-----------------------------------------

News dissemination in Algorithm 7 does not consider of leader failures. This is further improved in Algorithm 9. 

![enter image description here](https://lh3.googleusercontent.com/-ChmH-hwCr1s/V8ATWwnTlKI/AAAAAAAABX8/ZbidOEPkrmg9L2azIqx5MXkMkTO1VGfxACLcB/s0/9.png "9.png")

According to our protocol all ’Eligible for Leader’ nodes are closed to the Leader node. And periodic pull mechanism guaranteed ’Eligible for Leader’ nodes always updated with latest news from the Leader node. Also upon a leader failure, the new leader will be elected from the ’Eligible for Leader’ group. This guaranteed leader news are always replicated among ’Eligible for Leader’ nodes. That is, Leader’s news are replicated within center of the gradient.
Algorithm 9 is improved so that it could handle news items during un- availability of the leader. That is, NewsItems are always flagged as ’staging’ until it is captured in the leader. On each gradient cycles, a node pull news from its neighbors. This is improved with staging data handling. That is, each node forwards it staging news to leader before it pull news from the gradient neighbors. And it waits for acknowlege from leader before staging flag is removed. This guarantee news items are always written on leader.


Conclusions
----------------

The project is carried out in different tasks to implement a reliable news dissemination service. First, news dissemination on unstructured network was evaluated. According to the result, it shows the impact of TTL value on News coverage and Node knowledge. Having TTL value higher than 6 leads to over 90% of news coverage but exponential growth of flood messages.
A leader selection protocol on Gradient overlay network is implemented in task2. The protocol is highly depend on the gradient properties. The Leader selection algorithm initiates when the gradient overlay is stable. Then group of nodes from the gradient center is identified as ’Eligible for Leader’. A leader node is elected among ’Eligible for Leader’ nodes using the refined model of Bully Algorithm. In this algorithm, non of nodes main- tains the ’Eligible for Leader’ node list. Instead, it uses gradient sample to determine this list.
Once the leader node is elected, Leader dissemination is handled in task 3. Both pull and push mechanism was evaluated and decided to proceed with pull mechanism considering its highly node coverage property. News dissemination is next handled and analyzed both News coverage and nodes coverage.
Finally in task 4, the protocol is enhanced to handled leader failures. News dissemination algorithm is further improved considering node failures.


Appendix
-------------

##Simulation Scenarios

The simulation scenarios related to each tasks can be found under the pack- age se.kth.news.sim.*. Each task has its own Simulation scenario class and Observer class. The following are the main simulation classes.

>
1. task1: 
se.kth.news.sim.task1.NewsFloodScenario se.kth.news.sim.task1.NewsFloodObserver se.kth.news.sim.task1.NewsFloodClientComp
2. task2: 
se.kth.news.sim.task2.LeaderSelectionScenario se.kth.news.sim.task2.LeaderSelectionObserver se.kth.news.sim.task2.OverlayConvergeScenario se.kth.news.sim.task2.OverlayConvergeObserver
3. task3: 
se.kth.news.sim.task3.LeaderDisseminationScenario se.kth.news.sim.task3.LeaderDisseminationObserver se.kth.news.sim.task3.NewsDisseminationScenario se.kth.news.sim.task3.NewsDisseminationObserver se.kth.news.sim.task3.NewsGradientClientComp
4. task4: 
se.kth.news.sim.task1.LeaderFailureScenario se.kth.news.sim.task4.LeaderFailureObserver



References
---------------

 1. Jan Sacha, Jim Dowling, Raymond Cunningham, and Rene’ Meier Dis- covery of stable peers in a self-organising peer-to-peer gradient topology. In Distributed Applications and Interoperable Systems, pages 70–83. Springer Berlin Heidel- berg, 2006.
 2. Jim Dowling and Amir H Payberah. Shuffling with a croupier: Nat-aware peer-sampling. In Distributed Computing Systems (ICDCS), 2012 IEEE 32nd International Conference on, pages 102–111. IEEE, 2012.
 3. Kompics, SICS Swedish ICT and KTH Royal Institute of Technology. http://kompics.sics.se/. 2015

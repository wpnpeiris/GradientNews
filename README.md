
============

Introduction
------------------
*The project’s goal is to build a reliable data(News) dissemination service on Gradient Topology using Kompics framework.[3]*

The coordination of decentralized services is a vital requirement in any distributed system. In this project, a leader selection protocol on Gradient[1] overlay network is used in coordination of the news dissemination service. The properties of Gradient overlay network are considered for an optimized news dissemination protocol.
The project is carried out in tasks. In task 1, the dissemination of news is evaluated over an unstructured network, using flood/epidemic approach. In task 2, a leader selection protocol is build based on gradient overlay
network. That is, once the gradient overlay is stabilized, a set of nodes selected as eligible for leader from center of the network. A revised model of Bully algorithm is applied on these eligible leader nodes to agree on a single leader.
Once the leader node is selected, the protocol for leader and news dis- semination are implemented in task 3.
Finally in task 4, leader failures are handled for more reliable news dissemination service.

System Overview
------------------------
Figure 1 depicts the overall system design. It is based on the project skeleton given in https://github.com/Decentrify/id2210-vt16.git.  As it is in the dia- gram, the base core components are Gradient and Croupier modules come with KompicsToolbox (https://github.com/Decentrify/KompicsToolbox.git).

![System Overview](https://lh3.googleusercontent.com/-A3Clr1k_nIQNtu9h1uH4mp0I3C1_mrN2uFuujcUU2xzgJw8aFgY2cCIaRznCjlDwwf1xH_n=s0 "System Overview")


*NewsComp* (se.kth.news.core.news.NewsComp) is enhanced with interface type *INewsItemDAO* (se.kth.news.core.news.data.INewsItemDAO) and object type *NewsItem*(se.kth.news.core.news.data.NewsItem). The basic idea is to model a Fail Recovery abstraction for NewsComp. That is, NewsComp uses INewsItemDAO to read/write NewsItem objects from some reliable local storage. The implementation of INewsItemDAO can be a local file system or a database system, which will be provided in a separated component that will be hosted within *HostMngr*.

The implementation of news persistence component that adhere to *INewsItemDAO*, is not considered in this project. But this separation of data handler with *INewsItemDAO* has other important advantages. It allows to switch in between different mock implementation of *INewsItemDAO* on different Simulation scenarios. For example, Simulation for Gradient Overlay Convergence is provided with an implementation of *INewsItemDAO* that mocks the number of news items a node has.


Also NewsComp is extended into two separate component as *CroupierNewsComp*(se.kth.news.core.news.CroupierNewsComp) and *GradientNewsComponent*(se.kth.news.core.news.GradientNewsComponent) to evaluate unstructured and structured network overlays separately.

Each given tasks in the project are evaluated in separate Simulation Scenarios/Observers. To simplify the use of *GlobalView*(se.sics.kompics.simulator.util.GlobalView ) in Observers, a centralized global view variable handler is introduced as a singalton object, *GlobalViewControler* (se.kth.news.sim.GlobalViewControler).

System Abstractions and Implementation
----------------------------------------------------------
## News Flood in unstructured network
We first evaluate the News dissemination on an unstructured network, which is based on *Croupier* [2] peer sampling service. It is similar to Gnutella that uses query flood approach.

Similar to Gnutela, *NewsItems* are disseminated with a TTL value that determines how many hops (nodes) a NewsItem can be disseminated be- fore it expires. Algorithm 1 depicts the algorithm for news dissemination over unstructured network where neighbors are provided with croupier peer sampling service.
As it is in Algorithm 1, upon each CroupierSamples, CroupierNewsComp reads its all non-expired news items and forward to all neigbours given in the CroupierSample.
Two experiments are carried out within the simulation scenario *NewsFloodScenario*(se.kth.news.sim.task1.NewsFloodScenario) to analyze the influence of network size and TTL on news coverage and node knowledge.
Figure 2 shows the simulation object model of the experiment. (The same simulation object model is applied in all the other simulation scenarios in this project). NewsFloodClientComp (se.kth.news.sim.task1.NewsFloodClientComp) is introduced as a helper component to generate NewsItem in simulation sce- narios.

![enter image description here](https://lh3.googleusercontent.com/UwzUZBO9wQW0sNgf1xsRDwyiUwjDgRygxGhs19iahotxsC-y6OQ2bjj_Yd9bmUbDGybLqCux=s0 "News Flood Simulation Component Overview")

## News Coverage
News Coverage determines the percentage of nodes that received each of the NewsItems. NewsFloodScenario is used to analyze the behaviour of News Coverage on different Network Sizes and TTLs.

In NewsFloodScenario, a single NewsItem is spawned at NewsFloodClientComp. Then NewsItem is handled and disseminated according to Algorithm 1. GlobalView variables simulation.news coverage and simulation.num messages are updated in each croupier cycle for every nodes. NewsFloodObserver periodically checks for these variable log out the result.
The experiment is carried out with different TTL and different size of networks. Figure 4 (a) shows the result of news coverage over different TTL values.  

![enter image description here](https://lh3.googleusercontent.com/jDmFVd5jiaS-uHRpirhH18wdN6wuVVpoKyOp6LnfxU94UDluSkGA9HkzOjgb5RNEnXUenuRe=s0 "News Coverage over TTL")

As it can be clearly seen in the diagram, increasing TTL value has similar behavior on any size of networks. Changing TTL value from 4 to 5 achieves 40% of improvements in news coverage. TTL value over 6 provides almost 90% of network coverage on any size of network. Increasing TTL value higher than 6 reaches to 100% news coverage slowly but has a trade- off with number of messages it generated. This is clearly seen in Figure 4 (b). 

![enter image description here](https://lh3.googleusercontent.com/M5XhbOKWkQ0JosaHpp3a0ywoC6CXIRUSERGDa2KxaYCjPtgNOqpP2YypGFZjOqR-IGtD3OX-=s0 "News coverage vs Number of messages over TTL")

According to the diagram, an exponential growth in number of messages can be seen in incremental of TTL. Therefore, setting up TTL value 6-7 provides a more optimized news coverage in croupier news dissemination service.

##Node Knowledge
Node knowledge, the percentage of messages (NewsItem) that reach at each nodes is also analyzed on an unstructured network. The same Simulator scenario NewsFloodScenario is used with multiple messages spawned from NewsFloodClientComp. NewsFloodObserver simply captures the percentage of NewsItem received at each node with use of simulation.node knowlege GlobalView variable. Figure 4 shows the result with network size of 100 and 500 respectively. As it can be seen in the diagram both network size has a similar behavior of node knowledge over different TTL values. It shows a binomial like distribution of node knowledge with lower TTL value. But leads to 100% of node knowledge as TTL is increased.

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



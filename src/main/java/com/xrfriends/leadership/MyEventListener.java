package com.xrfriends.leadership;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.cluster.leader.event.AbstractLeaderEvent;
import org.springframework.context.ApplicationListener;

class MyEventListener implements ApplicationListener<AbstractLeaderEvent> {

    private final Logger log = LoggerFactory.getLogger(MyEventListener.class);

    @Override
    public void onApplicationEvent(AbstractLeaderEvent event) {
        // do something with OnGrantedEvent or OnRevokedEvent
        log.info("----------------------------------");
        log.info(event.toString());  //[role=leader, context=CuratorContext{role=leader, id=aaa, isLeader=true}, source=org.springframework.cloud.cluster.zk.leader.LeaderInitiator@1ecbba7c]
        log.info("----------------------------------");

        // TODO leader의 경우 역할을 수행할 수 있도록 여기에 코딩하면 됨.
    }
}

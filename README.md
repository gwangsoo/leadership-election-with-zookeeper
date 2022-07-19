# Springboot에서 Zookeeper 기반 리더 선출 방법

- 시스템을 운용할때 장애를 대비해 서버를 여러대 기동하게 되는데, 이 서버중 한 대에서만 특정 작업을 실행해야 하는 경우 어떻게 해야 할까요?
- Database로? File로? Zookeeper로? 다양한 아키텍쳐로 문제를 해결할 수 있지만 Zookeeper를 기반으로 문제를 해결해 봅니다.

## 개발환경
## Zookeeper
### 실행
```bash
docker run --rm --name myzk -d -p 2181:2181 zookeeper
```
### CLI
```bash
docker exec -it myzk zkCli.sh

[zk: localhost:2181(CONNECTED) 0] ls /spring-cloud/leader/leader
[_c_720f769a-6798-4936-84cb-f87b6c859b49-lock-0000000000]
[zk: localhost:2181(CONNECTED) 1] 
```

## Springboot
### build.gradle
```yaml
ext {
    set('springCloudVersion', "2021.0.3")
}

dependencies {
   ...
   implementation 'org.springframework.cloud:spring-cloud-cluster-autoconfigure:1.0.2.RELEASE'
   implementation 'org.springframework.cloud:spring-cloud-cluster:1.0.2.RELEASE'
   implementation 'org.springframework.cloud:spring-cloud-cluster-zookeeper:1.0.2.RELEASE'
   ...
}

dependencyManagement {
  imports {
  mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
  }
}
```

### application.yml
```yaml
spring:
  cloud:
    cluster:
      leader:
        enabled: true
#        id: xxx # 지정해도되고 지정하지 않으면 UUID 자동생성
      zookeeper:
        leader:
          enable: true
          namespace: ecs

logging:
  level:
    ROOT: INFO
    com.xrfriends.leadership: DEBUG
    org.springframework.cloud.cluster: DEBUG
```

### MyEventListener
```java
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
```

### Configuration
```java
@Configuration
public class Config {
    @Bean
    public MyEventListener myEventListener() {
        return new MyEventListener();
    }

    @Bean
    public LoggingListener loggingListener() {
        return new LoggingListener("trace");
    }
}
```

## Test
Java  Instance 를 3개 실행 후 Leader를 강제 종료했을때 다른 Instance가 Leader로 대체되는지 확인

### Instance 생성
#### First Instance
```bash
java -jar ./build/libs/*.jar --server.port=8081

2022-07-19 14:14:11.811  INFO 74129 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : ----------------------------------
2022-07-19 14:14:11.812  INFO 74129 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : AbstractLeaderEvent [role=leader, context=CuratorContext{role=leader, id=68596625-661b-464a-8f7a-f8aa94516ba8, isLeader=true}, source=org.springframework.cloud.cluster.zk.leader.LeaderInitiator@45599594]
2022-07-19 14:14:11.812  INFO 74129 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : ----------------------------------
```

#### Second Instance
```bash
java -jar ./build/libs/*.jar --server.port=8082
```

#### Third Instance
```bash
java -jar ./build/libs/*.jar --server.port=8083
```

### Leader 종료
#### Leader Instance 종료
```bash
java.lang.IllegalStateException: Expected state [STARTED] was [STOPPED]
        at org.apache.curator.shaded.com.google.common.base.Preconditions.checkState(Preconditions.java:823) ~[curator-client-5.1.0.jar!/:na]
        at org.apache.curator.framework.imps.CuratorFrameworkImpl.checkState(CuratorFrameworkImpl.java:423) ~[curator-framework-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.imps.CuratorFrameworkImpl.delete(CuratorFrameworkImpl.java:443) ~[curator-framework-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.locks.LockInternals.deleteOurPath(LockInternals.java:347) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.locks.LockInternals.releaseLock(LockInternals.java:124) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.locks.InterProcessMutex.release(InterProcessMutex.java:154) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.leader.LeaderSelector.doWork(LeaderSelector.java:454) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.leader.LeaderSelector.doWorkLoop(LeaderSelector.java:483) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.leader.LeaderSelector.access$100(LeaderSelector.java:66) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.leader.LeaderSelector$2.call(LeaderSelector.java:247) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at org.apache.curator.framework.recipes.leader.LeaderSelector$2.call(LeaderSelector.java:241) ~[curator-recipes-5.1.0.jar!/:5.1.0]
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515) ~[na:na]
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) ~[na:na]
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) ~[na:na]
        at java.base/java.lang.Thread.run(Thread.java:829) ~[na:na]
2022-07-19 14:19:47.104  INFO 74129 --- [ionShutdownHook] org.apache.zookeeper.ZooKeeper           : Session: 0x1000bbc344f0017 closed
2022-07-19 14:19:47.104  INFO 74129 --- [ain-EventThread] org.apache.zookeeper.ClientCnxn          : EventThread shut down for session: 0x1000bbc344f0017
```

#### Leader 전환 확인
Second Instance로 Leader 전환
```bash
2022-07-19 14:19:47.032  INFO 74302 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : ----------------------------------
2022-07-19 14:19:47.034  INFO 74302 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : AbstractLeaderEvent [role=leader, context=CuratorContext{role=leader, id=d1f7f96a-554f-4b3e-9404-0f1eeaeae71c, isLeader=true}, source=org.springframework.cloud.cluster.zk.leader.LeaderInitiator@712d2033]
2022-07-19 14:19:47.035  INFO 74302 --- [eaderSelector-0] c.xrfriends.leadership.MyEventListener   : ----------------------------------
```

## 결론
- Spring cloud cluster 가 제공하는 방법을 사용하면 단순하며 간단하게 견고한 프로그램을 만들 수 있음.
- 참고 : https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_spring_cloud_cluster

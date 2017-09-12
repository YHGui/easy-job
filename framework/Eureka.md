# 概念

- Register
  - 服务注册
  - 当Eureka客户端像Eureka Server注册时，它提供自身的元数据，比如IP地址，端口，运行状况指示符，主页等。
- Renew：服务续约
  - Eureka客户每隔30秒发送一次心跳来续约。通过续约来告知Eureka Server该Eureka客户仍然存在，没有出现问题。正常情况下，如果Eureka Server在90秒没有收到Eureka客户的续约，会将实例从其注册表中删除，建议不要更改续约间隔。
- Fetch Registries：获取注册列表信息
  - Eureka客户端从服务器获取注册表信息，并将其缓存在本地。客户端会使用该信息查找其他服务，从而进行远程调用。注册列表信息定期（30秒）更新一次。每次饭回注册列表信息可能与Eureka客户段缓存信息不同，Eureka客户端自动处理。如果由于某种原因导致注册列表信息不能及时匹配，Eureka客户端则会重新获取整个注册表信息。 Eureka服务器缓存注册列表信息，整个注册表以及每个应用程序的信息进行了压缩，压缩内容和没有压缩的内容完全相同。Eureka客户端和Eureka 服务器可以使用JSON / XML格式进行通讯。在默认的情况下Eureka客户端使用压缩JSON格式来获取注册列表的信息。
- Cancel：服务下线
  - Eureka客户端在程序关闭时向Eureka服务器发送取消请求。 发送请求后，该客户端实例信息将从服务器的实例注册表中删除。该下线请求不会自动完成，它需要调用以下内容：
    DiscoveryManager.getInstance().shutdownComponent()；
- Eviction 服务剔除
  - 在默认的情况下，当Eureka客户端连续90秒没有向Eureka服务器发送服务续约，即心跳，Eureka服务器会将该服务实例从服务注册列表删除，即服务剔除。

## Eureka高可用架构

![Alt text](https://user-gold-cdn.xitu.io/2017/6/11/398fdaf163f6e7101dee83b76e28ff36?imageView2/0/w/1280/h/960)

从图可以看出在这个体系中，有2个角色，即Eureka Server和Eureka Client。而Eureka Client又分为Applicaton Service和Application Client，即服务提供者和服务消费者。 每个区域有一个Eureka集群，并且每个区域至少有一个eureka服务器可以处理区域故障，以防服务器瘫痪。

Eureka Client向Eureka Server注册，并将自己的一些客户端信息发送Eureka Server。然后，Eureka Client通过向Eureka Server发送心跳（每30秒）来续约服务的。 如果客户端持续不能续约，那么，它将在大约90秒内从服务器注册表中删除。 注册信息和续订被复制到集群中的Eureka Server所有节点。 来自任何区域的Eureka Client都可以查找注册表信息（每30秒发生一次）。根据这些注册表信息，Application Client可以远程调用Applicaton Service来消费服务。

# Register服务注册

服务注册，即Eureka Client向Eureka Server提交自己的服务信息，包括IP地址、端口、service ID等信息。如果Eureka Client没有写service ID，则默认为 ${spring.application.name}。

服务注册其实很简单，在Eureka Client启动的时候，将自身的服务的信息发送到Eureka Server。现在来简单的阅读下源码。在Maven的依赖包下，找到eureka-client-1.6.2.jar包。在com.netflix.discovery包下有个DiscoveryClient类，该类包含了Eureka Client向Eureka Server的相关方法。其中DiscoveryClient实现了EurekaClient接口，并且它是一个单例模式，而EurekaClient继承了LookupService接口。它们之间的关系如图所示。
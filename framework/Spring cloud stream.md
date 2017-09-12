# Spring cloud stream

## 简介

- Spring cloud stream是一个构建消息驱动的微服务框架。基于Spring Boot，用以创建工业级的应用程序，并且Spring Integration提供了和消息代理的连接。Spring cloud stream提供了几个消息中间件的个性化配置，引入发布订阅，消费组和分区的语义概念。
- 添加@EnableBinding注解在程序中，被@StreamListener修饰的方法可以立即连接到消息代理接收流处理事件。@EnableBinding注解使用一个或者多个接口作为参数，接口可以定义输入或输出的channels，Spring cloud stream定义了三个借口Source，Sink，Processor，当然也可以自定义接口。
- @Input定义一个接收消息的输入channel，@Output定义一个发布消息的输出channel，这两个注解支持一个参数作为channel名称，如果没有设置参数则注解修饰的方法名被设置为channel名称。Spring cloud stream会为你创建一个接口的实现，可以通过自动装配在应用中使用。

## 主要概念

- Spring cloud stream提供了很多抽象和基础组件来简化消息驱动型微服务应用

- Spring cloud stream的应用模型

  ![Alt text](http://spring-cloud.io/reference/img/stream-binder.png)

  - Spring cloud stream由一个中立的中间件内核组成。Spring cloud stream会注入输入和输出的channels，应用程序通过这些channels与外界通信，而channels则通过一个明确的中间件Binder与外部brokers连接。

- 绑定抽象

  - Spring cloud stream提供对kafka，rabbitmq的Binder实现，绑定抽象使得SpringCloudStream应用可以灵活的连接到中间件。比如，开发者可以在运行时动态的选择channels连接的目标（可以是kafka topics或RabbitMQ exchanges）。这样的配置可以通过外部配置或任何SpringBoot支持的形式（包括应用参数，环境变量和application.yml或application.properties文件）。
  - Spring cloud stream能自动发现并使用类路径中的binder，因此可以使用不同类型的中间件，只需要在build的时候引入不同的binder。对于更复杂的情况，可以引入多个binders并选择使用哪一个，甚至可以在运行时根据不同的channels选择不同的binder。

- 持久化发布／订阅支持

  ![Alt text](http://spring-cloud.io/reference/img/stream-sensors.png)

  - 应用间通信按照发布-订阅模型，数据通过共享的topics进行广播
  - 数据被发送到一个公共的目标，在目标中，数据可以分别被多个微服务加工。发布-订阅通信模型降低了生产者和消费者的复杂性，并允许新的应用程序被添加到拓扑结构，而不会破坏现有的流程。通过共同的topics做沟通相比点对点的队列更能减少微服务间的耦合。

- 消费者组

  - 虽然发布-订阅模型可以很容易地通过共享topics连接应用程序，但创建一个应用多实例的扩张能力同等重要，当这样做时，应用程序的不同实例被放置在一个竞争的消费者关系中，其中只有一个实例将处理一个给定的消息。Spring cloud stream利用消费者组定义这种行为，灵感来源于Kafka consumer groups。所有订阅指定topics的组都会收到发布数据的一份副本，但是每一个组内只有一个成员会收到该消息。默认情况下，当一个组没有指定时，SpringCloudStream将分配给一个匿名的、独立的只有一个成员的消费组，该组与所有其他组都处于一个发布－订阅关系中。

- 持久性

  - SpringCloudStream一致性模型中，消费者组订阅是持久的，也就是说一个绑定的实现确保组的订阅者是持久的。一旦组中至少有一个成员创建了订阅，这个组就会收到消息，即使组中所有的应用都被停止了，组仍然会收到消息。

- 分区支持

  - SpringCloudStream支持在一个应用程序的多个实例之间数据分区，在分区的情况下，物理通信介质（例如，topic代理）被视为多分区结构。一个或多个生产者应用程序实例将数据发送给多个消费应用实例，并保证共同的特性的数据由相同的消费者实例处理。
  - SpringCloudStream提供了一个通用的抽象，用于统一方式进行分区处理，因此分区可以用于自带分区的代理（如kafka）或者不带分区的代理（如rabbitmq）

## 编程模型

- 声明和绑定通道

  - @EnableBinding触发绑定，将@EnableBinding注解添加到应用的配置类，就可以把一个spring应用转换成SpringCloudStream应用，@EnableBinding注解本身就包含@Configuration注解，会触发SpringCloudStream 基本配置。

  - @EnableBinding注解可以接收一个或多个接口类作为参数，后者包含代表了可绑定构件（一般来说是消息通道）的方法

  - 一个SpringCloudStream应用可以有任意数目的input和output通道，后者通过`@Input`和`@Output`注解在接口中定义。

  - **Source，Sink，Processor**

    最常见的场景中，包含一个输入通道或者包含一个输出通道或者二者都包含，SpringCloudStream提供了三个开箱即用的预定义接口。

- 访问绑定通道

  - 注入已绑定接口：对于每一个绑定的接口，SpringCloudStream将产生一个实现接口的bean，调用这个生成类的@Input或@Output方法，会返回一个相应的channel。
  - 直接注入到通道：绑定的通道也可以直接注入

- 生产和消费消息

  - 可以使用Spring Integration的注解或者SpringCloudStream的@StreamListener注解来实现一个SpringCloudStream应用。@StreamListener注解模仿其他spring消息注解（例如@MessageMapping, @JmsListener, @RabbitListener等），但是它增加了内容类型管理和类型强制特性。

    ​

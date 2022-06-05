package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;

public class Service02Stack extends Stack {
  public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic, Table productEventsDynamo) {
    this(scope, id, null, cluster, productEventsTopic, productEventsDynamo);
  }

  public Service02Stack(final Construct scope, final String id, final StackProps props, final Cluster cluster, SnsTopic productEventsTopic, Table productEventsDynamo) {
    super(scope, id, props);

    final Queue productEventsDlq = Queue.Builder.create(this, "ProductEventsDlq")
        .queueName("product-events-dlq")
        .build();

    final DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
        .queue(productEventsDlq)
        .maxReceiveCount(3)
        .build();

    final Queue productEventsQueue = Queue.Builder.create(this, "ProductEvents")
        .queueName("product-events")
        .deadLetterQueue(deadLetterQueue)
        .build();

    final SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventsQueue).build();
    productEventsTopic.getTopic().addSubscription(sqsSubscription);

    final HashMap<String, String> env = new HashMap<>();
    env.put("AWS_REGION", "us-east-1");
    env.put("AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventsQueue.getQueueName());

    final ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB02")
        .serviceName("service02")
        .cluster(cluster)
        .cpu(512)
        .desiredCount(2)
        .listenerPort(9090)
        .memoryLimitMiB(1024)
        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
            .containerName("aws_project02")
            .image(ContainerImage.fromRegistry("guirodg/aws-local-02:1.4.0"))
            .containerPort(9090)
            .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(LogGroup.Builder.create(this, "Service02Group")
                    .logGroupName("Service02")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .build())
                .streamPrefix("Service02")
                .build()))
            .environment(env)
            .build())
        .publicLoadBalancer(true)
        .build();

    service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
        .path("/actuator/health")
        .port("9090")
        .healthyHttpCodes("200")
        .build());

    final ScalableTaskCount scalableTaskCount = service02.getService().autoScaleTaskCount(EnableScalingProps.builder()
        .minCapacity(2) // Minimo 2 instancias
        .maxCapacity(4) // Maximo 4 instancias
        .build());

    scalableTaskCount.scaleOnCpuUtilization("Service02AutoScaling", CpuUtilizationScalingProps.builder()
        .targetUtilizationPercent(50) // Minha CPU ultrapassar 50% Cria outra instancia
        .scaleInCooldown(Duration.seconds(60)) // Tempo limite para criar nova instancia
        .scaleOutCooldown(Duration.seconds(60)) // Tempo limite para desligar instancia
        .build());

    productEventsQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());
    productEventsDynamo.grantReadWriteData(service02.getTaskDefinition().getTaskRole());
  }
}

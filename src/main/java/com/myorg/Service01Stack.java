package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;

public class Service01Stack extends Stack {
  public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic) {
    this(scope, id, null, cluster, productEventsTopic);
  }

  public Service01Stack(final Construct scope, final String id, final StackProps props, final Cluster cluster, SnsTopic productEventsTopic) {
    super(scope, id, props);

    final HashMap<String, String> env = new HashMap<>();
    env.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://" + Fn.importValue("rds-endpoint") +
        ":3306/aws_project01?createDatabaseIfNotExist=true");
    env.put("SPRING_DATASOURCE_USERNAME", "admin");
    env.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("rds-password"));

    final ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB01")
        .serviceName("service01")
        .cluster(cluster)
        .cpu(512)
        .desiredCount(2)
        .listenerPort(8080)
        .memoryLimitMiB(1024)
        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
            .containerName("aws_project01")
            .image(ContainerImage.fromRegistry("guirodg/aws-local-01:1.1.0"))
            .containerPort(8080)
            .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(LogGroup.Builder.create(this, "Service01Group")
                    .logGroupName("Service01")
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .build())
                .streamPrefix("Service01")
                .build()))
            .environment(env)
            .build())
        .publicLoadBalancer(true)
        .build();

    service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
        .path("/actuator/health")
        .port("8080")
        .healthyHttpCodes("200")
        .build());

    final ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
        .minCapacity(2) // Minimo 2 instancias
        .maxCapacity(4) // Maximo 4 instancias
        .build());

    scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
        .targetUtilizationPercent(50) // Minha CPU ultrapassar 50% Cria outra instancia
        .scaleInCooldown(Duration.seconds(60)) // Tempo limite para criar nova instancia
        .scaleOutCooldown(Duration.seconds(60)) // Tempo limite para desligar instancia
        .build());

    productEventsTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());
  }
}

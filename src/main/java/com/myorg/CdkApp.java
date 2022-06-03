package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;

public class CdkApp {
  public static void main(final String[] args) {
    App app = new App();

    new SnsStack(app, "sns");

    final VpcStack vpc = new VpcStack(app, "Vpc");

    final ClusterStack cluster = new ClusterStack(app, "Cluster", vpc.getVpc());
    cluster.addDependency(vpc);

    final RdsStack rds = new RdsStack(app, "Rds", vpc.getVpc());
    rds.addDependency(vpc);

    final SnsStack snsStack = new SnsStack(app, "Sns");

    final Service01Stack service01 = new Service01Stack(app, "Service01", cluster.getCluster(), snsStack.getSnsTopic());
    service01.addDependency(cluster);
    service01.addDependency(rds);
    service01.addDependency(snsStack);

    app.synth();
  }
}


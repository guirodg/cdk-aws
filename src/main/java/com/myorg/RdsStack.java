package com.myorg;

import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

public class RdsStack extends Stack {
  public RdsStack(final Construct scope, final String id, Vpc vpc) {
    this(scope, id, null, vpc);
  }

  public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
    super(scope, id, props);

    final CfnParameter parameterPassword = CfnParameter.Builder.create(this, "databasePassword")
        .type("String")
        .description("A senha do database RDS")
        .build();

    final ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
    securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));
  }
}

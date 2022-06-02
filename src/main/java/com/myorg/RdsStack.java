package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;

public class RdsStack extends Stack {
  public RdsStack(final Construct scope, final String id, Vpc vpc) {
    this(scope, id, null, vpc);
  }

  public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
    super(scope, id, props);

    final CfnParameter parameterPassword = CfnParameter.Builder.create(this, "databasePassword")
        .type("String")
        .description("A senha do database RDS")
        .noEcho(true) // Deve mascarar o valor?
        .build();

    final ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
    securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

    final DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "Rds01")
        .instanceIdentifier("aws-project01-db")
        .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
            .version(MysqlEngineVersion.VER_5_7)
            .build()))
        .vpc(vpc)
        .credentials(Credentials.fromUsername("admin", CredentialsFromUsernameOptions.builder()
            .password(SecretValue.cfnParameter(parameterPassword))
            .build()))
        .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
        .multiAz(false)
        .allocatedStorage(10)
        .securityGroups(Collections.singletonList(securityGroup))
        .vpcSubnets(SubnetSelection.builder()
            .subnets(vpc.getPrivateSubnets())
            .build())
        .build();

    CfnOutput.Builder.create(this, "rds-endpoint")
        .exportName("rds-endpoint")
        .value(databaseInstance.getDbInstanceEndpointAddress())
        .build();

    CfnOutput.Builder.create(this, "rds-password")
        .exportName("rds-password")
        .value(parameterPassword.getValueAsString())
        .build();

  }
}

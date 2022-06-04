package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;

public class DynamoDBStack extends Stack {

  private final Table productEventsDB;
  public DynamoDBStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public DynamoDBStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    productEventsDB = Table.Builder.create(this, "ProductEventsDB")
        .tableName("product-events")
        .billingMode(BillingMode.PROVISIONED)
        .readCapacity(1) // Limita a capacidade de leitura
        .writeCapacity(1) // " " Escrita
        .partitionKey(Attribute.builder()
            .name("pk")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("sk")
            .type(AttributeType.STRING)
            .build())
        .timeToLiveAttribute("ttl")
        .removalPolicy(RemovalPolicy.DESTROY) // Caso remova a tabela pode reter os dados
        .build();
  }

  public Table getProductEventsDB() {
    return productEventsDB;
  }
}

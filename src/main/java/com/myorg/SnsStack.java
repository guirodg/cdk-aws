package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ses.actions.Sns;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;

public class SnsStack extends Stack {
  public SnsStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public SnsStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);
    Sns.Builder.create().topic(Topic.Builder.create(this, "topic-sns").build()).build();
  }
}

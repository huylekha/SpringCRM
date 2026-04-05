package com.company.platform.shared.event;

public interface OutboxableEvent {

  String getAggregateType();

  String getAggregateId();

  String getEventType();

  Object getPayload();
}

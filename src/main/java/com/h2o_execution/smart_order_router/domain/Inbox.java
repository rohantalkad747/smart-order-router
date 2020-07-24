package com.h2o_execution.smart_order_router.domain;

import java.util.List;

public interface Inbox {
    void sendMessage(String s);

    void setEndpoint(String endpoint);

    List<String> getMessages();
}

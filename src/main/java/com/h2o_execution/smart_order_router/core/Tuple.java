package com.h2o_execution.smart_order_router.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class Tuple<T> {
    private final T first;
    private final T second;
}

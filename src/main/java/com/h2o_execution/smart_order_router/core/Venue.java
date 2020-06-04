package com.h2o_execution.smart_order_router.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue
{
    private Name name;
    private String country;
    private Type type;
    private boolean available;
    private List<String> symbols;
    private Map<String, Rank> symbolRankMap;
    private long avgLatency;

    public enum Type
    {LIT, DARK}
}

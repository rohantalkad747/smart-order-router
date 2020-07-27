# Smart Order Router

#### Description

This is an implementation of a cross-border, multi-venue (lit & dark) smart order router that makes latency adjustments such that child orders arrive at different venues at the same time. The primary goal is to avoid HFT arbitrage.

#### Project Components

- Consolidated Order Book for Order Sweeping
- Probabilistic Model for Order Posting
- FIX 4.2 Gateway for sending single orders and receiving rejects and executions
- FX Service for calculating best execution across international venues

### High-Level Diagram

![Alt text](doc/sor.png?raw=true "Title")

### Research Notes

https://pdfs.semanticscholar.org/be6a/9d7dfd3d4e95a7f7632f5c5181361bba5f8d.pdf

http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.190.3057

 http://qes.bmo.com/papers/17_BMO_SmartOrderRouters.pdf

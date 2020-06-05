package com.h2o_execution.smart_order_router.core;

public interface FXConversionService
{
    /**
     * @return the amount of USD to buy $1 CAD.
     */
    int getUSD(int amtCAD);
    /**
     * @return the amount of CAD to buy $1 USD.
     */
    int getCAD(int amtUSD);

    /**
     * Updates the amount of CAD needed to buy $1 USD to {@code amtCAD}.
     */
    void updateCADtoUSD(int amtCAD);
}

package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.db.Database;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.order.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

public class AlpacaTransactions {
    private static final Logger LOG = LogManager.getLogger(AlpacaTransactions.class);

    public static AlpacaOrder buySecurity(Security security, BigDecimal qty, BigDecimal price) {
        AlpacaAPI alpacaAPI = AlpacaLive.getInstance().getAlpacaAPI();
        try {
            Order order = alpacaAPI.requestNewLimitOrder(security.getSymbol(), qty.intValue(), OrderSide.BUY, OrderTimeInForce.DAY, price.doubleValue(), false);
            AlpacaOrder alpacaOrder = new AlpacaOrder(order);
            Database.persistAlpacaOrder(alpacaOrder);
            LOG.info("Buy order: " + alpacaOrder.toString().replace(",", ",\n\t"));
            return alpacaOrder;
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e.getLocalizedMessage());
        }
        LOG.error("Buy order failed: " + security.getSymbol() + ", " + qty + ", " + price);
        return null;
    }

    public static AlpacaOrder sellSecurity(Security security, BigDecimal qty, BigDecimal price) {
        AlpacaAPI alpacaAPI = AlpacaLive.getInstance().getAlpacaAPI();
        try {
            Order order = alpacaAPI.requestNewLimitOrder(security.getSymbol(), qty.intValue(), OrderSide.SELL, OrderTimeInForce.DAY, price.doubleValue(), false);
            AlpacaOrder alpacaOrder = new AlpacaOrder(order);
            LOG.info("Sell order: " + order.toString().replace(",", ",\n\t"));
            return alpacaOrder;
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e.getLocalizedMessage());
        }
        LOG.info("Sell order failed: " + security.getSymbol() + ", " + qty + ", " + price);
        return null;
    }
}

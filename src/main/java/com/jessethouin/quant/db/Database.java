package com.jessethouin.quant.db;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Date;
import java.util.List;

public class Database {
    private static Session session;
    private static final Logger LOG = LogManager.getLogger(Database.class);

    static {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        try {
            SessionFactory sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            session = sessionFactory.openSession();
        } catch (Exception e) {
            LOG.error(e);
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    public static Session getSession() {
        return session;
    }

    public static void closeSession() {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    public static void persistAlpacaOrder(AlpacaOrder alpacaOrder) {
        session.beginTransaction();
        session.persist(alpacaOrder);
        session.getTransaction().commit();
    }

    public static AlpacaOrder getAlpacaOrder(String id) {
        session.beginTransaction();
        AlpacaOrder result = (AlpacaOrder) getSession()
                .createQuery("from AlpacaOrder where id=:id")
                .setParameter("id", id)
                .uniqueResult();
        session.getTransaction().commit();
        return result;
    }

    public static void persistBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder) {
        session.beginTransaction();
        session.persist(binanceLimitOrder);
        session.getTransaction().commit();
    }

    public static BinanceLimitOrder getBinanceLimitOrder(String id) {
        session.beginTransaction();
        BinanceLimitOrder result = (BinanceLimitOrder) getSession()
                .createQuery("from BinanceLimitOrder where id=:id")
                .setParameter("id", id)
                .uniqueResult();
        session.getTransaction().commit();
        return result;
    }

    public static List<BinanceTradeHistory> getBinanceTradeHistory(int limit) {
        session.beginTransaction();
        List<BinanceTradeHistory> binanceTradeHistoryList = getSession().createQuery("from BinanceTradeHistory", BinanceTradeHistory.class).setMaxResults(limit).getResultList();
        session.getTransaction().commit();
        return binanceTradeHistoryList;
    }

    public static List<BinanceTradeHistory> getBinanceTradeHistory(Date start, Date end) {
        session.beginTransaction();
        List<BinanceTradeHistory> binanceTradeHistoryList = getSession()
                .createQuery("from BinanceTradeHistory where timestamp between :s and :e", BinanceTradeHistory.class)
                .setParameter("s", start)
                .setParameter("e", end)
                .getResultList();
        session.getTransaction().commit();
        return binanceTradeHistoryList;
    }

    public static void persistPortfolio(Portfolio portfolio) {
        session.beginTransaction();
        session.persist(portfolio);
        session.getTransaction().commit();
    }

    public static Portfolio getPortfolio() {
        session.beginTransaction();
        Portfolio result = (Portfolio) getSession().createQuery("from Portfolio").uniqueResult();
        session.getTransaction().commit();
        return result;
    }

    public static void persistTradeHistory(BinanceTradeHistory binanceTradeHistory) {
        session.beginTransaction();
        session.persist(binanceTradeHistory);
        session.getTransaction().commit();
    }

    public static void persistOrderHistoryLookup(OrderHistoryLookup orderHistoryLookup) {
        session.beginTransaction();
        session.persist(orderHistoryLookup);
        session.getTransaction().commit();
    }

    public static void persistEntity(Object t) {
        session.beginTransaction();
        session.persist(t);
        session.getTransaction().commit();
    }
}

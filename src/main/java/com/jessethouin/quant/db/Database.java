package com.jessethouin.quant.db;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.TickerHistory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

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

    public static void persistTickerHistory(TickerHistory tickerHistory) {
        session.beginTransaction();
        session.persist(tickerHistory);
        session.getTransaction().commit();
    }
}

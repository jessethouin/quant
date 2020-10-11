package com.jessethouin.quant.db;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Position;
import com.jessethouin.quant.beans.Security;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class Database {
    private static SessionFactory sessionFactory;
    private static Session session;
    private static final Logger LOG = LogManager.getLogger(Database.class);

    static {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            session = sessionFactory.openSession();
        } catch (Exception e) {
            LOG.error(e);
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static Session getSession() {
        return session;
    }

    public static void closeSession() {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    public static Portfolio mergePortfolio(Portfolio portfolio) {
        session.beginTransaction();
        session.flush();
        Portfolio p = (Portfolio) session.merge(portfolio);
        session.getTransaction().commit();
        return p;
    }

    public static void persistPortfolio(Portfolio portfolio) {
        session.beginTransaction();
        session.persist(portfolio);
        session.getTransaction().commit();
    }

    public static Security mergeSecurity(Security security) {
        session.beginTransaction();
        Security s = (Security) session.merge(security);
        session.getTransaction().commit();
        return s;
    }

    public static void updateSecurity(Security security) {
        session.beginTransaction();
        session.update(security);
        session.getTransaction().commit();
    }

    public static void saveSecurity(Security security) {
        session.beginTransaction();
        session.saveOrUpdate(security);
        session.getTransaction().commit();
    }

    public static Position mergePosition(Position position) {
        session.beginTransaction();
        Position p = (Position) session.merge(position);
        session.getTransaction().commit();
        return p;
    }

    public static void persistPosition(Position position) {
        session.beginTransaction();
        session.persist(position);
        session.getTransaction().commit();
    }

    public static Portfolio getPortfolio() {
        session.beginTransaction();
        Portfolio result = (Portfolio) getSession().createQuery( "from Portfolio" ).uniqueResult();
        session.getTransaction().commit();
        return result;
    }
}

package com.jessethouin.quant.db;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DatabaseTest {
    private static final Logger LOG = LogManager.getLogger(Transactions.class);
    private SessionFactory sessionFactory;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // A SessionFactory is set up once for an application!
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml
                .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
            // so destroy it manually.
            e.printStackTrace();
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @org.junit.jupiter.api.Test
    void testBasicUsage() {
        Portfolio portfolio = new Portfolio();
        portfolio.setCash(new BigDecimal("23000"));
        List<Security> securities = new ArrayList<>();
        Arrays.stream(new String[]{"F", "IBM"}).iterator().forEachRemaining(t -> {
            Security security = new Security();
            security.setSymbol(t);
            security.setPortfolio(portfolio);
            securities.add(security);
        });
        portfolio.setSecurities(securities);

        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.save(portfolio);
        session.getTransaction().commit();
        session.close();

        session = sessionFactory.openSession();
        session.beginTransaction();
        Object result = session.createQuery( "from Portfolio" ).uniqueResult();
        if (result instanceof Portfolio) {
            System.out.println("Cash: " + ((Portfolio) result).getCash());
            System.out.println("\tSecurities: " + ((Portfolio) result).getSecurities().size());
        }
        session.getTransaction().commit();
        session.close();
    }

    @org.junit.jupiter.api.Test
    void get() {
    }
}
package com.jessethouin.quant.db;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.*;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.CurrencyTypes;
import net.jacobpeterson.polygon.rest.exception.PolygonAPIRequestException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

class DatabaseTest {
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
    void testDuplicateCurrencies() {
        Portfolio portfolio = new Portfolio();

        Currency usd1 = new Currency();
        usd1.setSymbol("USD");
        usd1.setCurrencyType(CurrencyTypes.FIAT);

        Currency usd2 = new Currency();
        usd2.setSymbol("USD");
        usd2.setCurrencyType(CurrencyTypes.FIAT);

        portfolio.getCurrencies().add(usd1);
        portfolio.getCurrencies().add(usd2);

        try {
            savePortfolio(portfolio);
        } catch (Exception e) {
            System.out.println("Exception Caught:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @org.junit.jupiter.api.Test
    void testBasicUsage() {
        Portfolio portfolio = new Portfolio();

        Currency usd = new Currency();
        usd.setSymbol("USD");
        usd.setCurrencyType(CurrencyTypes.FIAT);

        Currency cad = new Currency();
        cad.setSymbol("CAD");
        cad.setCurrencyType(CurrencyTypes.FIAT);

        CurrencyPosition currencyPosition = new CurrencyPosition();
        currencyPosition.setQuantity(new BigDecimal("23000"));
        currencyPosition.setOpened(new Date());
        currencyPosition.setCounterCurrency(cad);
        currencyPosition.setPrice(new BigDecimal("30071.12"));
        currencyPosition.setBaseCurrency(usd);

        usd.getCurrencyPositions().add(currencyPosition);
        usd.setPortfolio(portfolio);
        cad.setPortfolio(portfolio);
        portfolio.getCurrencies().add(usd);
        portfolio.getCurrencies().add(cad);

        Set<Security> securities = new HashSet<>();
        Arrays.stream(new String[]{"F", "IBM"}).iterator().forEachRemaining(t -> {
            Security security = new Security();
            security.setSymbol(t);
            security.setCurrency(usd);

            SecurityPosition securityPosition = new SecurityPosition();
            securityPosition.setOpened(new Date());
            try {
                securityPosition.setPrice(BigDecimal.valueOf(AlpacaLive.getInstance().getPolygonAPI().getLastQuote(t).getLast().getAskprice()));
            } catch (PolygonAPIRequestException e) {
                e.printStackTrace();
                securityPosition.setPrice(BigDecimal.valueOf(10));
            }
            securityPosition.setQuantity(BigDecimal.valueOf(75));
            securityPosition.setSecurity(security);

            security.setSecurityPositions(Collections.singleton(securityPosition));
            security.setPortfolio(portfolio);
            securities.add(security);
        });
        portfolio.getSecurities().addAll(securities);

        savePortfolio(portfolio);

        Currency currencyFromPortfolio = Util.getCurrencyFromPortfolio(usd.getSymbol(), portfolio);
        System.out.println("USD positions in portfolio: " + currencyFromPortfolio.getCurrencyPositions().size());
        Database.closeSession();

        Session session = sessionFactory.openSession();
        session.beginTransaction();
        Object result = session.createQuery("from Portfolio p where p.portfolioId = :portfolioId").setParameter("portfolioId", portfolio.getPortfolioId()).getSingleResult();
        if (result instanceof Portfolio) {
            System.out.println("\tCurrencies: " + ((Portfolio) result).getCurrencies().size());
            ((Portfolio) result).getCurrencies().forEach(c -> {
                System.out.printf("\t\tSymbol: %s\n", c.getSymbol());
                System.out.printf("\t\t\tType: %s\n", c.getCurrencyType());
                c.getCurrencyPositions().forEach(p -> System.out.printf("\t\t\tQuantity: %f\n\t\t\tCounter Currency: %s\n\t\t\tCounter Price: %f\n", p.getQuantity(), p.getCounterCurrency().getSymbol(), p.getPrice()));
            });
            System.out.println("\tSecurities: " + ((Portfolio) result).getSecurities().size());
            ((Portfolio) result).getSecurities().forEach(s -> {
                System.out.printf("\t\tSymbol: %s\n", s.getSymbol());
                s.getSecurityPositions().forEach(p -> System.out.printf("\t\t\tQty: %f\n\t\t\tPrice: %f\n\t\t\tCurrency: %s\n", p.getQuantity(), p.getPrice(), s.getCurrency().getSymbol()));
            });
            System.out.printf("\tTotal USD: %f\n", Util.getPortfolioValue(portfolio, usd));
        }
        session.getTransaction().commit();
        session.close();

        deletePortfolio(portfolio);
    }

    private void savePortfolio(Portfolio portfolio) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.save(portfolio);
        session.getTransaction().commit();
        session.close();
    }


    private void deletePortfolio(Portfolio portfolio) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.delete(portfolio);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    void getBinanceLimitOrder() {
        BinanceLimitOrder binanceLimitOrder = Database.getBinanceLimitOrder("4089390235");
        Assertions.assertEquals(binanceLimitOrder.getInstrument(), "BTC/USDT");
    }
}
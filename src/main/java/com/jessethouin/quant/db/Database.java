package com.jessethouin.quant.db;

import com.jessethouin.quant.beans.Portfolio;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class Database {
    private static SessionFactory sessionFactory;

    static {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    public static void save(Portfolio portfolio) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.save(portfolio);
        session.getTransaction().commit();
        session.close();
    }

    public static Portfolio get() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        Portfolio result = (Portfolio) session.createQuery( "from Portfolio" ).uniqueResult();
        session.getTransaction().commit();
        session.close();
        return result;
    }
}

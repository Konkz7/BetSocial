package com.example.World.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts JDBC statements, so query-count claims can be measured rather than
 * asserted.
 *
 * Wraps the DataSource bean in a dynamic proxy that tallies every
 * prepareStatement/createStatement call. Pure JDK reflection - no extra
 * dependency for something that only exists to support a test.
 */
public final class QueryCounter {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private QueryCounter() {
    }

    public static void reset() {
        COUNT.set(0);
    }

    public static int count() {
        return COUNT.get();
    }

    @TestConfiguration
    public static class Config {

        @Bean
        public static BeanPostProcessor countingDataSourcePostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
                    if (!(bean instanceof DataSource dataSource)) {
                        return bean;
                    }
                    return (DataSource) Proxy.newProxyInstance(
                            DataSource.class.getClassLoader(),
                            new Class<?>[]{DataSource.class},
                            (proxy, method, args) -> {
                                Object result = method.invoke(dataSource, args);
                                return result instanceof Connection connection
                                        ? countingConnection(connection)
                                        : result;
                            });
                }
            };
        }

        private static Connection countingConnection(Connection connection) {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("prepareStatement") || name.equals("createStatement")
                        || name.equals("prepareCall")) {
                    COUNT.incrementAndGet();
                }
                return method.invoke(connection, args);
            };
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler);
        }
    }
}

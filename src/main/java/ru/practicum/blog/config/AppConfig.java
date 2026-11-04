package ru.practicum.blog.config;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"ru.practicum.blog.service", "ru.practicum.blog.repository"})
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        String url = System.getProperty("blog.jdbc.url", "jdbc:h2:file:" + System.getProperty("user.home") + "/my-blog-data/blog;AUTO_SERVER=TRUE");
        if (System.getProperty("blog.jdbc.url") == null) {
            try {
                java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), "my-blog-data"));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Cannot create application data directory", e);
            }
        }
        ds.setURL(url);
        ds.setUser("sa");
        ds.setPassword("");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
        return ds;
    }
    @Bean public JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
    @Bean public PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
}

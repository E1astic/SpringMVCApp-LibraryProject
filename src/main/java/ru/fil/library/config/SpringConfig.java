package ru.fil.library.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

// АНАЛОГ ФАЙЛА applicationContext.xml
@Configuration
@ComponentScan("ru.fil.library")
@PropertySource("classpath:hibernate.properties")
@EnableWebMvc
@EnableTransactionManagement  // Spring за нас управляет транзакциями
public class SpringConfig implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;
    private final Environment environment;

    @Autowired
    public SpringConfig(ApplicationContext applicationContext, Environment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    // ниже идут 3 бина для Thymeleaf
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("/WEB-INF/views/");
        templateResolver.setSuffix(".html");
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        registry.viewResolver(resolver);
    }

    // ниже идут 2 бина для JdbcTemplate
    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource=new DriverManagerDataSource();
        dataSource.setDriverClassName(Objects.requireNonNull(environment.getRequiredProperty("hibernate.driver_class")));
        dataSource.setUrl(environment.getRequiredProperty("hibernate.connection.url"));
        dataSource.setUsername(environment.getRequiredProperty("hibernate.connection.username"));
        dataSource.setPassword(environment.getRequiredProperty("hibernate.connection.password"));
        return dataSource;
    }

//    @Bean
//    JdbcTemplate jdbcTemplate(){
//        return new JdbcTemplate(dataSource());
//    }

    //  метод и 2 бина ниже необходимы для конфигурации Hibernate
    private Properties hibernateProperties(){
        Properties properties=new Properties();
        properties.put("hibernate.dialect", environment.getRequiredProperty("hibernate.dialect"));
        properties.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
        return properties;
    }

    // по аналогии собираем sessionFactory, как и в чистом Hibernate:
    // dataSource+hib.properties+EntityClasses
    @Bean
    public LocalSessionFactoryBean sessionFactory(){
        LocalSessionFactoryBean sessionFactory=new LocalSessionFactoryBean();

        sessionFactory.setDataSource(dataSource());
        sessionFactory.setHibernateProperties(hibernateProperties());
        sessionFactory.setPackagesToScan("ru.fil.library.models");

        return sessionFactory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(){
        HibernateTransactionManager transactionManager=new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory().getObject());
        return transactionManager;
    }

}

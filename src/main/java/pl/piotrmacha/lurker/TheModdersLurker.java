package pl.piotrmacha.lurker;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@SpringBootApplication
public class TheModdersLurker {
    public static void main(String[] args) {
        SpringApplication.run(TheModdersLurker.class, args);
    }

    @Bean
    DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}

package it.bz.sta.lf.lfbackend;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = { "it.bz.sta.lf", "it.bz.sta.lf.lfbackend" })
@EntityScan(basePackages = { "it.bz.sta.lf", "it.bz.sta.lf.lfbackend" })
@EnableJpaRepositories(basePackages = { "it.bz.sta.lf", "it.bz.sta.lf.lfbackend" })
public class LfBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(LfBackendApplication.class, args);
    }
}
package dev.bootupgrade.samples.securityservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Deprecated {@code WebSecurityConfigurerAdapter} style, removed entirely in Spring Security 6 / Spring Boot 3.
 * OpenRewrite's recipes can migrate the mechanical parts of a Spring Boot 2->3 upgrade, but rewriting a custom
 * {@code WebSecurityConfigurerAdapter} subclass into the component-based {@code SecurityFilterChain} style needs
 * judgment calls (which is exactly why it is left as fixture material for the Phase 3 LLM agent fallback).
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/status/public").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("admin")
                .password("{noop}password")
                .roles("USER");
    }
}

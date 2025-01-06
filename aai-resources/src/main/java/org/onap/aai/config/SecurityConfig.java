package org.onap.aai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> requests
                .antMatchers("/util/echo", "/actuator/**")
                .permitAll()
                .anyRequest()
                .authenticated())
            .httpBasic();

        return httpSecurity.build();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("AAI")
            .password("AAI")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}

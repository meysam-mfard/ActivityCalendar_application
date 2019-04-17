package se.meysam.activitycalendarApplication;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class OAuth2ClientSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                .antMatchers("/oauthLogin")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .oauth2Login()
                //.defaultSuccessUrl("/loginSuccess")
                //.failureUrl("/loginFailure")
                .loginPage("/oauthLogin");
    }
}

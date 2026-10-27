package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.security.AdminApiRequestMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminApiSecurityTest
{
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    public void initialize()
    {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestSecurity.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @AfterEach
    public void close()
    {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void ordinaryUsersCannotCallAdministrativeApis()
            throws Exception
    {
        for (String path : new String[] {"/api/v1/role/list", "/api/v1/menu/list", "/api/v1/schedule/1/trigger", "/api/v1/admin/schedule/list", "/api/v1/user/list", "/api/v1/function/list"}) {
            mvc.perform(request(HttpMethod.POST, path).servletPath(path).header("X-Test-Role", "USER")).andExpect(status().isForbidden());
            mvc.perform(request(HttpMethod.POST, path).servletPath(path).header("X-Test-Role", "ADMIN")).andExpect(status().isOk());
        }
        String configure = "/api/v1/configure/runtime/list/EXECUTOR";
        mvc.perform(request(HttpMethod.GET, configure).servletPath(configure).header("X-Test-Role", "USER")).andExpect(status().isForbidden());
    }

    @Test
    public void personalEndpointsAndEditorCompletionRemainAvailable()
            throws Exception
    {
        for (String path : new String[] {"/api/v1/user", "/api/v1/user/menus", "/api/v1/user/checkUsername", "/api/v1/function/list/MySQL", "/api/v1/source/admin/count"}) {
            mvc.perform(request(HttpMethod.GET, path).servletPath(path).header("X-Test-Role", "USER")).andExpect(status().isOk());
        }
        String password = "/api/v1/user/changePassword";
        mvc.perform(request(HttpMethod.PUT, password).servletPath(password).header("X-Test-Role", "USER")).andExpect(status().isOk());
    }

    @Test
    public void trailingSlashesCannotBypassAdministrativeWrites()
            throws Exception
    {
        for (String path : new String[] {"/api/v1/user/", "/api/v1/user/allocationRole/", "/api/v1/function/", "/api/v1/function/import/"}) {
            mvc.perform(request(HttpMethod.PUT, path).servletPath(path.substring(0, path.length() - 1)).pathInfo("/").header("X-Test-Role", "USER")).andExpect(status().isForbidden());
        }
    }

    @Test
    public void anonymousAccessRequiresAuthentication()
            throws Exception
    {
        String path = "/api/v1/role/list";
        mvc.perform(request(HttpMethod.POST, path).servletPath(path)).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    public static class TestSecurity
            extends WebSecurityConfigurerAdapter
    {
        @Bean
        public Probe probe()
        {
            return new Probe();
        }

        @Override
        protected void configure(HttpSecurity http)
                throws Exception
        {
            http.csrf().disable().exceptionHandling().authenticationEntryPoint((request, response, exception) -> response.sendError(401)).and()
                    .authorizeRequests().requestMatchers(new AdminApiRequestMatcher()).hasAuthority("ADMIN").anyRequest().authenticated().and()
                    .addFilterBefore(new OncePerRequestFilter()
                    {
                        @Override
                        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                                throws ServletException, IOException
                        {
                            String role = request.getHeader("X-Test-Role");
                            if (role != null) {
                                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test", "", List.of(new SimpleGrantedAuthority(role))));
                            }
                            chain.doFilter(request, response);
                        }
                    }, AnonymousAuthenticationFilter.class);
        }
    }

    @RestController
    public static class Probe
    {
        @RequestMapping("/**")
        public String result()
        {
            return "ok";
        }
    }
}

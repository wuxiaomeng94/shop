package com.demo.shop.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DruidConfig {

    @ConfigurationProperties(prefix = "spring.datasource")  //在这里配置一下，让加载数据源的时候可以加载druid里才有的一些配置属性
    @Bean
    public DataSource druid() {
        return new DruidDataSource();
    }


    //配置druid监控
    @Bean
    public ServletRegistrationBean statViewServlet() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
        Map<String, String> initParams = new HashMap<>();
        /*initParams.put("loginUsername", "admin");
        initParams.put("loginPassword", "123456");*/
        initParams.put("allow", "");//默认允许所有访问
        //initParams.put("deny", "192.168.0.102");  拒绝某个地址的访问
        servletRegistrationBean.setInitParameters(initParams);
        return servletRegistrationBean;
    }


    //配置filter
    @Bean
    public FilterRegistrationBean webStatFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        Map<String, String> initParams = new HashMap<>();
        initParams.put("exclusions", "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico,/druid/*,*.html");
        filterRegistrationBean.setInitParameters(initParams);
        filterRegistrationBean.setUrlPatterns(Arrays.asList("/*"));
        return filterRegistrationBean;
    }




}

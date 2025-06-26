package com.bestseller.bestone.bi4.avroschemagenerator.genavroschemadb;

import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.config.KafkaSchemaGenProperties;
import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.config.SchemaGenProperties;
import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.domain.postgres.metadata.repository.ViewColumnUsageRepository;
import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.schemagen.service.SchemaGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.web.servlet.View;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({KafkaSchemaGenProperties.class})
public class SchemaWriterConfig {

    @Autowired
    private ApplicationArguments applicationArguments;

    private String applicationConfigPath;

    private Map<String, Object> configProperties;

    @PostConstruct
    public void loadApplicationConfig() {
        String filePath;

        if (!applicationArguments.getNonOptionArgs().isEmpty()) {
            filePath = applicationArguments.getNonOptionArgs().getFirst();
        } else {
            throw new RuntimeException("No argument provided");
        }

        File configFile = new File(filePath);

        // Check if the provided path is directly to a file
        if (configFile.isFile()) {
            applicationConfigPath = filePath;
        } else {
            try {
                java.nio.file.Path startPath = java.nio.file.Paths.get(filePath);
                java.util.Optional<java.nio.file.Path> configFilePath = java.nio.file.Files.find(
                        startPath,
                        30, // Search depth limit
                        (path, attr) -> path.getFileName().toString().equals("application-local.yml")
                                && !path.toString().contains("/target/") && !path.toString().contains("/test/")
                ).findFirst();
                if (configFilePath.isPresent()) {
                    applicationConfigPath = configFilePath.get().toString();
                    configFile = configFilePath.get().toFile();
                    System.out.println("Found configuration file at: " + applicationConfigPath);
                } else {
                    throw new IOException("Could not find application-local.yml in " + filePath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error searching for application-local.yml: " + e.getMessage(), e);
            }
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            configProperties = mapper.readValue(configFile, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error reading configuration file: " + e.getMessage(), e);
        }
    }

    @Bean
    public String applicationConfigPath() {
        return applicationConfigPath;
    }

    @Bean
    public DataSource dataSource() {

        @SuppressWarnings("unchecked")
        Map<String, Object> springConfig = (Map<String, Object>) configProperties.get("spring");
        @SuppressWarnings("unchecked")
        Map<String, Object> datasourceConfig = (Map<String, Object>) springConfig.get("datasource");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl((String) datasourceConfig.get("url"));
        dataSource.setUsername((String) datasourceConfig.get("username"));
        dataSource.setPassword((String) datasourceConfig.get("password"));
        return dataSource;
    }

    @Bean
    public KafkaSchemaGenProperties kafkaSchemaGenProperties(View view) {
        KafkaSchemaGenProperties properties = new KafkaSchemaGenProperties();

        boolean propertiesFound = false;

        if (configProperties.containsKey("kafka-schema-gen")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kafkaSchemaGenConfig = (Map<String, Object>) configProperties.get("kafka-schema-gen");

            // Set boolean properties
            if (kafkaSchemaGenConfig.containsKey("createSchema")) {
                properties.setCreateSchema((Boolean) kafkaSchemaGenConfig.get("createSchema"));
            }
            if (kafkaSchemaGenConfig.containsKey("createTopic")) {
                properties.setCreateTopic((Boolean) kafkaSchemaGenConfig.get("createTopic"));
            }

            // Set schemaGenProperties list if available
            if (kafkaSchemaGenConfig.containsKey("schema-gen-properties")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> schemaGenPropertiesList = (List<Map<String, Object>>) kafkaSchemaGenConfig.get("schema-gen-properties");
                List<SchemaGenProperties> schemaGenPropertiesObjects = new ArrayList<>();

                for (Map<String, Object> propMap : schemaGenPropertiesList) {
                    SchemaGenProperties schemaGenProps = new SchemaGenProperties();
                    if (propMap.containsKey("entity-short-name")) schemaGenProps.setEntityShortName((String) propMap.get("entity-short-name"));
                    if (propMap.containsKey("topic-name")) schemaGenProps.setTopicName((String) propMap.get("topic-name"));
                    if (propMap.containsKey("view-key-name")) schemaGenProps.setViewName((String) propMap.get("view-key-name"));
                    if (propMap.containsKey("view-name")) schemaGenProps.setViewKeyName((String) propMap.get("view-name"));
                    if (propMap.containsKey("name-space")) schemaGenProps.setNameSpace((String) propMap.get("name-space"));
                    schemaGenPropertiesObjects.add(schemaGenProps);
                }
                properties.setSchemaGenProperties(schemaGenPropertiesObjects);
            }

            propertiesFound = true;
        } else if (configProperties.containsKey("spring")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> springConfig = (Map<String, Object>) configProperties.get("spring");

            if (springConfig.containsKey("kafka-schema-gen")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kafkaSchemaGenConfig = (Map<String, Object>) springConfig.get("kafka-schema-gen");

                // Set boolean properties
                if (kafkaSchemaGenConfig.containsKey("createSchema")) {
                    properties.setCreateSchema((Boolean) kafkaSchemaGenConfig.get("createSchema"));
                }
                if (kafkaSchemaGenConfig.containsKey("createTopic")) {
                    properties.setCreateTopic((Boolean) kafkaSchemaGenConfig.get("createTopic"));
                }

                // Set schemaGenProperties list if available
                if (kafkaSchemaGenConfig.containsKey("schemaGenProperties")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> schemaGenPropertiesList = (List<Map<String, Object>>) kafkaSchemaGenConfig.get("schemaGenProperties");
                    List<SchemaGenProperties> schemaGenPropertiesObjects = new ArrayList<>();

                    for (Map<String, Object> propMap : schemaGenPropertiesList) {
                        SchemaGenProperties schemaGenProps = new SchemaGenProperties();
                        if (propMap.containsKey("entityShortName")) schemaGenProps.setEntityShortName((String) propMap.get("entityShortName"));
                        if (propMap.containsKey("topicName")) schemaGenProps.setTopicName((String) propMap.get("topicName"));
                        if (propMap.containsKey("viewName")) schemaGenProps.setViewName((String) propMap.get("viewName"));
                        if (propMap.containsKey("viewKeyName")) schemaGenProps.setViewKeyName((String) propMap.get("viewKeyName"));
                        if (propMap.containsKey("nameSpace")) schemaGenProps.setNameSpace((String) propMap.get("nameSpace"));
                        schemaGenPropertiesObjects.add(schemaGenProps);
                    }
                    properties.setSchemaGenProperties(schemaGenPropertiesObjects);
                }

                propertiesFound = true;
            }
        }

        if (propertiesFound) {
            System.out.println("KafkaSchemaGenProperties loaded from configuration file");
            return properties;
        }

        // If automatic configuration failed, fall back to manual input
        System.out.println("KafkaSchemaGenProperties not found in configuration, please provide manually:");

        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            SchemaGenProperties schemaGenProperties = new SchemaGenProperties();
            System.out.print("Enter schema short name : ");
            String schemaShortName = scanner.nextLine();
            schemaGenProperties.setEntityShortName(schemaShortName);

            System.out.print("Enter topic name: ");
            String topicName = scanner.nextLine();
            schemaGenProperties.setTopicName(topicName);

            System.out.print("Enter view name: ");
            String viewName = scanner.nextLine();
            schemaGenProperties.setViewName(viewName);
            schemaGenProperties.setViewKeyName(viewName + "_id");

            System.out.print("Enter namespace: ");
            String nameSpace = scanner.nextLine();
            schemaGenProperties.setNameSpace(nameSpace);

            // Transfer values from schemaGenProperties to properties
            properties.setSchemaGenProperties(List.of(schemaGenProperties));

            System.out.println("Manually configured KafkaSchemaGenProperties successfully");
        }

        return properties;
    }

    @Bean
    public ViewColumnUsageRepository viewColumnUsageRepository(EntityManager entityManager) {
        return new ViewColumnUsageRepository(entityManager);
    }

    @Bean
    public SchemaRegistryClient schemaRegistryClient() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> springConfig = (Map<String, Object>) configProperties.get("spring");
        @SuppressWarnings("unchecked")
        Map<String, Object> kafkaConfig = (Map<String, Object>) springConfig.get("kafka");
        @SuppressWarnings("unchecked")
        Map<String, Object> kafkaProps = (Map<String, Object>) kafkaConfig.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaConfig = (Map<String, Object>) kafkaProps.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> registryConfig = (Map<String, Object>) schemaConfig.get("registry");

        String schemaRegistryUrl = (String) registryConfig.get("url");
        return new CachedSchemaRegistryClient(schemaRegistryUrl, 100);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bestseller.bestone.bi4","com.bestseller.bestone.bi4.avroschemagenerator.genavroschemadb");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.POSTGRESQL);
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "validate");
        properties.put("hibernate.show_sql", "false");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(9999);
    }

    @Bean
    public SchemaGenerationService schemaGenerationService(ViewColumnUsageRepository viewColumnUsageRepository, SchemaRegistryClient schemaRegistryClient, KafkaProperties kafkaProperties) {
        return new SchemaGenerationService(viewColumnUsageRepository, schemaRegistryClient, kafkaProperties);
    }
}
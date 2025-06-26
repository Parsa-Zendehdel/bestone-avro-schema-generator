package com.bestseller.bestone.bi4.avroschemagenerator.genavroschemadb;

import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.config.KafkaSchemaGenProperties;
import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.config.SchemaGenProperties;
import com.bestseller.bestone.bi4.sales.standardutils.kafkaschemautils.schemagen.service.SchemaGenerationService;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class GenAvroSchemaDbApplication implements CommandLineRunner {

    @Autowired
    private KafkaSchemaGenProperties kafkaSchemaGenProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SchemaGenerationService schemaGenerationService;

    public static void main(String[] args) {
        // Configure Spring Boot to minimize logging output
        SpringApplication app = new SpringApplication(GenAvroSchemaDbApplication.class);

        app.setBanner((environment, sourceClass, out) -> {
            out.println("  _____      _                           _____            ");
            out.println(" / ____|    | |                         / ____|           ");
            out.println("| (___   ___| |__   ___ _ __ ___   __ _| |  __  ___ _ __  ");
            out.println(" \\___ \\ / __| '_ \\ / _ \\ '_ ` _ \\ / _` | | |_ |/ _ \\ '_ \\ ");
            out.println(" ____) | (__| | | |  __/ | | | | | (_| | |__| |  __/ | | |");
            out.println("|_____/ \\___|_| |_|\\___|_| |_| |_|\\__,_|\\_____|\\___|_| |_|");
            out.println("                                                          ");
            out.println(":: Schema Generator Tool ::                (v1.0.0)       ");
            out.println();
        });

        // Set properties to reduce logging
        Map<String, Object> properties = new HashMap<>();
        properties.put("logging.level.root", "OFF");
        properties.put("logging.level.org.springframework", "OFF");
        properties.put("spring.main.log-startup-info", "false");


        app.setDefaultProperties(properties);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        List<SchemaGenProperties> schemaGenProperties = kafkaSchemaGenProperties.getSchemaGenProperties();
        if( args.length == 0) {
            throw new RuntimeException("No configuration file path provided as an argument.");
        }
        File configFile = new File(args[0]);
        String dirPath = configFile.getParent();

        for (SchemaGenProperties schemaGenProperty : schemaGenProperties) {
            Schema s = schemaGenerationService.buildSchema(schemaGenProperty.getTopicName(), schemaGenProperty.getViewName(), schemaGenProperty.getEntityShortName(), schemaGenProperty.getNameSpace());
            String filename = schemaGenProperty.getEntityShortName() + "_" + schemaGenProperty.getTopicName() + ".avsc";
            File schemaFile = new File(dirPath, filename);

            try (FileWriter writer = new FileWriter(schemaFile)) {
                writer.write(s.toString(true)); // Pretty-print the schema
                System.out.println("Schema " + s.getName() + " saved to: file://" + schemaFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);

    }
}
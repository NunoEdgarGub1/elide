/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.hjson.JsonValue;
import org.json.JSONObject;
import org.json.JSONTokener;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
/**
 * Util class for Dynamic config module.
 */
public class DynamicConfigHelpersUtil {

    public static final String SCHEMA_TYPE_TABLE = "table";
    public static final String SCHEMA_TYPE_SECURITY = "security";
    public static final String SCHEMA_TYPE_VARIABLE = "variable";

    public static final String TABLE_CONFIG_PATH = "tables/";
    public static final String SECURITY_CONFIG_PATH = "security.hjson";
    public static final String VARIABLE_CONFIG_PATH = "variables.hjson";

    public static final String ELIDE_TABLE_VALIDATION_SCHEMA = "elideTableSchema.json";
    public static final String ELIDE_SECURITY_SCHEMA = "elideSecuritySchema.json";
    public static final String ELIDE_VARIABLE_SCHEMA = "elideVariableSchema.json";

    public static final String INVALID_ERROR_MSG = "Incompatible or invalid config";
    public static final String HTTP_PREFIX = "http";
    public static final String CHAR_SET = "UTF-8";
    public static final String NEW_LINE = "\n";

    /**
     * Converts hjson input string to valid json string.
     * @param hjson
     * @return json string
     */
    public String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    /**
     * Validates json config against json schema.
     * @param schemaObj
     * @param jsonConfig
     * @return true or false
     */
    public boolean validateDataWithSchema(JSONObject schemaObj, String jsonConfig) {

        try {
            JSONObject data = new JSONObject(new JSONTokener(jsonConfig));
            Schema schema = SchemaLoader.load(schemaObj);
            schema.validate(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether input is null or empty.
     * @param input
     * @return true or false
     */
    public boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().length() == 0);
    }

    /**
     * Loads appropriate schema per input schema type.
     * @param schemaType
     * @return jsonObject of schema
     * @throws IOException
     */
    public JSONObject schemaToJsonObject(String schemaType) throws IOException {

        switch (schemaType) {
        case SCHEMA_TYPE_TABLE:
            return loadSchema(ELIDE_TABLE_VALIDATION_SCHEMA);

        case SCHEMA_TYPE_SECURITY:
            return loadSchema(ELIDE_SECURITY_SCHEMA);

        case SCHEMA_TYPE_VARIABLE:
            return loadSchema(ELIDE_VARIABLE_SCHEMA);
        default:
            return null;
        }
    }

    /**
     * Converts json string to appropriate POJO.
     * @param inputConfigType
     * @param jsonConfig
     * @return model pojo
     * @throws Exception
     */
    public Object getModelPojo(String inputConfigType, String jsonConfig) throws Exception {

        switch (inputConfigType) {
        case DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE:
            return new ObjectMapper().readValue(jsonConfig, ElideTable.class);

        case DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY:
            return new ObjectMapper().readValue(jsonConfig, ElideSecurity.class);

        case DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE:
            return new ObjectMapper().readValue(jsonConfig, Map.class);

        default:
            return null;
        }
    }

    /**
     * Load config from LocalPath.
     * @param basePath
     * @param configType
     * @return List of Json configs from local dir.
     * @throws Exception
     */
    public List<String> getJsonConfig(String basePath, String configType) throws Exception {
        List<String> configs = new ArrayList<>();

        if (!basePath.endsWith("/")) {
            basePath += '/';
        }

        switch (configType) {
        case SCHEMA_TYPE_VARIABLE:
            configs.add(hjsonToJson(readConfigFile(basePath + VARIABLE_CONFIG_PATH)));
            return configs;

        case SCHEMA_TYPE_SECURITY:
            configs.add(hjsonToJson(readConfigFile(basePath  + SECURITY_CONFIG_PATH)));
            return configs;

        case SCHEMA_TYPE_TABLE:
            return getTablesConfig(basePath + TABLE_CONFIG_PATH);
        }
        return null;
    }

    private static String readFileContent(BufferedReader reader) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(NEW_LINE);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private static BufferedReader getLocalFileReader(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        return Files.newBufferedReader(path, Charset.forName(CHAR_SET));
    }

    private static JSONObject loadSchema(String confFilePath) throws IOException {
        try (InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(confFilePath);
                InputStreamReader reader = new InputStreamReader(stream)) {
            String content = readFileContent(new BufferedReader(reader));
            return new JSONObject(new JSONTokener(content));
        }
    }

    private List<String> getTablesConfig(String tablePath) throws Exception {
        List<String> configList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(tablePath))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    String hjson = readConfigFile(tablePath + path.getFileName().toString());
                    configList.add(hjsonToJson(hjson));
                }
            }
        }
        return configList;
    }

    private static String readConfigFile(String filePath) throws Exception {
        try {
            BufferedReader reader = getLocalFileReader(filePath);
            return readFileContent(reader);
        } catch (Exception e) {
            throw e;
        }
    }
}

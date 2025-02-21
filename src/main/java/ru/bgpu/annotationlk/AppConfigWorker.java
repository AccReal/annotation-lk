package ru.bgpu.annotationlk;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AppConfigWorker {

    private static Logger logger = Logger.getLogger(AppConfigWorker.class.getName());

    public static void configProcessing(String prefix, String filePropName) {
        Reflections reflections = new Reflections(prefix, Scanners.FieldsAnnotated);

        File prop = new File(filePropName);
        if (prop.isFile()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(prop));

                reflections.getFieldsAnnotatedWith(AppConfig.class).forEach(
                        field -> {
                            String value = properties.getProperty(
                                    field.getName(),
                                    field.getAnnotation(AppConfig.class).defValue()
                            );
                            Object targetValue = null;

                            try {
                                // Обработка примитивных типов и их обёрток
                                if (field.getType().equals(String.class)) {
                                    targetValue = value;
                                } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                                    targetValue = Integer.parseInt(value);
                                } else if (field.getType().equals(Float.class) || field.getType().equals(float.class)) {
                                    targetValue = Float.parseFloat(value);
                                } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                                    targetValue = Double.parseDouble(value);
                                }
                                // Обработка массивов
                                else if (field.getType().equals(String[].class)) {
                                    targetValue = value.split(",");
                                } else if (field.getType().equals(Integer[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .map(Integer::parseInt)
                                            .toArray(Integer[]::new);
                                } else if (field.getType().equals(int[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .mapToInt(Integer::parseInt)
                                            .toArray();
                                } else if (field.getType().equals(Float[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .map(Float::parseFloat)
                                            .toArray(Float[]::new);
                                } else if (field.getType().equals(float[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .mapToDouble(Float::parseFloat)
                                            .mapToObj(d -> (float) d)
                                            .collect(Collectors.toList())
                                            .toArray(new float[0]);
                                } else if (field.getType().equals(Double[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .map(Double::parseDouble)
                                            .toArray(Double[]::new);
                                } else if (field.getType().equals(double[].class)) {
                                    targetValue = Arrays.stream(value.split(","))
                                            .mapToDouble(Double::parseDouble)
                                            .toArray();
                                } else {
                                    logger.log(Level.WARNING, "Unsupported field type: " + field.getType().getName());
                                    return;
                                }

                                field.setAccessible(true);
                                field.set(field.getDeclaringClass(), targetValue);
                                field.setAccessible(false);
                            } catch (NumberFormatException e) {
                                logger.log(Level.WARNING,
                                        "Invalid number format for field " + field.getName() + ": " + value, e);
                            } catch (IllegalAccessException e) {
                                logger.log(Level.WARNING,
                                        "Error setting " + field.getDeclaringClass().getName()
                                                + "." + field.getName() + " with value " + value, e);
                            }
                        }
                );
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error loading properties file", e);
            }
        } else {
            logger.log(Level.WARNING, "Config file not found: " + filePropName);
        }
    }
}
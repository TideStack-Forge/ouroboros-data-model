package com.ouroboros.util.config;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperConfigurator extends Consumer<ObjectMapper> {
}

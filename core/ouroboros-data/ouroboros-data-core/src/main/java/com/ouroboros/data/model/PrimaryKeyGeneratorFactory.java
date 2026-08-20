package com.ouroboros.data.model;

import java.util.Optional;

import com.ouroboros.data.util.DataServices;

public interface PrimaryKeyGeneratorFactory {

  PrimaryKeyGeneratorFactory PK_GENERATOR_FACTORY_CHAIN = DataServices.getSortedServiceStream(PrimaryKeyGeneratorFactory.class)
      .reduce(n -> Optional.empty(), (f1, f2) -> s -> {
        var pkGenerator = f2.get(s);
        if (pkGenerator.isPresent()) {
          return pkGenerator;
        }
        return f1.get(s);
      });

  static Optional<PrimaryKeyGenerator<?>> getPkGenerator(String generatorName) {
    return Optional.ofNullable(generatorName).flatMap(PK_GENERATOR_FACTORY_CHAIN::get);
  }

  Optional<PrimaryKeyGenerator<?>> get(String generatorName);
}

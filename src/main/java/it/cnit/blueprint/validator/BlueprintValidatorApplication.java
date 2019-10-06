package it.cnit.blueprint.validator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import it.nextworks.nfvmano.catalogue.blueprint.elements.CtxBlueprint;
import it.nextworks.nfvmano.catalogue.blueprint.elements.ExpBlueprint;
import it.nextworks.nfvmano.catalogue.blueprint.elements.VsBlueprint;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class BlueprintValidatorApplication implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  private static ObjectMapper OBJECT_MAPPER;
  private static javax.validation.Validator VALIDATOR;

  public static void main(String[] args) {
    SpringApplication.run(BlueprintValidatorApplication.class, args);
  }

  private static Namespace parseArguments(String[] args) {
    ArgumentParser parser = ArgumentParsers.newFor("validator").build()
        .defaultHelp(true)
        .description("Simple tool to validate blueprints for the 5G EVE platform.");

    parser.addArgument("--debug").action(Arguments.storeTrue());
    parser.addArgument("-t", "--type").choices("vsb", "cb", "expb", "tstb").required(true)
        .help("Specify the type of blueprint you want to validate.");
    parser.addArgument("file").help("YAML blueprint file path");

    Namespace ns = null;
    try {
      ns = parser.parseArgs(args);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }
    return ns;
  }

  private static void validateVSB(InputStream is) throws ValidationException, IOException {
    VsBlueprint vsb = OBJECT_MAPPER.readValue(is, VsBlueprint.class);
    Set<ConstraintViolation<VsBlueprint>> violations = VALIDATOR.validate(vsb);
    if (!violations.isEmpty()) {
      for (ConstraintViolation<VsBlueprint> v : violations) {
        LOG.error("Violation: property \'{}\' {}", v.getPropertyPath(), v.getMessage());
      }
      throw new ValidationException();
    }
  }

  private static void validateCB(InputStream is) throws ValidationException, IOException {
    CtxBlueprint cb = OBJECT_MAPPER.readValue(is, CtxBlueprint.class);
    Set<ConstraintViolation<CtxBlueprint>> violations = VALIDATOR.validate(cb);
    if (!violations.isEmpty()) {
      for (ConstraintViolation<CtxBlueprint> v : violations) {
        LOG.error("Violation: property \'{}\' {}", v.getPropertyPath(), v.getMessage());
      }
      throw new ValidationException();
    }
  }

  private static void validateExpB(InputStream is) throws ValidationException, IOException {
    ExpBlueprint expB = OBJECT_MAPPER.readValue(is, ExpBlueprint.class);
    Set<ConstraintViolation<ExpBlueprint>> violations = VALIDATOR.validate(expB);
    if (!violations.isEmpty()) {
      for (ConstraintViolation<ExpBlueprint> v : violations) {
        LOG.error("Violation: property \'{}\' {}", v.getPropertyPath(), v.getMessage());
      }
      throw new ValidationException();
    }
  }

  private static void validateTstB(InputStream is) throws ValidationException, IOException {
    LOG.warn("Not implemented");
    throw new ValidationException();
  }

  @Override
  public void run(String... args) throws Exception {
    YAMLFactory yamlFactory = new YAMLFactory();
    yamlFactory.configure(Feature.SPLIT_LINES, false);
    OBJECT_MAPPER = new ObjectMapper(yamlFactory);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    VALIDATOR = factory.getValidator();

    Namespace ns = parseArguments(args);
    LOG.info("Validating file {}", ns.getString("file"));
    try (InputStream is = Files.newInputStream(Paths.get(ns.getString("file")))) {
      switch (ns.getString("type")) {
        case "vsb":
          LOG.info("Selected type: Vertical Service Blueprint");
          validateVSB(is);
          break;
        case "cb":
          LOG.info("Selected type: Context Blueprint");
          validateCB(is);
          break;
        case "expb":
          LOG.info("Selected type: Experiment Blueprint");
          validateExpB(is);
          break;
        case "tstb":
          LOG.info("Selected type: Test Blueprint");
          validateTstB(is);
          break;
      }
      LOG.info("Validation success");
    } catch (UnrecognizedPropertyException | InvalidFormatException e) {
      LOG.error(e.getOriginalMessage());
      LOG.error("Error at line {}, column {} of YAML file", e.getLocation().getLineNr(),
          e.getLocation().getColumnNr());
      LOG.error("Validation failed");
    } catch (ValidationException e) {
      LOG.error("Validation failed");
    } catch (IOException e) {
      LOG.error("Can't read input file {}", e.getMessage());
    }
  }
}

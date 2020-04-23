package it.cnit.blueprint.validator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import it.nextworks.nfvmano.catalogue.blueprint.elements.*;
import it.nextworks.nfvmano.libs.ifa.common.DescriptorInformationElement;
import it.nextworks.nfvmano.libs.ifa.common.exceptions.MalformattedElementException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
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

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

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
        SpringApplication app = new SpringApplication(BlueprintValidatorApplication.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    private static Namespace parseArguments(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("validator").build()
                .defaultHelp(true)
                .description("Simple tool to validate blueprints for the 5G EVE platform.");

        parser.addArgument("--debug").action(Arguments.storeTrue());
        parser.addArgument("-t", "--type").choices("vsb", "ctx", "expb", "tcb").required(true)
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

    private static <T extends DescriptorInformationElement> void validate(InputStream is, Class<T> cls)
            throws ValidationException, IOException, MalformattedElementException {
        T b = OBJECT_MAPPER.readValue(is, cls);
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(b);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<T> v : violations) {
                LOG.error("Violation: property '{}' {}", v.getPropertyPath(), v.getMessage());
            }
            throw new ValidationException();
        }
        b.isValid();
        LOG.debug("Dump:\n{}", OBJECT_MAPPER.writeValueAsString(b));
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
                    validate(is, VsBlueprint.class);
                    break;
                case "ctx":
                    LOG.info("Selected type: Context Blueprint");
                    validate(is, CtxBlueprint.class);
                    break;
                case "expb":
                    LOG.info("Selected type: Experiment Blueprint");
                    validate(is, ExpBlueprint.class);
                    break;
                case "tcb":
                    LOG.info("Selected type: Test Case Blueprint");
                    validate(is, TestCaseBlueprint.class);
                    break;
            }
            LOG.info("Validation success");
        } catch (MismatchedInputException e) {
            LOG.error(e.getOriginalMessage());
            LOG.error("Error at line {}, column {} of YAML file", e.getLocation().getLineNr(),
                    e.getLocation().getColumnNr());
            LOG.error("Validation failed");
        } catch (ValidationException | MalformattedElementException e) {
            LOG.error(e.getMessage());
            LOG.error("Validation failed");
        } catch (IOException e) {
            LOG.error("Can't read input file {}", e.getMessage());
        }
    }
}

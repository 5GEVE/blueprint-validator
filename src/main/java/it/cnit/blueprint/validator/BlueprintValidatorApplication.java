package it.cnit.blueprint.validator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import it.nextworks.nfvmano.catalogue.blueprint.elements.CtxBlueprint;
import it.nextworks.nfvmano.catalogue.blueprint.elements.ExpBlueprint;
import it.nextworks.nfvmano.catalogue.blueprint.elements.TestCaseBlueprint;
import it.nextworks.nfvmano.catalogue.blueprint.elements.VsBlueprint;
import it.nextworks.nfvmano.libs.ifa.common.DescriptorInformationElement;
import it.nextworks.nfvmano.libs.ifa.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.ifa.descriptors.nsd.Nsd;
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
import javax.validation.ValidatorFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    enum TYPE {
        vsb,
        ctx,
        expb,
        tcb,
        nsd
    }

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
        parser.addArgument("-t", "--type").type(TYPE.class).required(true)
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

    /**
     * @param is  inputstream containing the blueprint to be validated
     * @param cls the specific blueprint class to use for validation (e.g. VsBlueprint.class)
     * @param <T> The type of the class to be validated
     * @throws IOException                  if Jackson fails to deserialize the blueprint; it can be JsonParseException or JsonMappingException
     * @throws ViolationException           if javax.validation fails
     * @throws MalformattedElementException if call to isValid() fails
     */
    private static <T extends DescriptorInformationElement> void validate(InputStream is, Class<T> cls)
            throws IOException, ViolationException, MalformattedElementException {
        T b = OBJECT_MAPPER.readValue(is, cls);
        LOG.debug("Dump:\n{}", OBJECT_MAPPER.writeValueAsString(b));
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(b);
        if (!violations.isEmpty()) {
            throw new ViolationException(violations);
        }
        b.isValid();
    }

    @Override
    public void run(String... args) {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(Feature.SPLIT_LINES, false);
        OBJECT_MAPPER = new ObjectMapper(yamlFactory);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();

        Namespace ns = parseArguments(args);
        LOG.info("Validating file {}", ns.getString("file"));
        try (InputStream is = Files.newInputStream(Paths.get(ns.getString("file")))) {
            switch ((TYPE) ns.get("type")) {
                case vsb:
                    LOG.info("Selected type: Vertical Service Blueprint");
                    validate(is, VsBlueprint.class);
                    break;
                case ctx:
                    LOG.info("Selected type: Context Blueprint");
                    validate(is, CtxBlueprint.class);
                    break;
                case expb:
                    LOG.info("Selected type: Experiment Blueprint");
                    validate(is, ExpBlueprint.class);
                    break;
                case tcb:
                    LOG.info("Selected type: Test Case Blueprint");
                    validate(is, TestCaseBlueprint.class);
                    break;
                case nsd:
                    LOG.info("Selected type: Network Service Descriptor");
                    boolean collection = false;
                    char prev = '*';
                    int i;
                    while (!collection) {
                        i = is.read();
                        char curr = (char) i;
                        if (prev == '-' && curr == ' ') {
                            collection = true;
                        }
                        else {
                            prev = curr;
                        }
                    }
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                    StringBuffer sb = new StringBuffer();
//                    String str;
//                    while((str = reader.readLine())!= null){
//                        sb.append(str);
//                    }
//                    LOG.debug(sb.toString());
                    validate(is, Nsd.class);
                    break;

            }
            LOG.info("Validation success");
        } catch (JsonParseException | JsonMappingException e) {
            LOG.error(e.getOriginalMessage());
            LOG.error("Error at line {}, column {} of YAML file", e.getLocation().getLineNr(),
                    e.getLocation().getColumnNr());
        } catch (ViolationException e) {
            for (String v : e.getViolationMessages()) {
                LOG.error(v);
            }
        } catch (MalformattedElementException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error("Can't read input file {}", e.getMessage());
        }
    }
}

package it.cnit.blueprint.validator;

import javax.validation.ConstraintViolation;
import java.util.Set;

public class ViolationException extends Exception {

    Set<String> violationMessages;

    public <T> ViolationException(Set<ConstraintViolation<T>> violations) {
        for (ConstraintViolation<T> v : violations) {
            assert false;
            violationMessages.add("Property " + v.getPropertyPath() + " " + v.getMessage());
        }
    }

    public Set<String> getViolationMessages() {
        return violationMessages;
    }
}

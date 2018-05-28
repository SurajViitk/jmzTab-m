/*
 * Copyright 2018 Leibniz-Institut für Analytische Wissenschaften – ISAS – e.V..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.isas.lipidomics.jmztabm.validation.validators;

import de.isas.lipidomics.jmztabm.validation.constraints.CheckParameter;
import de.isas.mztab1_1.model.Parameter;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 *
 * @author Leibniz-Institut für Analytische Wissenschaften – ISAS – e.V.
 */
public interface ParameterValidator extends ConstraintValidator<CheckParameter, Parameter> {

    default boolean isValid(Parameter parameter,
        ConstraintValidatorContext context) {
        if (parameter == null) {
            return true;
        }
        if ((parameter.getCvLabel() != null && parameter.getCvAccession() != null && parameter.
            getName() != null) || (parameter.getName() != null && parameter.
            getValue() != null)) {
            return true;
        }
        return false;
    }
}

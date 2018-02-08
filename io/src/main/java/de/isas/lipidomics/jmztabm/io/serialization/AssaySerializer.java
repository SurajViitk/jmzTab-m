/*
 * Copyright 2017 Leibniz Institut für Analytische Wissenschaften - ISAS e.V..
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
package de.isas.lipidomics.jmztabm.io.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import static de.isas.lipidomics.jmztabm.io.serialization.Serializers.addLineWithProperty;
import static de.isas.lipidomics.jmztabm.io.serialization.Serializers.addSubElementStrings;
import static de.isas.lipidomics.jmztabm.io.serialization.Serializers.addLineWithPropertyParameters;
import de.isas.mztab1_1.model.Assay;
import de.isas.mztab1_1.model.MsRun;
import de.isas.mztab1_1.model.Sample;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import uk.ac.ebi.pride.jmztab1_1.model.Section;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public class AssaySerializer extends StdSerializer<Assay> {

    public AssaySerializer() {
        this(null);
    }

    public AssaySerializer(Class<Assay> t) {
        super(t);
    }

    @Override
    public void serialize(Assay assay, JsonGenerator jg, SerializerProvider sp) throws IOException {
        if (assay != null) {
            addLineWithProperty(jg, Section.Metadata.getPrefix(), "name", assay,
                assay.
                    getName());
            addLineWithProperty(jg, Section.Metadata.getPrefix(), "external_uri",
                assay, assay.getExternalUri());

            addLineWithPropertyParameters(jg, Section.Metadata.getPrefix(),
                "custom",
                assay, assay.getCustom());

            addSubElementStrings(jg, Section.Metadata.getPrefix(), assay,
                "ms_run_ref", Arrays.asList(assay.getMsRunRef()).
                    stream().
                    sorted(Comparator.comparing(MsRun::getId,
                        Comparator.nullsFirst(Comparator.
                            naturalOrder())
                    )).
                    map((msRunRef) ->
                    {
                        return new StringBuilder().append("ms_run").
                            append("[").
                            append(msRunRef.getId()).
                            append("]").
                            toString();
                    }).
                    collect(Collectors.toList()), true);
            addSubElementStrings(jg, Section.Metadata.getPrefix(), assay,
                "sample_ref", Arrays.asList(assay.getSampleRef()).
                    stream().
                    sorted(Comparator.comparing(Sample::getId,
                        Comparator.nullsFirst(Comparator.
                            naturalOrder())
                    )).
                    map((assayRef) ->
                    {
                        return new StringBuilder().append("sample").
                            append("[").
                            append(assayRef.getId()).
                            append("]").
                            toString();
                    }).
                    collect(Collectors.toList()), true);

        } else {
            System.err.println("Assay is null!");
        }
    }
}

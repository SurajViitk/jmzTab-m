/*
 * 
 */
package de.isas.lipidomics.jmztabm.io.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import static de.isas.lipidomics.jmztabm.io.serialization.Serializers.writeAsNumberArray;
import static de.isas.lipidomics.jmztabm.io.serialization.Serializers.writeAsStringArray;
import de.isas.mztab1_1.model.SmallMoleculeSummary;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public class SmallMoleculeSummarySerializer extends StdSerializer<SmallMoleculeSummary> {

    public SmallMoleculeSummarySerializer() {
        this(null);
    }

    public SmallMoleculeSummarySerializer(Class<SmallMoleculeSummary> t) {
        super(t);
    }

    @Override
    public void serialize(SmallMoleculeSummary smallMoleculeSummary,
        JsonGenerator jg,
        SerializerProvider sp) throws IOException {
        if (smallMoleculeSummary != null) {

            jg.writeString("SML");
            jg.writeString(smallMoleculeSummary.getSmlId());
            jg.writeStartArray();
            writeAsStringArray(jg, smallMoleculeSummary.getSmfIdRefs());
            writeAsStringArray(jg, smallMoleculeSummary.getDatabaseIdentifier());
            writeAsStringArray(jg, smallMoleculeSummary.getChemicalFormula());
            writeAsStringArray(jg, smallMoleculeSummary.getSmiles());
            writeAsStringArray(jg, smallMoleculeSummary.getInchi());
            writeAsStringArray(jg, smallMoleculeSummary.getChemicalName());
            writeAsStringArray(jg, smallMoleculeSummary.getUri());
            writeAsNumberArray(jg, smallMoleculeSummary.
                getTheoreticalNeutralMass());
            jg.writeNumber(smallMoleculeSummary.getExpMassToCharge());
            jg.writeNumber(smallMoleculeSummary.getRetentionTime());
            writeAsStringArray(jg, smallMoleculeSummary.getAdductIons());
            jg.writeString(smallMoleculeSummary.getReliability());
            jg.writeString(ParameterSerializer.toString(smallMoleculeSummary.
                getBestIdConfidenceMeasure()));
            jg.writeNumber(smallMoleculeSummary.getBestIdConfidenceValue());
            smallMoleculeSummary.getAbundanceAssay().
                forEach((abundance_assay) ->
                {
                    try {
                        jg.writeNumber(abundance_assay);
                    } catch (IOException ex) {
                        Logger.getLogger(SmallMoleculeSummarySerializer.class.
                            getName()).
                            log(Level.SEVERE, null, ex);
                    }
                });

            smallMoleculeSummary.getAbundanceStudyVariable().
                forEach((abundance_sv) ->
                {
                    try {
                        jg.writeNumber(abundance_sv);
                    } catch (IOException ex) {
                        Logger.getLogger(SmallMoleculeSummarySerializer.class.
                            getName()).
                            log(Level.SEVERE, null, ex);
                    }
                });

            smallMoleculeSummary.getAbundanceCoeffvarStudyVariable().
                forEach((abundance_coeffvar_sv) ->
                {
                    try {
                        jg.writeNumber(abundance_coeffvar_sv);
                    } catch (IOException ex) {
                        Logger.getLogger(SmallMoleculeSummarySerializer.class.
                            getName()).
                            log(Level.SEVERE, null, ex);
                    }
                });
            //TODO opt columns
        } else {
            System.err.println("StudyVariable is null!");
        }
    }
}
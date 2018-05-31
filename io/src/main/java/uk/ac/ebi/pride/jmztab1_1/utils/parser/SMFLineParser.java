package uk.ac.ebi.pride.jmztab1_1.utils.parser;

import uk.ac.ebi.pride.jmztab1_1.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab1_1.model.ISmallMoleculeFeatureColumn;
import uk.ac.ebi.pride.jmztab1_1.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab1_1.model.MZBoolean;
import uk.ac.ebi.pride.jmztab1_1.model.IMZTabColumn;
import uk.ac.ebi.pride.jmztab1_1.model.SmallMoleculeFeatureColumn;
import uk.ac.ebi.pride.jmztab1_1.model.OptionColumn;
import uk.ac.ebi.pride.jmztab1_1.model.AbundanceColumn;
import de.isas.mztab1_1.model.Metadata;
import de.isas.mztab1_1.model.OptColumnMapping;
import de.isas.mztab1_1.model.SmallMoleculeFeature;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorList;


/**
 * <p>SMFLineParser class.</p>
 *
 * @author nilshoffmann
 * @since 11/09/17
 * 
 */
public class SMFLineParser extends MZTabDataLineParser<SmallMoleculeFeature> {

    private SmallMoleculeFeature smallMoleculeFeature;

    /**
     * <p>Constructor for SMFLineParser.</p>
     *
     * @param context a {@link uk.ac.ebi.pride.jmztab1_1.utils.parser.MZTabParserContext} object.
     * @param factory a {@link uk.ac.ebi.pride.jmztab1_1.model.MZTabColumnFactory} object.
     * @param positionMapping a {@link uk.ac.ebi.pride.jmztab1_1.utils.parser.PositionMapping} object.
     * @param metadata a {@link de.isas.mztab1_1.model.Metadata} object.
     * @param errorList a {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorList} object.
     */
    public SMFLineParser(MZTabParserContext context, MZTabColumnFactory factory, PositionMapping positionMapping,
                         Metadata metadata, MZTabErrorList errorList) {
        super(context, factory, positionMapping, metadata, errorList);
    }

    /** {@inheritDoc} */
    @Override
    protected int checkData() {

        IMZTabColumn column;
        String columnName;
        String target;
        int physicalPosition;
        String logicalPosition;
        smallMoleculeFeature = new SmallMoleculeFeature();//(factory, metadata);

        for (physicalPosition = 1; physicalPosition < items.length; physicalPosition++) {
            logicalPosition = positionMapping.get(physicalPosition);
            column = factory.getColumnMapping().get(logicalPosition);

            if (column != null) {
                columnName = column.getName();
                target = items[physicalPosition];
                if (column instanceof ISmallMoleculeFeatureColumn) {
                    SmallMoleculeFeatureColumn.Stable stableColumn = SmallMoleculeFeatureColumn.Stable.forName(columnName);
                    switch(stableColumn) {
                        case ADDUCT_ION:
                            smallMoleculeFeature.adductIon(checkString(column,
                                target));
                            break;
                        case CHARGE:
                            smallMoleculeFeature.charge(checkInteger(column,
                                target));
                            break;
                        case EXP_MASS_TO_CHARGE:
                            smallMoleculeFeature.expMassToCharge(
                                checkDouble(column, target));
                            break;
                        case ISOTOPOMER:
                            smallMoleculeFeature.isotopomer(checkParameter(
                                column, target, true));
                            break;
                        case RETENTION_TIME_IN_SECONDS:
                            smallMoleculeFeature.retentionTimeInSeconds(checkDouble(
                                column, target));
                            break;
                        case RETENTION_TIME_IN_SECONDS_END:
                            smallMoleculeFeature.retentionTimeInSecondsEnd(checkDouble(
                                column, target));
                            break;
                        case RETENTION_TIME_IN_SECONDS_START:
                            smallMoleculeFeature.retentionTimeInSecondsStart(checkDouble(
                                column, target));
                            break;
                        case SME_ID_REFS:
                            smallMoleculeFeature.smeIdRefs(checkStringList(
                                column, target, MZTabConstants.BAR));
                            break;
                        case SME_ID_REF_AMBIGUITY_CODE:
                            smallMoleculeFeature.smeIdRefAmbiguityCode(checkInteger(
                                column, target));
                            break;
                        case SMF_ID:
                            smallMoleculeFeature.smfId(checkString(
                                column, target));
                            break;
                    }

                } else if (column instanceof AbundanceColumn) {
                    //Double check, the column name should contain
                    //Double check, the column name should contain
                    if (columnName.startsWith(SmallMoleculeFeature.Properties.abundanceAssay.getPropertyName())) {
                        smallMoleculeFeature.addAbundanceAssayItem(checkDouble(column, target));
                    }
                } else if (column instanceof OptionColumn) {
                    //Double check, the column name should opt
                   if (columnName.startsWith(MZTabConstants.OPT_PREFIX)) {
                        Class dataType = column.getDataType();
                        OptColumnMapping optColMapping = new OptColumnMapping();
                        optColMapping.identifier(columnName.substring(MZTabConstants.OPT_PREFIX.length()));
                        if (dataType.equals(String.class)) {
                            optColMapping.value(checkString(column, target));
                        } else if (dataType.equals(Double.class)) {
                            optColMapping.value(Double.toString(checkDouble(column, target)));
                        } else if (dataType.equals(MZBoolean.class)) {
                            optColMapping.value(Boolean.toString(checkMZBoolean(column, target).toBoolean()));
                        }
                        smallMoleculeFeature.addOptItem(optColMapping);
                    }
                }
            }
        }

        return physicalPosition;
    }

    /** {@inheritDoc} */
    @Override
    public SmallMoleculeFeature getRecord() {

        if(smallMoleculeFeature == null){
            smallMoleculeFeature = new SmallMoleculeFeature();//(factory, metadata);
        }
        return smallMoleculeFeature;
    }
}

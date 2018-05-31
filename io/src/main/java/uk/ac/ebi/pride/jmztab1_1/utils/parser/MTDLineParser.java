package uk.ac.ebi.pride.jmztab1_1.utils.parser;

import uk.ac.ebi.pride.jmztab1_1.model.MetadataProperty;
import uk.ac.ebi.pride.jmztab1_1.model.MetadataSubElement;
import uk.ac.ebi.pride.jmztab1_1.model.MetadataElement;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabException;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.FormatErrorType;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.LogicalErrorType;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException;
import de.isas.mztab1_1.model.Assay;
import de.isas.mztab1_1.model.CV;
import de.isas.mztab1_1.model.Contact;
import de.isas.mztab1_1.model.Database;
import de.isas.mztab1_1.model.IndexedElement;
import de.isas.mztab1_1.model.Instrument;
import de.isas.mztab1_1.model.Metadata;
import de.isas.mztab1_1.model.MsRun;
import de.isas.mztab1_1.model.Parameter;
import de.isas.mztab1_1.model.Publication;
import de.isas.mztab1_1.model.Sample;
import de.isas.mztab1_1.model.SampleProcessing;
import de.isas.mztab1_1.model.Software;
import de.isas.mztab1_1.model.StudyVariable;
import de.isas.mztab1_1.model.Uri;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.ac.ebi.pride.jmztab1_1.model.MZTabUtils;

import static uk.ac.ebi.pride.jmztab1_1.model.MZTabUtils.*;
import uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType;

/**
 * Parse a metadata line into a element. Metadata Element start with MTD, its
 * structure like: MTD
 * {@link uk.ac.ebi.pride.jmztab1_1.model.MetadataElement}([id])(-{@link uk.ac.ebi.pride.jmztab1_1.model.MetadataSubElement}[pid])(-{@link uk.ac.ebi.pride.jmztab1_1.model.MetadataProperty})    {Element Value}
 *
 * @see MetadataElement
 * @see MetadataSubElement
 * @see MetadataProperty
 * @author qingwei
 * @since 08/02/13
 * 
 */
public class MTDLineParser extends MZTabLineParser {

    private static final String Error_Header = Metadata.PrefixEnum.MTD.getValue()+"\t";
    
    public static final String normalMetadataRegexp = "(\\w+)(\\[(\\w+)\\])?(-(\\w+)(\\[(\\w+)\\])?)?(-(\\w+))?";

    private final Metadata metadata = new Metadata();

    /**
     * <p>Constructor for MTDLineParser.</p>
     *
     * @param context a {@link uk.ac.ebi.pride.jmztab1_1.utils.parser.MZTabParserContext} object.
     */
    public MTDLineParser(MZTabParserContext context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     *
     * Most of e, we use {@link #parseNormalMetadata(String, String)} to
     * parse defineLabel into Metadata Element.
     */
    @Override
    public void parse(int lineNumber, String mtdLine, MZTabErrorList errorList) throws MZTabException {
        super.parse(lineNumber, mtdLine, errorList);

        if (items.length != 3) {
            MZTabError error = new MZTabError(FormatErrorType.MTDLine,
                lineNumber, mtdLine);
            throw new MZTabException(error);
        }

        String defineLabel = items[1].trim().
            toLowerCase();
        String valueLabel = items[2].trim();

        parseNormalMetadata(defineLabel, valueLabel);
    }

    /**
     * Parse valueLabel based on email format. If exists parse error, add it
     * into {@link MZTabErrorList}.
     */
    private String checkEmail(String defineLabel, String valueLabel) {
        String email = parseEmail(valueLabel);

        if (email == null) {
            errorList.add(new MZTabError(FormatErrorType.Email, lineNumber,
                Error_Header + defineLabel, valueLabel));
        }

        return email;
    }

    /**
     * Parse {@link MetadataProperty} which depend on the
     * {@link MetadataElement}. If exists parse error, stop validate and throw
     * {@link MZTabException} directly.
     */
    private MetadataProperty checkProperty(MetadataElement element,
        String propertyName) throws MZTabException {
        if (isEmpty(propertyName)) {
            return null;
        }

        MetadataProperty property = MetadataProperty.findProperty(element,
            propertyName);
        if (property == null) {
            MZTabError error = new MZTabError(FormatErrorType.MTDDefineLabel,
                lineNumber, element.getName() + "-" + propertyName);
            throw new MZTabException(error);
        }

        return property;
    }

    /**
     * Parse valueLabel to {@link Parameter} If exists parse error, add it into
     * {@link MZTabErrorList}
     */
    private Parameter checkParameter(String defineLabel, String valueLabel) {
        Parameter param = parseParam(valueLabel);
        if (param == null) {
            errorList.add(new MZTabError(FormatErrorType.Param, lineNumber,
                Error_Header + defineLabel, valueLabel));
        }
        return param;
    }

    /**
     * Parse valueLabel to a list of '|' separated parameters. If exists parse
     * error, add it into {@link MZTabErrorList}
     */
    private List<Parameter> checkParameterList(String defineLabel,
        String valueLabel) {
        List<Parameter> paramList = parseParamList(valueLabel);

        if (paramList.isEmpty()) {
            errorList.add(new MZTabError(FormatErrorType.ParamList, lineNumber,
                Error_Header + defineLabel, valueLabel));
        }

        return paramList;
    }

    /**
     * Parse valueLabel to a list of '|' separated parameters. If exists parse
     * error, add it into {@link MZTabErrorList}
     */
    private Publication checkPublication(Integer id, String defineLabel,
        String valueLabel) {
        if (!context.getPublicationMap().
            containsKey(id)) {
            context.addPublication(metadata, new Publication().id(id));
        }
        Publication publications = parsePublicationItems(context.
            getPublicationMap().
            get(id), valueLabel);

        if (publications.getPublicationItems() == null || publications.
            getPublicationItems().
            isEmpty()) {
            errorList.add(
                new MZTabError(FormatErrorType.Publication, lineNumber,
                    Error_Header + defineLabel, valueLabel));
        }

        return publications;
    }

    /**
     * Parse valueLabel to a {@link java.net.URI} If exists parse error, add it
     * into {@link MZTabErrorList}
     */
    private java.net.URI checkURI(String defineLabel, String valueLabel) {
        java.net.URI uri = parseURI(valueLabel);
        if (uri == null) {
            errorList.add(new MZTabError(FormatErrorType.URI, lineNumber,
                Error_Header + defineLabel, valueLabel));
        }

        return uri;
    }

    /**
     * Parse valueLabel to {@link java.net.URL} If exists parse error, add it
     * into {@link MZTabErrorList}
     */
    private java.net.URL checkURL(String defineLabel, String valueLabel) {

        if (null == parseString(valueLabel)) {
            // "null" value is supported when the ms_run[1-n]-location is unknown
            errorList.add(new MZTabError(LogicalErrorType.NotNULL, lineNumber,
                Error_Header + defineLabel, valueLabel));
            return null;
        }

        java.net.URL url = parseURL(valueLabel);
        if (url == null) {  //Malformed exception
            errorList.add(new MZTabError(FormatErrorType.URL, lineNumber,
                Error_Header + defineLabel, valueLabel));
            return null;
        }

        return url;
    }

    /**
     * Parse defineLabel to a index id number. If exists parse error, stop
     * validate and throw {@link MZTabException} directly.
     */
    private int checkIndex(String defineLabel, String id) throws MZTabException {
        try {
            Integer index = Integer.parseInt(id);
            if (index < 1) {
                throw new NumberFormatException();
            }

            return index;
        } catch (NumberFormatException e) {
            MZTabError error = new MZTabError(LogicalErrorType.IdNumber,
                lineNumber, Error_Header + defineLabel, id);
            throw new MZTabException(error);
        }
    }

    /**
     * Parse valueLabel to a {@link IndexedElement} If exists parse error, stop
     * validate and throw {@link MZTabException} directly.
     */
    private IndexedElement checkIndexedElement(String defineLabel,
        String valueLabel, MetadataElement element) throws MZTabException {
        IndexedElement indexedElement = parseIndexedElement(valueLabel, element);
        if (indexedElement == null) {
            MZTabError error = new MZTabError(FormatErrorType.IndexedElement,
                lineNumber, Error_Header + defineLabel, valueLabel);
            throw new MZTabException(error);
        }

        return indexedElement;
    }

    /**
     * Parse valueLabel to a {@link IndexedElement} list. If exists parse error,
     * stop validate and throw {@link MZTabException} directly.
     */
    private List<IndexedElement> checkIndexedElementList(String defineLabel,
        String valueLabel, MetadataElement element) throws MZTabException {
        List<IndexedElement> indexedElementList = parseRefList(valueLabel,
            element);
        if (indexedElementList == null || indexedElementList.isEmpty()) {
            MZTabError error = new MZTabError(FormatErrorType.IndexedElement,
                lineNumber, Error_Header + defineLabel, valueLabel);
            throw new MZTabException(error);
        }
        return indexedElementList;
    }

    /**
     * The metadata line including three parts: MTD {defineLabel} {valueLabel}
     *
     * In normal, define label structure like:
     * {@link MetadataElement}([id])(-{@link MetadataSubElement}[pid])(-{@link MetadataProperty})
     *
     * @see MetadataElement : Mandatory
     * @see MetadataSubElement : Optional
     * @see MetadataProperty : Optional.
     *
     * If exists parse error, add it into {@link MZTabErrorList}
     */
    private void parseNormalMetadata(String defineLabel, String valueLabel) throws MZTabException {
        Pattern pattern = Pattern.compile(normalMetadataRegexp);
        Matcher matcher = pattern.matcher(defineLabel);

        if (matcher.find()) {
            // Stage 1: create Unit.
            MetadataElement element = MetadataElement.findElement(matcher.group(
                1));
            if (element == null) {
                throw new MZTabException(new MZTabError(
                    FormatErrorType.MTDDefineLabel, lineNumber, defineLabel));
            }

            switch (element) {
                case MZTAB:
                    handleMzTab(element, matcher, defineLabel, valueLabel);
                    break;
                case TITLE:
                    handleTitle(defineLabel, valueLabel);
                    break;
                case DESCRIPTION:
                    handleDescription(defineLabel, valueLabel);
                    break;
                case SAMPLE_PROCESSING:
                    handleSampleProcessing(defineLabel, matcher, valueLabel);
                    break;
                case INSTRUMENT:
                    handleInstrument(defineLabel, matcher, element, valueLabel);
                    break;
                case SOFTWARE:
                    handleSoftware(defineLabel, matcher, element, valueLabel);
                    break;
                case PUBLICATION:
                    handlePublication(defineLabel, matcher, valueLabel);
                    break;
                case CONTACT:
                    handleContact(defineLabel, matcher, element, valueLabel);
                    break;
                case URI:
                    handleUri(defineLabel, matcher, valueLabel);
                    break;
                case EXTERNAL_STUDY_URI:
                    handleExternalStudyUri(defineLabel, matcher, valueLabel);
                    break;
                case QUANTIFICATION_METHOD:
                    handleQuantificationMethod(defineLabel, valueLabel);
                    break;
                case SMALL_MOLECULE:
                    handleSmallMolecule(element, matcher, defineLabel,
                valueLabel);
                    break;
                case SMALL_MOLECULE_FEATURE:
                    handleSmallMoleculeFeature(element, matcher, defineLabel,
                valueLabel);
                    break;
                case MS_RUN:
                    handleMsRun(defineLabel, matcher, element, valueLabel);

                    break;
                case CUSTOM:
                    handleCustom(defineLabel, matcher, valueLabel);
                    break;
                case SAMPLE:
                    handleSample(defineLabel, matcher, element, valueLabel);
                    break;
                case ASSAY:
                    handleAssay(matcher, defineLabel, element, valueLabel);
                    break;
                case STUDY_VARIABLE:
                    handleStudyVariable(defineLabel, matcher, element,
                valueLabel);
                    break;
                case CV:
                    handleCv(defineLabel, matcher, element, valueLabel);
                    break;
                case DATABASE:
                    handleDatabase(defineLabel, matcher, element, valueLabel);
                    break;
                case COLUNIT:
                case COLUNIT_SMALL_MOLECULE:
                case COLUNIT_SMALL_MOLECULE_FEATURE:
                case COLUNIT_SMALL_MOLECULE_EVIDENCE:
                    handleColunit(defineLabel, valueLabel);
                    break;
                case ID_CONFIDENCE_MEASURE:
                    handleIdConfidenceMeasure(defineLabel, matcher, valueLabel);
                    break;
            }

        } else {
            throw new MZTabException(new MZTabError(FormatErrorType.MTDLine,
                lineNumber, line));
        }
    }

    protected void handleIdConfidenceMeasure(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        context.addIdConfidenceMeasure(metadata, id, checkParameter(
            defineLabel, valueLabel));
    }

    protected void handleColunit(String defineLabel, String valueLabel) throws MZTabErrorOverflowException {
        // In this stage, just store them into colUnitMap<defineLabel, valueLabel>.
        // after the section columns is created we will add the col unit.
        if (!defineLabel.equals("colunit-protein")
            && !defineLabel.equals("colunit-peptide")
            && !defineLabel.equals("colunit-psm")
            && !defineLabel.equals(Metadata.Properties.colunitSmallMolecule.getPropertyName())
            && !defineLabel.equals(Metadata.Properties.colunitSmallMoleculeEvidence.getPropertyName())
            && !defineLabel.equals(Metadata.Properties.colunitSmallMoleculeFeature.getPropertyName())) {
            errorList.add(new MZTabError(
                FormatErrorType.MTDDefineLabel, lineNumber,
                defineLabel));
        } else {
            context.getColUnitMap().
                put(defineLabel, valueLabel);
        }
    }

    protected void handleDatabase(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addDatabase(metadata, property, id, defineLabel, valueLabel);
    }

    protected void handleCv(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addCv(metadata, property, id, valueLabel);
    }

    protected void handleStudyVariable(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException, MZTabErrorOverflowException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addStudyVariable(metadata, property, defineLabel, valueLabel,
            id);
    }

    protected void handleAssay(Matcher matcher, String defineLabel,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        if (isEmpty(matcher.group(6))) {
            // no quantification modification. For example: assay[1-n]-quantification_reagent
            id = checkIndex(defineLabel, matcher.group(3));
            property = checkProperty(element, matcher.group(5));
            addAssay(metadata, property, defineLabel, valueLabel, id);
        } else {
            throw new MZTabException("assay does not support quantification modification!");
        }
    }

    protected void handleSample(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addSample(metadata, property, id, defineLabel, valueLabel);
    }

    protected void handleCustom(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        context.addCustomItem(metadata, id, checkParameter(
            defineLabel, valueLabel));
    }

    protected void handleMsRun(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addMsRun(metadata, property, id, defineLabel, valueLabel);
    }

    protected void handleSmallMoleculeFeature(MetadataElement element,
        Matcher matcher, String defineLabel, String valueLabel) throws MZTabException {
        MetadataProperty property;
        property = checkProperty(element, matcher.group(5));
        switch (property != null ? property : null) {
            case SMALL_MOLECULE_FEATURE_QUANTIFICATION_UNIT:
                if (metadata.
                    getSmallMoleculeFeatureQuantificationUnit() != null) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.DuplicationDefine,
                        lineNumber, defineLabel));
                }
                metadata.setSmallMoleculeFeatureQuantificationUnit(
                    checkParameter(defineLabel, valueLabel));
                break;
        }
    }

    protected void handleSmallMolecule(MetadataElement element, Matcher matcher,
        String defineLabel, String valueLabel) throws MZTabException {
        MetadataProperty property;
        property = checkProperty(element, matcher.group(5));
        switch (property != null ? property : null) {
            case SMALL_MOLECULE_QUANTIFICATION_UNIT:
                if (metadata.getSmallMoleculeQuantificationUnit() != null) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.DuplicationDefine,
                        lineNumber, defineLabel));
                }
                metadata.setSmallMoleculeQuantificationUnit(
                    checkParameter(defineLabel, valueLabel));
                break;
            case SMALL_MOLECULE_IDENTIFICATION_RELIABILITY:
                if (metadata.
                    getSmallMoleculeIdentificationReliability() != null) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.DuplicationDefine,
                        lineNumber, defineLabel));
                }
                metadata.setSmallMoleculeIdentificationReliability(
                    checkParameter(defineLabel, valueLabel));
                break;
        }
    }

    protected void handleQuantificationMethod(String defineLabel,
        String valueLabel) throws MZTabException {
        if (metadata.getQuantificationMethod() != null) {
            throw new MZTabException(new MZTabError(
                LogicalErrorType.DuplicationDefine, lineNumber,
                defineLabel));
        }
        metadata.setQuantificationMethod(checkParameter(
            defineLabel, valueLabel));
    }

    protected void handleExternalStudyUri(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        metadata.addExternalStudyUriItem(new Uri().id(id).value(checkURI(defineLabel, valueLabel).toASCIIString()));
    }

    protected void handleUri(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        metadata.addUriItem(new Uri().id(id).value(checkURI(defineLabel, valueLabel).toASCIIString()));
    }

    protected void handleContact(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addContact(metadata, property, id, valueLabel, defineLabel);
    }

    protected void handlePublication(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        checkPublication(id, defineLabel, valueLabel);
    }

    protected void handleSoftware(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabErrorOverflowException, MZTabException {
        Integer id;
        MetadataProperty property;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        addSoftware(metadata, property, defineLabel, valueLabel, id);
    }

    protected void handleInstrument(String defineLabel, Matcher matcher,
        MetadataElement element, String valueLabel) throws MZTabException {
        Integer id;
        MetadataProperty property;
        Parameter param;
        id = checkIndex(defineLabel, matcher.group(3));
        property = checkProperty(element, matcher.group(5));
        param = checkParameter(defineLabel, valueLabel);
        addInstrument(metadata, property, id, param);
    }

    protected void handleSampleProcessing(String defineLabel, Matcher matcher,
        String valueLabel) throws MZTabException {
        Integer id;
        id = checkIndex(defineLabel, matcher.group(3));
        addSampleProcessing(metadata, id, checkParameterList(
            defineLabel, valueLabel));
    }

    protected void handleDescription(String defineLabel, String valueLabel) throws MZTabException {
        if (metadata.getDescription() != null) {
            throw new MZTabException(new MZTabError(
                LogicalErrorType.DuplicationDefine, lineNumber,
                defineLabel));
        }
        metadata.setDescription(valueLabel);
    }

    protected void handleTitle(String defineLabel, String valueLabel) throws MZTabException {
        if (metadata.getTitle() != null) {
            throw new MZTabException(new MZTabError(
                LogicalErrorType.DuplicationDefine, lineNumber,
                defineLabel));
        }
        metadata.setTitle(valueLabel);
    }

    protected void handleMzTab(MetadataElement element, Matcher matcher,
        String defineLabel, String valueLabel) throws MZTabException {
        MetadataProperty property;
        property = checkProperty(element, matcher.group(5));
        switch (property) {
            case MZTAB_VERSION:
                if (metadata.getMzTabVersion()!= null) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.DuplicationDefine,
                        lineNumber, defineLabel));
                }
                if (MZTabUtils.parseMzTabVersion(valueLabel) == null) {
                    throw new MZTabException(new MZTabError(
                        FormatErrorType.MZTabVersion, lineNumber,
                        defineLabel, valueLabel));
                }
                metadata.mzTabVersion(valueLabel);
                break;
            case MZTAB_ID:
                if (metadata.getMzTabID() != null) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.DuplicationDefine,
                        lineNumber, defineLabel));
                }
                metadata.mzTabID(valueLabel);
                break;
        }
    }

    /**
     * Refine the metadata, and check whether missing some important
     * information. fixed_mode, variable_mode must provide in the Complete file.
     * Detail information see specification 5.5
     *
     * @throws uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabException if any.
     */
    public void refineNormalMetadata() throws MZTabException {
        SortedMap<Integer, StudyVariable> svMap = context.getStudyVariableMap();
        SortedMap<Integer, Assay> assayMap = context.getAssayMap();
        SortedMap<Integer, MsRun> runMap = context.getMsRunMap();

        if (metadata.getQuantificationMethod() == null) {
            throw new MZTabException(new MZTabError(
                LogicalErrorType.NotDefineInMetadata, lineNumber,
                Metadata.Properties.quantificationMethod.getPropertyName()));
        }

        for (Integer id : assayMap.keySet()) {
            if (assayMap.get(id).
                getMsRunRef() == null) {
                throw new MZTabException(new MZTabError(
                    LogicalErrorType.NotDefineInMetadata, lineNumber,
                    Metadata.Properties.assay+"[" + id + "]-ms_run_ref"));
            }
        }
        if (svMap.size() > 0 && assayMap.size() > 0) {
            for (Integer id : svMap.keySet()) {
                if (svMap.get(id).
                    getAssayRefs().
                    size() == 0) {
                    throw new MZTabException(new MZTabError(
                        LogicalErrorType.AssayRefs, lineNumber,
                        Metadata.Properties.studyVariable+"[" + id + "]-assay_refs"));
                }
            }
        }

        if (metadata.getDescription() == null) {
            throw new MZTabException(new MZTabError(
                LogicalErrorType.NotDefineInMetadata, lineNumber, "description",
                metadata.getMzTabVersion()));
        }
        for (Integer id : runMap.keySet()) {
            if (runMap.get(id).
                getLocation() == null) {
                throw new MZTabException(new MZTabError(
                    LogicalErrorType.NotDefineInMetadata, lineNumber,
                    Metadata.Properties.msRun+"[" + id + "]-location", metadata.getMzTabVersion()));
            }
        }

    }

    /**
     * <p>Getter for the field <code>metadata</code>.</p>
     *
     * @return a {@link de.isas.mztab1_1.model.Metadata} object.
     */
    public Metadata getMetadata() {
        return metadata;
    }

    private void addSampleProcessing(Metadata metadata, Integer id,
        List<Parameter> checkParameterList) throws MZTabException {
        SampleProcessing sp = context.addSampleProcessing(metadata, id,
            checkParameterList);
        if (sp == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.sampleProcessing+"[" + id + "]"));
        }
    }

    /**
     * <p>handleParam.</p>
     *
     * @param defineLabel a {@link java.lang.String} object.
     * @param valueLabel a {@link java.lang.String} object.
     * @param errorType a {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType} object.
     * @param lineNumber a int.
     * @param consumer a {@link java.util.function.Consumer} object.
     * @throws uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException if any.
     */
    public void handleParam(String defineLabel, String valueLabel,
        MZTabErrorType errorType, int lineNumber,
        Consumer<Parameter> consumer) throws MZTabErrorOverflowException {
        Parameter param;
        param = checkParameter(defineLabel, valueLabel);
        if (param != null && (param.getValue() == null || param.getValue().
            trim().
            length() == 0)) {
            // this is a warn.
            errorList.add(new MZTabError(errorType, lineNumber, valueLabel));
        } else {
            consumer.accept(param);
        }
    }

    private void addInstrument(Metadata metadata, MetadataProperty property,
        Integer id,
        Parameter param) throws MZTabException {
        Instrument instrument = null;

        switch (property != null ? property : null) {
            case INSTRUMENT_NAME:
                instrument = context.addInstrumentName(metadata, id, param);
                break;
            case INSTRUMENT_SOURCE:
                instrument = context.addInstrumentSource(metadata, id, param);
                break;
            case INSTRUMENT_ANALYZER:
                instrument = context.addInstrumentAnalyzer(metadata, id, param);
                break;
            case INSTRUMENT_DETECTOR:
                instrument = context.addInstrumentDetector(metadata, id, param);
                break;
        }
        if (instrument == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.instrument+"[" + id + "]"));
        }
    }

    private void addSoftware(Metadata metadata, MetadataProperty property,
        String defineLabel,
        String valueLabel, Integer id) throws MZTabErrorOverflowException, MZTabException {
        Parameter param;
        Software software = null;
        if (property == null) {
            param = checkParameter(defineLabel, valueLabel);
            if (param != null && (param.getValue() == null || param.getValue().
                trim().
                length() == 0)) {
                // this is a warn.
                errorList.add(new MZTabError(LogicalErrorType.SoftwareVersion,
                    lineNumber, valueLabel));
            }
            software = context.addSoftwareParameter(metadata, id, param);
        } else {
            switch (property) {
                case SOFTWARE_SETTING:
                    software = context.addSoftwareSetting(metadata, id,
                        valueLabel);
                    break;
            }
        }
        if (software == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.software+"[" + id + "]"));
        }
    }

    private void addContact(Metadata metadata, MetadataProperty property,
        Integer id,
        String valueLabel, String defineLabel) throws MZTabException {
        Contact contact = null;
        switch (property != null ? property : null) {
            case CONTACT_NAME:
                contact = context.addContactName(metadata, id, valueLabel);
                break;
            case CONTACT_AFFILIATION:
                contact = context.
                    addContactAffiliation(metadata, id, valueLabel);
                break;
            case CONTACT_EMAIL:
                checkEmail(defineLabel, valueLabel);
                contact = context.addContactEmail(metadata, id, valueLabel);
                break;
        }
        if (contact == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.contact+"[" + id + "]"));
        }
    }

    private void addPublication(Metadata metadata, Publication publication) {
        context.addPublication(metadata, publication);
    }

    private void addMsRun(Metadata metadata, MetadataProperty property,
        Integer id,
        String defineLabel, String valueLabel) throws MZTabException {
        MsRun msRun = null;
        if (property == null) {
            msRun = context.addMsRun(metadata, new MsRun().id(id).
                name(valueLabel));
        } else {
            switch (property) {
                case MS_RUN_FORMAT:
                    msRun = context.addMsRunFormat(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
                case MS_RUN_LOCATION:
                    msRun = context.addMsRunLocation(metadata, id, checkURL(
                        defineLabel, valueLabel));
                    break;
                case MS_RUN_INSTRUMENT_REF:
                    List<IndexedElement> indexedElements = checkIndexedElementList(defineLabel, valueLabel,
                        MetadataElement.INSTRUMENT);
                    if (indexedElements != null && !indexedElements.isEmpty() && indexedElements.size()==1) {
                        Instrument instrument = context.getInstrumentMap().
                            get(indexedElements.get(0).getId());
                        if (instrument == null) {
                            throw new MZTabException(new MZTabError(
                                LogicalErrorType.NotDefineInMetadata, lineNumber,
                                valueLabel,
                                valueLabel));
                        }
                        msRun = context.addMsRunInstrumentRef(metadata, id, instrument);
                    }
                    break;
                case MS_RUN_ID_FORMAT:
                    msRun = context.addMsRunIdFormat(metadata, id,
                        checkParameter(defineLabel, valueLabel));
                    break;
                case MS_RUN_FRAGMENTATION_METHOD:
                    msRun = context.addMsRunFragmentationMethod(metadata, id,
                        checkParameter(defineLabel, valueLabel));
                    break;
                case MS_RUN_HASH:
                    msRun = context.addMsRunHash(metadata, id, valueLabel);
                    break;
                case MS_RUN_HASH_METHOD:
                    msRun = context.addMsRunHashMethod(metadata, id,
                        checkParameter(defineLabel, valueLabel));
                    break;
            }
        }
        if (msRun == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.msRun+"[" + id + "]"));
        }
    }

    private void addDatabase(Metadata metadata, MetadataProperty property,
        Integer id,
        String defineLabel, String valueLabel) throws MZTabException {
        Database database = null;
        if (property == null) {
            database = context.addDatabase(metadata, new Database().id(id).
                param(checkParameter(defineLabel, valueLabel)));
        } else {
            switch (property) {
                case DATABASE_PREFIX:
                    database = context.addDatabasePrefix(metadata, id,
                        valueLabel);
                    break;
                case DATABASE_VERSION:
                    database = context.addDatabaseVersion(metadata, id,
                        valueLabel);
                    break;
                case DATABASE_URL:
                    database = context.addDatabaseUrl(metadata, id, checkURL(
                        defineLabel,
                        valueLabel));
                    break;
            }
        }
        if (database == null) {
            throw new MZTabException(new MZTabError(LogicalErrorType.NULL,
                lineNumber, Metadata.Properties.database+"[" + id + "]"));
        }
    }

    private void addSample(Metadata metadata, MetadataProperty property,
        Integer id,
        String defineLabel, String valueLabel) {
        if (property == null) {
            context.addSample(metadata, new Sample().id(id).
                name(valueLabel));
        } else {
            switch (property) {
                case SAMPLE_SPECIES:
                    context.addSampleSpecies(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
                case SAMPLE_TISSUE:
                    context.addSampleTissue(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
                case SAMPLE_CELL_TYPE:
                    context.addSampleCellType(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
                case SAMPLE_DISEASE:
                    context.addSampleDisease(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
                case SAMPLE_DESCRIPTION:
                    context.addSampleDescription(metadata, id, valueLabel);
                    break;
                case SAMPLE_CUSTOM:
                    context.addSampleCustom(metadata, id, checkParameter(
                        defineLabel, valueLabel));
                    break;
            }
        }
    }

    private void addAssay(Metadata metadata, MetadataProperty property,
        String defineLabel,
        String valueLabel, Integer id) throws MZTabException {
        IndexedElement indexedElement;
        if (property == null) {
            context.addAssay(metadata, new Assay().id(id).
                name(valueLabel));
        } else {
            switch (property) {
                case ASSAY_SAMPLE_REF:
                    indexedElement = checkIndexedElement(defineLabel, valueLabel,
                        MetadataElement.SAMPLE);
                    if (indexedElement != null) {
                        Sample sample = context.getSampleMap().
                            get(indexedElement.getId());
                        if (sample == null) {
                            throw new MZTabException(new MZTabError(
                                LogicalErrorType.NotDefineInMetadata, lineNumber,
                                valueLabel,
                                valueLabel));
                        }
                        context.addAssaySample(metadata, id, sample);
                    }
                    break;
                case ASSAY_MS_RUN_REF:
                    indexedElement = checkIndexedElement(defineLabel, valueLabel,
                        MetadataElement.MS_RUN);
                    if (indexedElement != null) {
                        MsRun msRun = context.getMsRunMap().
                            get(indexedElement.getId());
                        if (msRun == null) {
                            throw new MZTabException(new MZTabError(
                                LogicalErrorType.NotDefineInMetadata, lineNumber,
                                valueLabel));
                        }
                        context.addAssayMsRun(metadata, id, msRun);
                    }
                    break;
            }
        }
    }

    private void addStudyVariable(Metadata metadata, MetadataProperty property,
        String defineLabel,
        String valueLabel, Integer id) throws MZTabErrorOverflowException, MZTabException {
        List<IndexedElement> indexedElementList;
        if (property == null) {
            context.addStudyVariable(metadata, new StudyVariable().id(id).
                name(valueLabel));
        } else {
            switch (property) {
                case STUDY_VARIABLE_ASSAY_REFS:
                    indexedElementList = checkIndexedElementList(defineLabel,
                        valueLabel, MetadataElement.ASSAY);
                    // detect duplicates
                    indexedElementList.stream().
                        filter(i ->
                            Collections.frequency(indexedElementList, i) > 1).
                        collect(Collectors.toSet()).
                        forEach((indexedElement) ->
                        {
                            errorList.add(new MZTabError(
                                LogicalErrorType.DuplicationID, lineNumber,
                                valueLabel));
                        });
                    // check that assays exist
                    for (IndexedElement e : indexedElementList) {
                        //assays need to be defined before
                        if (!context.getAssayMap().
                            containsKey(e.getId())) {
                            // can not find assay[id] in metadata.
                            throw new MZTabException(new MZTabError(
                                LogicalErrorType.NotDefineInMetadata, lineNumber,
                                valueLabel));
                        }
                        context.addStudyVariableAssay(metadata, id, context.
                            getAssayMap().
                            get(e.getId()));
                    }
                    break;
                case STUDY_VARIABLE_SAMPLE_REFS:
                    indexedElementList = checkIndexedElementList(defineLabel,
                        valueLabel, MetadataElement.SAMPLE);
                    // detect duplicates
                    indexedElementList.stream().
                        filter(i ->
                            Collections.frequency(indexedElementList, i) > 1).
                        collect(Collectors.toSet()).
                        forEach((indexedElement) ->
                        {
                            errorList.add(new MZTabError(
                                LogicalErrorType.DuplicationID, lineNumber,
                                valueLabel));
                        });
                    // check that sample exist
                    for (IndexedElement e : indexedElementList) {
                        if (!context.getSampleMap().
                            containsKey(e.getId())) {
                            // can not find assay[id] in metadata.
                            throw new MZTabException(new MZTabError(
                                LogicalErrorType.NotDefineInMetadata, lineNumber,
                                valueLabel,
                                valueLabel));
                        }
                        context.addStudyVariableSample(metadata, id, context.
                            getSampleMap().
                            get(e.getId()));
                    }
                    break;
                case STUDY_VARIABLE_DESCRIPTION:
                    context.
                        addStudyVariableDescription(metadata, id, valueLabel);
                    break;
                case STUDY_VARIABLE_AVERAGE_FUNCTION:
                    context.addStudyVariableAverageFunction(metadata, id,
                        checkParameter(defineLabel, valueLabel));
                    break;
                case STUDY_VARIABLE_VARIATION_FUNCTION:
                    context.addStudyVariableVariationFunction(metadata, id,
                        checkParameter(defineLabel, valueLabel));
                    break;
            }
        }
    }

    private void addCv(Metadata metadata, MetadataProperty property, Integer id,
        String valueLabel) {
        if(property==null) {
            context.addCV(metadata, new CV().id(id));
        } else {
            switch (property) {
                case CV_LABEL:
                    context.addCVLabel(metadata, id, valueLabel);
                    break;
                case CV_FULL_NAME:
                    context.addCVFullName(metadata, id, valueLabel);
                    break;
                case CV_VERSION:
                    context.addCVVersion(metadata, id, valueLabel);
                    break;
                case CV_URL:
                    context.addCVURL(metadata, id, valueLabel);
                    break;
            }
        }
    }
}

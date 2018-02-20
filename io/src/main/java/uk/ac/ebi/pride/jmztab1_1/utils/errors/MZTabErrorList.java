package uk.ac.ebi.pride.jmztab1_1.utils.errors;

import uk.ac.ebi.pride.jmztab1_1.model.MZTabConstants;
import uk.ac.ebi.pride.jmztab1_1.utils.MZTabProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.ac.ebi.pride.jmztab1_1.utils.MZTabProperties.MAX_ERROR_COUNT;

/**
 * A limit max capacity list, if contains a couple of {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError} objects.
 * If overflow, system will raise {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException}. Besides this, during
 * add a new {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError} object, it's {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType#level} SHOULD equal or
 * great than its level setting.
 *
 * @author qingwei
 * @since 29/01/13
 * 
 */
public class MZTabErrorList {
    private int maxErrorCount;
    private List<MZTabError> errorList;
    private MZTabErrorType.Level level;

    /**
     * Generate a error list, which max size is {@link uk.ac.ebi.pride.jmztab1_1.utils.MZTabProperties#MAX_ERROR_COUNT},
     * and only allow {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType.Level#Error} or greater level errors to be added
     * into list.
     */
    public MZTabErrorList() {
        this(MZTabErrorType.Level.Error);
    }

    /**
     * Generate a error list, which max size is {@link uk.ac.ebi.pride.jmztab1_1.utils.MZTabProperties#MAX_ERROR_COUNT}
     *
     * @param level if null, default level is {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType.Level#Error}
     */
    public MZTabErrorList(MZTabErrorType.Level level) {
        this(level, MAX_ERROR_COUNT);
    }

    /**
     * Generate a error list, with given error level and maximum error count.
     *
     * @param level if null, default level is {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType.Level#Error}
     * @param maxErrorCount the maximum number of errors recorded by this list before an
     *        {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException} is thrown
     */
    public MZTabErrorList(MZTabErrorType.Level level, int maxErrorCount) {
        this.level = level == null ? MZTabErrorType.Level.Error : level;
        this.maxErrorCount = maxErrorCount>=0?maxErrorCount:0;
        this.errorList = new ArrayList<MZTabError>(this.maxErrorCount);
    }

    /**
     * Unmodifiable list of errors
     *
     * @return error list
     */
    public List<MZTabError> getErrorList() {
        return Collections.unmodifiableList(errorList);
    }

    /**
     * A limit max capacity list, if contains a couple of {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError} objects.
     * If overflow, system will raise {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException}. Besides this, during
     * add a new {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError} object, it's {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType#level} SHOULD equal or
     * greater than its level setting.
     *
     * @param error SHOULD NOT set null
     * @return a boolean.
     * @throws uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException if any.
     */
    public boolean add(MZTabError error) throws MZTabErrorOverflowException {
        if (error == null) {
            throw new NullPointerException("Can not add a null error into list.");
        }

        if (error.getType().getLevel().ordinal() < level.ordinal()) {
            return false;
        }

        if (errorList.size() >= maxErrorCount) {
            throw new MZTabErrorOverflowException();
        }

        return errorList.add(error);
    }

    /**
     * <p>Getter for the field <code>maxErrorCount</code>.</p>
     *
     * @return The maximum number of errors that are going to be reported before the parser stops with an {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException}
     */
    public int getMaxErrorCount() {
        return maxErrorCount;
    }

    /**
     * Define the maximum number of errors recorded by this list before an {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorOverflowException} is thrown
     *
     * @param maxErrorCount needs to be a positive number or zero will be set
     */
    public void setMaxErrorCount(int maxErrorCount) {
        this.maxErrorCount = maxErrorCount>=0?maxErrorCount:0;
    }

    /**
     * <p>Getter for the field <code>level</code>.</p>
     *
     * @return level of errors reported {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType#level}
     */
    public MZTabErrorType.Level getLevel() {
        return level;
    }

    /**
     * Define the level of the errors that are going to be store in the list. The incoming errors with an equal or highest level will be stored.
     *
     * @param level {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabErrorType#level}
     */
    public void setLevel(MZTabErrorType.Level level) {
        this.level = level;
    }

    /**
     * Clear all errors stored in the error list.
     */
    public void clear() {
        errorList.clear();
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return a int.
     */
    public int size() {
        return errorList.size();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return a {@link uk.ac.ebi.pride.jmztab1_1.utils.errors.MZTabError} object.
     */
    public MZTabError getError(int index) {
        return errorList.get(index);
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return a boolean.
     */
    public boolean isEmpty() {
        return errorList.isEmpty();
    }

    /**
     * Print error list to output stream.
     *
     * @param out SHOULD NOT set null.
     * @throws java.io.IOException if any.
     */
    public void print(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("Output stream should be set first.");
        }

        for (MZTabError e : errorList) {
            out.write(e.toString().getBytes());
        }
    }

    /**
     * Print error list to string.
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (MZTabError error : errorList) {
            sb.append(error).append(MZTabConstants.NEW_LINE);
        }

        return sb.toString();
    }
}

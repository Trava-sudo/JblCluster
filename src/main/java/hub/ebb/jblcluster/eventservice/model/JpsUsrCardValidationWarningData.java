package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.cardValidation.JpsRetCode;

import java.io.Serializable;

/**
 * Card validation warning data.
 * Virtual gateless entry/exits stations do not prevent
 * entry/exit even if the value in JpsUsrCardValidationResult.getRetCode() is for error.
 * In this scenario we might want to store the validation errors in JpsUsrCardValidationWarningData
 * and continue running the validations, even if some validation fails.
 */
public class JpsUsrCardValidationWarningData implements Serializable {
    private JpsRetCode warningCode;
    private String message;

    public JpsRetCode getWarningCode() {
        return warningCode;
    }

    public void setWarningCode(JpsRetCode warningCode) {
        this.warningCode = warningCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

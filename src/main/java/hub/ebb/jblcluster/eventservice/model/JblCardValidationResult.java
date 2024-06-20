package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.cardValidation.JpsRetCode;
import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.dto.jps.event.JpsLogUsrPass;

/**
 * Created by Stefano.Coletta on 27/12/2016.
 */
public class JblCardValidationResult extends JblEventExtendedJbl {

    private JpsRetCode retCode;
    private double amount = 0;
    private int freeTimeLeft = 0;
    private long timeToExit = 0;
    private String message;

    public JpsRetCode getRetCode() {
        return retCode;
    }

    public void setRetCode(JpsRetCode retCode) {
        this.retCode = retCode;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getFreeTimeLeft() {
        return freeTimeLeft;
    }

    public void setFreeTimeLeft(int freeTimeLeft) {
        this.freeTimeLeft = freeTimeLeft;
    }

    public long getTimeToExit() {
        return timeToExit;
    }

    public void setTimeToExit(long timeToExit) {
        this.timeToExit = timeToExit;
    }

    @Override
    public void setJpsEvent(JpsEvent jpsEvent) {
        if (jpsEvent instanceof JpsLogUsrPass)
            super.setJpsEvent(jpsEvent);

    }

    @Override
    public JpsLogUsrPass getJpsEvent() {
        return (JpsLogUsrPass) super.getJpsEvent();
    }

}

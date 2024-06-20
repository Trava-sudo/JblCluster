package hub.ebb.jblcluster.eventservice.model;


import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.dto.jps.event.JpsLogUsrPass;
import hub.jbl.core.dto.parser.JpsRawData;
import hub.jbl.core.dto.parser.JpsRawDataParser;

public class JblUsrPass extends JblEventExtendedJbl {

    private String lostTicketSourceCode;
    private String authToken;

    @Override
    public JpsLogUsrPass getJpsEvent() {
        return (JpsLogUsrPass) super.getJpsEvent();
    }

    @Override
    public void setJpsEvent(JpsEvent jpsEvent) {
        if (jpsEvent instanceof JpsLogUsrPass)
            super.setJpsEvent(jpsEvent);
    }

    public JpsRawData getBarcodeData(String siteCode) {
        return JpsRawDataParser.JpsDataParserFactory.get().parser(this.getJpsEvent().getRawData(), this.getJpsEvent().getNowGmt(), siteCode).parse();
    }

    public String getLostTicketSourceCode() {
        return lostTicketSourceCode;
    }

    public void setLostTicketSourceCode(String lostTicketSourceCode) {
        this.lostTicketSourceCode = lostTicketSourceCode;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}

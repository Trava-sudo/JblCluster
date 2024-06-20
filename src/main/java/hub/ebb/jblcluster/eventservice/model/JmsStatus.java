package hub.ebb.jblcluster.eventservice.model;

import org.springframework.util.StringUtils;

/**
 * Created by Stefano.Coletta on 11/11/2016.
 */
public enum JmsStatus {

    NOT_SENT, SENT, MAPPING_ERROR, JMS_FAIL;

    public static JmsStatus getEnum(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return valueOf(value);
        }
    }
}

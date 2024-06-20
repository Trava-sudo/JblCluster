package hub.ebb.jblcluster.eventservice.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * @author Lorenzo.Santini
 */
public enum SellableProductType {

    APS("0x30000000"),
    APC("0x38000000"),
    FCJ("0x58000000"),
    FCJ_OL("0xa0000000"),
    LE_POS("0x80000000"),
    LE("0x08000000");

    private final String type;

    SellableProductType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static SellableProductType fromType(String type) {
        return StringUtils.isNotBlank(type) ? Arrays.stream(values()).filter(v -> v.getType().equals(type)).findFirst().orElse(null) : null;
    }
}

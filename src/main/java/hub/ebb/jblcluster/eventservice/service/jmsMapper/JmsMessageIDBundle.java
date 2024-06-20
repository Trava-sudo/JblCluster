package hub.ebb.jblcluster.eventservice.service.jmsMapper;

import hub.jbl.core.dto.jps.event.SpecCodeEnum;

/**
 * Created by Stefano.Coletta on 18/11/2016.
 */

/**
 * <bold>IMPORTANT!!</bold>
 * <p>
 * IF YOU NEED TO CREATE A NEW MESSAGE ID FOR AN ALARM OR A MESSAGE YOU MUST USE THE CLASS <code>SpecCodeEnum</code>.
 * THIS IS AN EXAMPLE
 *</p>
 * <p>
 * public static final long JBL_ALARM_COUNTER_EXCEEDED = SpecCodeEnum.JblAlarmCounterExceeded.getMessageId();
 *</p>
 * <p>
 *     IN THAT CLASS YOU CAN FIND, AS COMMENT, THE SQL SCRIPT TO ADD TO JMS
 * </p>
 *
 */
public class JmsMessageIDBundle {

    //YOU MUST GENERATE YOUR OWN MESSAGE ID STARTING FROM 4294967296 TO 9223372036854775807

    private static final long BASE_MESSAGE_ID = 4294967296L;
    //You can configure bundle in jms executing select janus.lpinsertorupdatemsmessage('jbl', 1342873610, 'false', 0, 'The access loop is free');
    //region LOG


    // region LOOP
    public static final long ACCESS_LOOP_FREE = BASE_MESSAGE_ID + 1;
    public static final long ARMING_LOOP_FREE = BASE_MESSAGE_ID + 2;
    public static final long AUXARMING_LOOP_FREE = BASE_MESSAGE_ID + 3;
    public static final long CLOSING_LOOP_FREE = BASE_MESSAGE_ID + 4;

    public static final long ACCESS_LOOP_BUSY = BASE_MESSAGE_ID + 5;
    public static final long ARMING_LOOP_BUSY = BASE_MESSAGE_ID + 6;
    public static final long AUXARMING_LOOP_BUSY = BASE_MESSAGE_ID + 7;
    public static final long CLOSING_LOOP_BUSY = BASE_MESSAGE_ID + 8;

    //endregion

    //region BAR
    public static final long ACCESS_BAR_OPENED = BASE_MESSAGE_ID + 9;
    public static final long CLOSING_BAR_OPENED = BASE_MESSAGE_ID + 10;

    public static final long ACCESS_BAR_CLOSED = BASE_MESSAGE_ID + 11;
    public static final long CLOSING_BAR_CLOSED = BASE_MESSAGE_ID + 12;

    public static final long ACCESS_BAR_CLOSING = BASE_MESSAGE_ID + 13;
    public static final long CLOSING_BAR_CLOSING = BASE_MESSAGE_ID + 14;

    public static final long ACCESS_BAR_HALTED = BASE_MESSAGE_ID + 15;
    public static final long CLOSING_BAR_HALTED = BASE_MESSAGE_ID + 16;

    public static final long ACCESS_BAR_OPENING = BASE_MESSAGE_ID + 17;
    public static final long CLOSING_BAR_OPENING = BASE_MESSAGE_ID + 18;
    //endregion

    //region BUTTON
    public static final long ACCESS_BUTTON_RELEASED = BASE_MESSAGE_ID + 19;
    public static final long ACCESS_BUTTON_PUSHED = BASE_MESSAGE_ID + 20;

    public static final long ACCESS_VCS_CLASS_A = BASE_MESSAGE_ID + 21;
    public static final long ACCESS_VCS_CLASS_B = BASE_MESSAGE_ID + 22;
    public static final long ACCESS_VCS_CLASS_C = BASE_MESSAGE_ID + 23;
    public static final long ACCESS_VCS_CLASS_D = BASE_MESSAGE_ID + 24;
    public static final long ACCESS_VCS_UNKWNOWN = BASE_MESSAGE_ID + 25;
    //endregion

    //region CARD
    public static final long CARD_AMOUNT_PAYED = BASE_MESSAGE_ID + 26;
    public static final long CARD_JAMMED = BASE_MESSAGE_ID + 27;
    public static final long CARD_ISSUED = BASE_MESSAGE_ID + 28;
    public static final long CARD_NOT_ISSUED = BASE_MESSAGE_ID + 29;
    public static final long CARD_NOT_READABLE = BASE_MESSAGE_ID + 30;
    public static final long CARD_NOT_RETIRED = BASE_MESSAGE_ID + 31;
    public static final long CARD_NOT_VALID = BASE_MESSAGE_ID + 32;
    public static final long CARD_READ = BASE_MESSAGE_ID + 33;
    public static final long CARD_RETIRED = BASE_MESSAGE_ID + 34;
    public static final long CARD_WITHDROWN = BASE_MESSAGE_ID + 35;
    public static final long CARD_LOST = BASE_MESSAGE_ID + 36;
    //endregion

    //region PERIPHERAL

    public static final long SOFTWARE_PERIPHERAL_REBOOT = BASE_MESSAGE_ID + 37;
    public static final long HARDWARE_PERIPHERAL_REBOOT = BASE_MESSAGE_ID + 38;
    //endregion

    //region JBL
    public static final long LOG_VIRTUAL_PERIPHERAL_AUTHENTICATING = BASE_MESSAGE_ID + 39;
    //endregion

    //region from Operation
    public static final long ENTRANCE_BREACH = BASE_MESSAGE_ID + 40;
    public static final long ENTRANCE_CARD_VALIDATION = BASE_MESSAGE_ID + 41;
    public static final long SUCCESSFUL_ENTRY = BASE_MESSAGE_ID + 42;
    public static final long ENTRANCE_MOVING_BACK = BASE_MESSAGE_ID + 43;
    public static final long UNSUCCESSFUL_ENTRY = BASE_MESSAGE_ID + 44;
    public static final long EXIT_BREACH = BASE_MESSAGE_ID + 45;
    public static final long EXIT_CARD_VALIDATION = BASE_MESSAGE_ID + 46;
    public static final long SUCCESSFUL_EXIT = BASE_MESSAGE_ID + 47;
    public static final long EXIT_MOVING_BACK = BASE_MESSAGE_ID + 48;
    public static final long UNSUCCESSFUL_EXIT = BASE_MESSAGE_ID + 49;
    public static final long SINGLESTAY_PAYMENT_COMPLETED = BASE_MESSAGE_ID + 50;
    public static final long SINGLESTAY_PAYMENT_DIFFERENCE_COMPLETED = BASE_MESSAGE_ID + 51;
    public static final long UNSUCCESSFUL_PAYMENT = BASE_MESSAGE_ID + 52;
    public static final long APS_CARD_VALIDATION = BASE_MESSAGE_ID + 53;
    public static final long APS_CHANGE_GIVEN = BASE_MESSAGE_ID + 54;
    public static final long APS_COIN_INSERTED = BASE_MESSAGE_ID + 55;
    public static final long APS_BANKNOTE_INSERTED = BASE_MESSAGE_ID + 56;
    public static final long APS_OPEN_DOOR = BASE_MESSAGE_ID + 57;
    public static final long APS_CLOSE_DOOR = BASE_MESSAGE_ID + 58;
    public static final long APS_MONEY_SNAPSHOOT_INSERTED_MONEY = BASE_MESSAGE_ID + 59;
    public static final long APS_MONEY_SNAPSHOOT_WITHDRAWAL_MONEY = BASE_MESSAGE_ID + 60;
    public static final long CASHIER_LOGIN_SUCCESS = BASE_MESSAGE_ID + 61;
    public static final long CASHIER_LOGIN_FAILED = BASE_MESSAGE_ID + 62;


    //endregion
    //endregion

    //region ALARM
    //region BAR
    public static final long ALARM_LE_GATE_BREACH = BASE_MESSAGE_ID + 76;
    public static final long ALARM_LX_GATE_BREACH = BASE_MESSAGE_ID + 77;
    //endregion

    //region JBL Alarm
    public static final long ALARM_JBL_VIRTUAL_PERIPHERAL_TYPE_MISSMATCH = BASE_MESSAGE_ID + 87;
    public static final long ALARM_JBL_VIRTUAL_PERIPHERAL_NONE_PERIPHERAL_NODE = BASE_MESSAGE_ID + 88;
    public static final long ALARM_JBL_PERIPHERAL_COMMAND_ERROR = BASE_MESSAGE_ID + 89;
    public static final long ALARM_JBL_TARIFF_MODULE_EXCEPTION = BASE_MESSAGE_ID + 90;
    public static final long ALARM_JBL_UDPATE_TARIFF_COMMAND = BASE_MESSAGE_ID + 91;
    public static final long ALARM_JBL_NEW_AUTHENTICATED_PERIPHERAL = BASE_MESSAGE_ID + 92;
    public static final long ALARM_JBL_CASHIER_LOGIN_FAILED = BASE_MESSAGE_ID + 93;
    //endregion


    //region LOG CASHIER ACTIVITY
    public static final long CASHIER_ACTIVITY_COIN_SAFE_EMPTY_LOG = BASE_MESSAGE_ID + 94;
    public static final long CASHIER_ACTIVITY_BILL_SAFE_EMPTY_LOG = BASE_MESSAGE_ID + 95;
    public static final long CASHIER_ACTIVITY_COIN_DISPENSE_LOG = BASE_MESSAGE_ID + 96;
    public static final long CASHIER_ACTIVITY_BILL_DISPENSE_LOG = BASE_MESSAGE_ID + 97;
    public static final long CASHIER_ACTIVITY_COIN_RELOAD_LOG = BASE_MESSAGE_ID + 98;
    public static final long CASHIER_ACTIVITY_BILL_RELOAD_LOG = BASE_MESSAGE_ID + 99;
    public static final long CASHIER_ACTIVITY_COIN_REPLACE_LOG = BASE_MESSAGE_ID + 100;
    public static final long CASHIER_ACTIVITY_BILL_REPLACE_LOG = BASE_MESSAGE_ID + 101;
    public static final long CASHIER_ACTIVITY_COIN_EMPTY_LOG = BASE_MESSAGE_ID + 102;
    public static final long CASHIER_ACTIVITY_BILL_EMPTY_LOG = BASE_MESSAGE_ID + 103;
    public static final long CASHIER_ACTIVITY_BILL_SAFE_DISPENSE_LOG = BASE_MESSAGE_ID + 104;
    public static final long CASHIER_ACTIVITY_COIN_SAFE_DISPENSE_LOG = BASE_MESSAGE_ID + 105;
    public static final long CASHIER_ACTIVITY_BILL_SAFE_RELOAD_LOG = BASE_MESSAGE_ID + 106;
    public static final long CASHIER_ACTIVITY_COIN_SAFE_RELOAD_LOG = BASE_MESSAGE_ID + 107;
    public static final long CASHIER_ACTIVITY_BILL_SAFE_REPLACE_LOG = BASE_MESSAGE_ID + 108;
    public static final long CASHIER_ACTIVITY_COIN_SAFE_REPLACE_LOG = BASE_MESSAGE_ID + 109;
    public static final long APS_RENEW_COMPLETED = BASE_MESSAGE_ID + 110;
    //endregion
    //endregion

    public static final long SUCCESSFUL_ENTRY_VALUE_CARD = BASE_MESSAGE_ID + 111;
    public static final long CARD_VALIDATION_VALUE_CARD = BASE_MESSAGE_ID + 112;
    public static final long ENTRANCE_MOVING_BACK_VALUE_CARD = BASE_MESSAGE_ID + 113;
    public static final long UNSUCCESSFUL_PAYMENT_VALUE_CARD = BASE_MESSAGE_ID + 114;
    public static final long RECHARGED_COMPLETED_VALUE_CARD = BASE_MESSAGE_ID + 115;
    public static final long EXIT_MOVING_BACK_VALUE_CARD = BASE_MESSAGE_ID + 116;
    public static final long SUCCESSFUL_EXIT_VALUE_CARD = BASE_MESSAGE_ID + 117;
    public static final long EXIT_CARD_VALIDATION_VALUE_CARD = BASE_MESSAGE_ID + 118;

    public static final long SUCCESSFUL_ENTRY_PER_USAGE = BASE_MESSAGE_ID + 119;
    public static final long CARD_VALIDATION_PER_USAGE = BASE_MESSAGE_ID + 120;
    public static final long ENTRANCE_MOVING_BACK_PER_USAGE = BASE_MESSAGE_ID + 121;


    public static final long ENTRANCE_BREACH_VALUE_CARD = BASE_MESSAGE_ID + 122;
    public static final long ENTRANCE_BREACH_PER_USAGE = BASE_MESSAGE_ID + 123;
    public static final long ENTRANCE_BREACH_PER_PERIOD = BASE_MESSAGE_ID + 124;

    public static final long EXIT_BREACH_VALUE_CARD = BASE_MESSAGE_ID + 125;
    public static final long EXIT_BREACH_PER_USAGE = BASE_MESSAGE_ID + 126;
    public static final long EXIT_BREACH_PER_PERIOD = BASE_MESSAGE_ID + 127;

    public static final long SUCCESSFUL_EXIT_PER_USAGE = BASE_MESSAGE_ID + 128;
    public static final long SUCCESSFUL_EXIT_PER_PERIOD = BASE_MESSAGE_ID + 129;

    public static final long ENTRANCE_MOVING_BACK_PER_PERIOD = BASE_MESSAGE_ID + 130;
    public static final long SUCCESSFUL_ENTRY_PER_PERIOD = BASE_MESSAGE_ID + 131;

    public static final long EXIT_MOVING_BACK_PER_USAGE = BASE_MESSAGE_ID + 132;


    public static final long SINGLESTAY_PAYMENT_DIFFERENCE_COMPLETED_PER_USAGE = BASE_MESSAGE_ID + 133;
    public static final long SINGLESTAY_PAYMENT_COMPLETED_PER_USAGE = BASE_MESSAGE_ID + 134;
    public static final long UNSUCCESSFUL_PAYMENT_PER_USAGE = BASE_MESSAGE_ID + 135;
    public static final long UNSUCCESSFUL_PAYMENT_PER_PERIOD = BASE_MESSAGE_ID + 136;
    public static final long CARD_VALIDATION_PER_PERIOD = BASE_MESSAGE_ID + 137;
    public static final long CARD_VALIDATION_WITH_OVERAGE = BASE_MESSAGE_ID + 138;
    public static final long LOG_LPR_READING = BASE_MESSAGE_ID + 139;
    public static final long LOG_LPR_READ = BASE_MESSAGE_ID + 140;
    public static final long ALARM_JBL_LPR_MISSMATCH = BASE_MESSAGE_ID + 141;
    public static final long ALARM_JBL_CARD_POOL_SOFT_LIMIT_EXCEEDED = BASE_MESSAGE_ID + 142;
    public static final long LOG_VOUCHER_READ = BASE_MESSAGE_ID + 143;

    public static final long TIME_PERIPHERAL_MISMATCH = BASE_MESSAGE_ID + 144;
    public static final long ALARM_CONNECTION_POOL_LEAK = BASE_MESSAGE_ID + 145;

    public static final long APS_EXCESSES_PAYMENT_COMPLETED = BASE_MESSAGE_ID + 146;

    public static final long CARD_VALIDATION_OPERATOR_CARD = BASE_MESSAGE_ID + 147;
    public static final long EXIT_MOVING_BACK_OPERATOR_CARD = BASE_MESSAGE_ID + 148;
    public static final long SUCCESSFUL_EXIT_OPERATOR_CARD = BASE_MESSAGE_ID + 149;
    public static final long EXIT_BREACH_OPERATOR_CARD = BASE_MESSAGE_ID + 150;
    public static final long ENTRANCE_MOVING_BACK_OPERATOR_CARD = BASE_MESSAGE_ID + 151;
    public static final long SUCCESSFUL_ENTRY_OPERATOR_CARD = BASE_MESSAGE_ID + 152;
    public static final long ENTRANCE_BREACH_OPERATOR_CARD = BASE_MESSAGE_ID + 153;

    public static final long LOG_VEHICLE_COUNTER_ACTIVATED = BASE_MESSAGE_ID + 154;
    public static final long LOG_VEHICLE_COUNTER_DEACTIVATED = BASE_MESSAGE_ID + 155;

    public static final long LOG_VEHICLE_COUNTER_AUTO_RESET = BASE_MESSAGE_ID + 156;
    public static final long PREPAID_TICKET_CONSUMPTION = BASE_MESSAGE_ID + 157;

    public static final long ALARM_JBL_FIXED_PATH_TIME_VIOLATION = SpecCodeEnum.JblAlarmFixedPathTimeViolation.getMessageId();
    public static final long ALARM_JBL_FIXED_PATH_NOT_RESPECTED = BASE_MESSAGE_ID + 159;
    public static final long BAD_FACILITY_CODE = BASE_MESSAGE_ID + 160;

    public static final long LOG_FISCPRINTER_TIME_SYNCHRONIZED = BASE_MESSAGE_ID + 161;

    public static final long ALARM_JBL_LPR_LICENSE_PLATE_NOT_READ = BASE_MESSAGE_ID + 162;
    public static final long ALARM_JBL_LPR_PARKING_FEE_NOT_PAID = BASE_MESSAGE_ID + 163;
    public static final long ALARM_JBL_LPR_OVERDRAFT_LIMIT_EXCEEDED = BASE_MESSAGE_ID + 164;
    public static final long ALARM_JBL_LPR_OVERSTAY_LIMIT_EXCEEDED = BASE_MESSAGE_ID + 165;
    public static final long ALARM_JBL_LPR_EXPIRED = BASE_MESSAGE_ID + 166;
    public static final long ALARM_JBL_LPR_NOT_RESPECTED_SEQUENCE_ANTIPB = BASE_MESSAGE_ID + 167;
    public static final long ALARM_JBL_LPR_NOT_ENABLED_USAGE = BASE_MESSAGE_ID + 168;
    public static final long ALARM_JBL_LPR_LICENSE_PLATE_NOT_FOUND = BASE_MESSAGE_ID + 169;
    public static final long ALARM_JBL_GATELESS_PERIPHERAL_OFFLINE = BASE_MESSAGE_ID + 170;

    public static final long LOG_USER_CATEGORY_CHANGED = BASE_MESSAGE_ID + 171;
    public static final long ALARM_JBL_EBB_CONF_IGNORED = BASE_MESSAGE_ID + 172;
    public static final long LOG_JBL_EBB_CONF_APPLIED = BASE_MESSAGE_ID + 173;

    public static final Long ALARM_JBL_TYPE_SHIFT = BASE_MESSAGE_ID + 174;

    public static final long ALARM_JBL_PERIPHERAL_OFFLINE = BASE_MESSAGE_ID + 175;

    public static final long SUCCESSFUL_ENTRY_POINTS_CARD = BASE_MESSAGE_ID + 176;
    public static final long CARD_VALIDATION_POINTS_CARD = BASE_MESSAGE_ID + 177;
    public static final long ENTRANCE_MOVING_BACK_POINTS_CARD = BASE_MESSAGE_ID + 178;
    public static final long UNSUCCESSFUL_PAYMENT_POINTS_CARD = BASE_MESSAGE_ID + 179;
    public static final long RECHARGED_COMPLETED_POINTS_CARD = BASE_MESSAGE_ID + 180;
    public static final long EXIT_MOVING_BACK_POINTS_CARD = BASE_MESSAGE_ID + 181;
    public static final long SUCCESSFUL_EXIT_POINTS_CARD = BASE_MESSAGE_ID + 182;
    public static final long EXIT_CARD_VALIDATION_POINTS_CARD = BASE_MESSAGE_ID + 183;

    public static final long ENTRANCE_BREACH_POINTS_CARD = BASE_MESSAGE_ID + 184;
    public static final long EXIT_BREACH_POINTS_CARD = BASE_MESSAGE_ID + 185;
    public static final long BALANCE_USAGE_POINTS_CARD = BASE_MESSAGE_ID + 186;

    public static final long JMS_MESSAGE_LOW_BALANCE = BASE_MESSAGE_ID + 187;

    public static final long JBL_ALARM_COUNTER_EXCEEDED = SpecCodeEnum.JblAlarmCounterExceeded.getMessageId();

    public static final long NEGOTIATED_AMOUNT_PAYMENT = SpecCodeEnum.JmsNegotiatedAmountPayment.getMessageId();

    public static final long LOG_LPR_READ_FILTERED = SpecCodeEnum.JblLogLpr.getMessageId();

    public static final long LOG_THIRD_PARTY_BARCODE_VOUCHER_READ = SpecCodeEnum.JblLogThirdPartyBarcodeVoucherRead.getMessageId();
    public static final long ALARM_JBL_MEMBERSHIP_TRANSIT_NO_PLATE = SpecCodeEnum.JblAlarmMembershipTransitNoPlate.getMessageId();

    /**
     * <bold>IMPORTANT!!</bold>
     * <p>
     * IF YOU NEED TO CREATE A NEW MESSAGE ID FOR AN ALARM OR A MESSAGE YOU MUST USE THE CLASS <code>SpecCodeEnum</code>.
     * THIS IS AN EXAMPLE
     *</p>
     * <p>
     * public static final long JBL_ALARM_COUNTER_EXCEEDED = SpecCodeEnum.JblAlarmCounterExceeded.getMessageId();
     *</p>
     * <p>
     *     IN THAT CLASS YOU CAN FIND, AS COMMENT, THE SQL SCRIPT TO ADD TO JMS
     * </p>
     *
     */

}

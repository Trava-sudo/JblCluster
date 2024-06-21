package hub.ebb.jblcluster.eventservice.mapper;

import hub.ebb.jblcluster.eventservice.model.*;
import hub.jbl.core.dto.jps.event.FcjDevAlarmPrinterPaperOut;
import hub.jbl.core.dto.jps.event.FcjDevHandheldAlarmBatteryLow;
import hub.jbl.core.dto.jps.event.JpsAppAlrmEmergency;
import hub.jbl.core.dto.jps.event.JpsLogFloorCounter;
import hub.jbl.core.dto.onstreet.OnStreetGenericAlarm;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import io.vertx.core.AsyncResult;

import java.util.concurrent.CompletableFuture;

public interface JblEventMapperVisitor extends JpsEventMapperVisitor<JmsEvent> {

    CompletableFuture<AsyncResult<JmsEvent>> map(JpsUsrCardValidationResult event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblEventExtendedJbl jblEvent);

    CompletableFuture<AsyncResult<JmsEvent>> map(JmsExternalPaymentComplete event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JmsVoidPaymentOperation event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogAuthenticatedVirtualPeripheral event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogVehicleCounterAutoReset event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogUserCategoryChanged event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogLpr event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogVoucherRead event) throws Exception;

	CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmBadVirtualPeripheralType event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmNotPeripheralNode event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmUpdateTariffCommand event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmNewAuthenticatedDevice event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLxGateBreach event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmTariffModuleException event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmCommandError event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLeGateBreach event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmCashierOperatorLoginFailed event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRMissmatch event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRAntiPB event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRExpired event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRNotEnabledUsage event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPROverdraftLimitExceeded event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPROverstayLimitExceeded event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRPlateNotFound event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRParkingFeeNotPaid event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRPlateNotRead event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmCardPoolSoftLimitExceeded event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblTimePeripheralMismatch event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmConnectionPoolLeak event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmFixedPathTimeViolation event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmVehicleClassMismatch event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmFixedPathNotRespected event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmConfigUpdateIgnored event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogThirdPartyBarcodeVoucherRead event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JblLogConfigUpdateApplied event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmGatelessOffline event);

    CompletableFuture<AsyncResult<JmsEvent>> map(OnStreetGenericAlarm event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JpsLogFloorCounter event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JmsCancelSalePaymentOperation event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmTypeShift event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmPeripheralOffline event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmCounterExceeded event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JmsBillingOperation event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JmsPaymentOperation event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmExpiredSubscriptionEntryWithTicket event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmContractBlockedAtExit event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmContractBlockedAtEntry event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmContractBlockedTotally event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmAccountBlockedAtEntry event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmAccountBlockedAtExit event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmAccountBlockedTotally event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRWarninglist event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLPRBlocklist event) throws Exception;

    CompletableFuture<AsyncResult<JmsEvent>> map(FcjDevHandheldAlarmBatteryLow event);

    CompletableFuture<AsyncResult<JmsEvent>> map(FcjDevAlarmPrinterPaperOut event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmMembershipTransitNoPlate event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JpsAppAlrmEmergency event);

    CompletableFuture<AsyncResult<JmsEvent>> map(JblAlarmLUnipolNotAccessible event) throws Exception;
}


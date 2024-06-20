package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.generator.InvalidTypeException;

public class JblEventFactoryContainer {
	private static final Object INSTANCE = new JblEventFactoryContainer();

	private JblEventFactoryContainer() {
	}

	public Object jpsEventFactory(final String eventSpecCode, final String eventType) throws
			InvalidTypeException {
		switch (eventSpecCode) {
			case "JblLogAuthenticatedVirtualPeripheral": return new JblLogAuthenticatedVirtualPeripheral();
			case "JpsUsrCardValidationResult": return new JpsUsrCardValidationResult();
			case "JblAlarmBadVirtualPeripheralType": return new JblAlarmBadVirtualPeripheralType();
			case "JblAlarmNotPeripheralNode": return new JblAlarmNotPeripheralNode();
			case "JblAlarmLeGateBreach": return new JblAlarmLeGateBreach();
			case "JblAlarmLxGateBreach": return new JblAlarmLxGateBreach();
			case "JblCardValidationResult": return new JblCardValidationResult();
			case "JblAlarmCommandError": return new JblAlarmCommandError();
			case "JblAlarmNewAuthenticatedDevice": return new JblAlarmNewAuthenticatedDevice();
			case "JmsExternalPaymentComplete": return new JmsExternalPaymentComplete();
			case "JmsVoidPaymentOperation": return new JmsVoidPaymentOperation();
			case "JblAlarmTariffModuleException": return new JblAlarmTariffModuleException();
			case "JblAlarmUpdateTariffCommand": return new JblAlarmUpdateTariffCommand();
			case "JblAlarmCashierOperatorLoginFailed": return new JblAlarmCashierOperatorLoginFailed();
			case "JblLogLpr": return new JblLogLpr();
			case "JblAlarmLPRMissmatch": return new JblAlarmLPRMissmatch();
			case "JblAlarmLPRAntiPB": return new JblAlarmLPRAntiPB();
			case "JblAlarmLPRExpired": return new JblAlarmLPRExpired();
			case "JblAlarmLPRNotEnabledUsage": return new JblAlarmLPRNotEnabledUsage();
			case "JblAlarmLPROverdraftLimitExceeded": return new JblAlarmLPROverdraftLimitExceeded();
			case "JblAlarmLPROverstayLimitExceeded": return new JblAlarmLPROverstayLimitExceeded();
			case "JblAlarmLPRParkingFeeNotPaid": return new JblAlarmLPRParkingFeeNotPaid();
			case "JblAlarmLPRPlateNotRead": return new JblAlarmLPRPlateNotRead();
			case "JblAlarmLPRPlateNotFound": return new JblAlarmLPRPlateNotFound();
			case "JblAlarmCardPoolSoftLimitExceeded": return new JblAlarmCardPoolSoftLimitExceeded();
			case "JblLogVoucherRead": return new JblLogVoucherRead();
			case "JblTimePeripheralMismatch": return new JblTimePeripheralMismatch();
			case "JblAlarmConnectionPoolLeak": return new JblAlarmConnectionPoolLeak();
			case "JblLogVehicleCounterAutoReset": return new JblLogVehicleCounterAutoReset();
			case "JblAlarmFixedPathTimeViolation": return new JblAlarmFixedPathTimeViolation();
			case "JblAlarmFixedPathNotRespected": return new JblAlarmFixedPathNotRespected();
			case "JblAlarmConfigUpdateIgnored": return new JblAlarmConfigUpdateIgnored();
			case "JblLogConfigUpdateApplied": return new JblLogConfigUpdateApplied();
			case "JblAlarmVehicleClassMismatch": return new JblAlarmVehicleClassMismatch();
			case "JblLogUserCategoryChanged": return new JblLogUserCategoryChanged();
			case "JblAlarmGatelessOffline": return new JblAlarmGatelessOffline();
			case "JmsCancelSalePaymentOperation": return new JmsCancelSalePaymentOperation();
			case "JblAlarmTypeShift": return new JblAlarmTypeShift();
			case "JblAlarmPeripheralOffline": return new JblAlarmPeripheralOffline();
			case "JblAlarmCounterExceeded": return new JblAlarmCounterExceeded();
			case "JmsBillingOperation": return new JmsBillingOperation();
			case "JmsPaymentOperation": return new JmsPaymentOperation();
			case "JmsNegotiatedAmountPayment": return new JmsNegotiatedAmountPayment();
			case "JblAlarmExpiredSubscriptionEntryWithTicket": return new JblAlarmExpiredSubscriptionEntryWithTicket();
			case "JblAlarmContractBlockedAtEntry": return new JblAlarmContractBlockedAtEntry();
			case "JblAlarmContractBlockedAtExit": return new JblAlarmContractBlockedAtExit();
			case "JblAlarmContractBlockedTotally": return new JblAlarmContractBlockedTotally();
			case "JblAlarmAccountBlockedTotally": return new JblAlarmAccountBlockedTotally();
			case "JblAlarmAccountBlockedAtEntry": return new JblAlarmAccountBlockedAtEntry();
			case "JblAlarmAccountBlockedAtExit": return new JblAlarmAccountBlockedAtExit();
			case "JblAlarmLPRWarninglist": return new JblAlarmLPRWarninglist();
			case "JblAlarmLPRBlocklist": return new JblAlarmLPRBlocklist();
			case "JblLogThirdPartyBarcodeVoucherRead": return new JblLogThirdPartyBarcodeVoucherRead();
			case "JpsLogLaneButtonAccess2Pushed": return new JpsLogLaneButtonAccess2Pushed();
			case "JpsLogLaneButtonAccess2Released": return new JpsLogLaneButtonAccess2Released();
			case "JblGuestPassCreateEvent": return new JblGuestPassCreateEvent();
			case "JblGuestPassChangeEvent": return new JblGuestPassChangeEvent();
			case "JblGuestPassDeleteEvent": return new JblGuestPassDeleteEvent();
			case "JblGuestPassOvernightsChangedEvent": return new JblGuestPassOvernightsChangedEvent();
			case "JblAlarmMembershipTransitNoPlate": return new JblAlarmMembershipTransitNoPlate();
			case "JblAlarmLUnipolNotAccessible": return new JblAlarmLUnipolNotAccessible();
			default: throw new InvalidTypeException("Error", eventType);
		}
	}

	public <T extends JpsEvent> T initEvent(T event) throws InvalidTypeException {
		String className = event.getClass().getSimpleName();
		switch (className) {
			case "JblLogAuthenticatedVirtualPeripheral": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogAuthenticatedVirtualPeripheral); break;
			case "JpsUsrCardValidationResult": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JpsUsrCardValidationResult); break;
			case "JblAlarmBadVirtualPeripheralType": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmBadVirtualPeripheralType); break;
			case "JblAlarmNotPeripheralNode": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmNotPeripheralNode); break;
			case "JblAlarmLeGateBreach": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLeGateBreach); break;
			case "JblAlarmLxGateBreach": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLxGateBreach); break;
			case "JblCardValidationResult": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblCardValidationResult); break;
			case "JblAlarmCommandError": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmCommandError); break;
			case "JblAlarmNewAuthenticatedDevice": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmNewAuthenticatedDevice); break;
			case "JmsExternalPaymentComplete": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsExternalPaymentComplete); break;
			case "JmsVoidPaymentOperation": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsVoidPaymentOperation); break;
			case "JblAlarmTariffModuleException": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmTariffModuleException); break;
			case "JblAlarmUpdateTariffCommand": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmUpdateTariffCommand); break;
			case "JblAlarmCashierOperatorLoginFailed": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmCashierOperatorLoginFailed); break;
			case "JblLogLpr": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogLpr); break;
			case "JblAlarmLPRMissmatch": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRMissmatch); break;
			case "JblAlarmLPRAntiPB": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRAntiPB); break;
			case "JblAlarmLPRExpired": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRExpired); break;
			case "JblAlarmLPRNotEnabledUsage": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRNotEnabledUsage); break;
			case "JblAlarmLPROverdraftLimitExceeded": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPROverdraftLimitExceeded); break;
			case "JblAlarmLPROverstayLimitExceeded": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPROverstayLimitExceeded); break;
			case "JblAlarmLPRParkingFeeNotPaid": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRParkingFeeNotPaid); break;
			case "JblAlarmLPRPlateNotRead": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRPlateNotRead); break;
			case "JblAlarmLPRPlateNotFound": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRPlateNotFound); break;
			case "JblAlarmCardPoolSoftLimitExceeded": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmCardPoolSoftLimitExceeded); break;
			case "JblLogVoucherRead": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogVoucherRead); break;
			case "JblTimePeripheralMismatch": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblTimePeripheralMismatch); break;
			case "JblAlarmConnectionPoolLeak": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmConnectionPoolLeak); break;
			case "JblLogVehicleCounterAutoReset": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogVehicleCounterAutoReset); break;
			case "JblAlarmFixedPathTimeViolation": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmFixedPathTimeViolation); break;
			case "JblAlarmFixedPathNotRespected": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmFixedPathNotRespected); break;
			case "JblAlarmConfigUpdateIgnored": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmConfigUpdateIgnored); break;
			case "JblLogConfigUpdateApplied": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogConfigUpdateApplied); break;
			case "JblAlarmVehicleClassMismatch": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmVehicleClassMismatch); break;
			case "JblLogUserCategoryChanged": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogUserCategoryChanged); break;
			case "JblAlarmGatelessOffline": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmGatelessOffline); break;
			case "JmsCancelSalePaymentOperation": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsCancelSalePaymentOperation); break;
			case "JblAlarmTypeShift": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmTypeShift); break;
			case "JblAlarmPeripheralOffline": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmPeripheralOffline); break;
			case "JblAlarmCounterExceeded": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmCounterExceeded); break;
			case "JmsBillingOperation": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsBillingOperation); break;
			case "JmsPaymentOperation": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsPaymentOperation); break;
			case "JmsNegotiatedAmountPayment": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JmsNegotiatedAmountPayment); break;
			case "JblAlarmExpiredSubscriptionEntryWithTicket": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmExpiredSubscriptionEntryWithTicket); break;
			case "JblAlarmContractBlockedAtEntry": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmContractBlockedAtEntry); break;
			case "JblAlarmContractBlockedAtExit": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmContractBlockedAtExit); break;
			case "JblAlarmContractBlockedTotally": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmContractBlockedTotally); break;
			case "JblAlarmAccountBlockedTotally": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmAccountBlockedTotally); break;
			case "JblAlarmAccountBlockedAtEntry": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmAccountBlockedAtEntry); break;
			case "JblAlarmAccountBlockedAtExit": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmAccountBlockedAtExit); break;
			case "JblAlarmLPRWarninglist": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRWarninglist); break;
			case "JblAlarmLPRBlocklist": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLPRBlocklist); break;
			case "JblLogThirdPartyBarcodeVoucherRead": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblLogThirdPartyBarcodeVoucherRead); break;
			case "JpsLogLaneButtonAccess2Pushed": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JpsLogLaneButtonAccess2Pushed); break;
			case "JpsLogLaneButtonAccess2Released": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JpsLogLaneButtonAccess2Released); break;
			case "JblGuestPassCreateEvent": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblGuestPassCreateEvent); break;
			case "JblGuestPassChangeEvent": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblGuestPassChangeEvent); break;
			case "JblGuestPassDeleteEvent": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblGuestPassDeleteEvent); break;
			case "JblGuestPassOvernightsChangedEvent": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblGuestPassOvernightsChangedEvent); break;
			case "JblAlarmMembershipTransitNoPlate": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmMembershipTransitNoPlate); break;
			case "JblAlarmLUnipolNotAccessible": event.setEventSpecCode(hub.jbl.core.dto.jps.event.SpecCodeEnum.JblAlarmLUnipolNotAccessible); break;
			default: throw new InvalidTypeException("Error", event.getEventType());
		}
		return event;
	}

	private Object getEventType(final String eventType) throws InvalidTypeException {
		switch (eventType) {
			default: throw new InvalidTypeException("Error", eventType);
		}
	}

	public static Object getInstance() {
		// After obtaining the instance, it should be cast to JblEventFactoryContainer type if necessary.
		return INSTANCE;
	}
}

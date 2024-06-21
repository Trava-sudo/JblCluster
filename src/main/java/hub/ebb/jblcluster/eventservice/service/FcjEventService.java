package hub.ebb.jblcluster.eventservice.service;

import com.google.common.base.Strings;
import hub.jbl.common.dao.authentication.JpsAuthenticatedPeripheral;
import hub.jbl.common.lib.SessionFields;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.core.dto.jps.authentication.common.JpsPeripheral;
import hub.jbl.core.dto.jps.event.JpsOpOptorLogin;
import hub.jbl.core.dto.jps.event.JpsOptorLoginError;
import hub.jbl.core.dto.jps.event.SpecCodeEnum;
import hub.jbl.dao.FcjOptorShiftDao;
import hub.jbl.dao.JblEventDao;
import hub.jms.common.model.utils.JSONUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class FcjEventService {
    private JBLContext context;
    private JpsAuthenticatedPeripheral jpsAuthenticatedPeripheral;
    private CompletableFuture<AsyncResult<Map<String, String>>> shiftPromise;
    private HashMap<String, String> extendedSession;
    private final JblEventDao jblEventDAO;
    private final FcjOptorShiftDao fcjOptorShiftDao;
    private final JblTransactionManager jblTransactionManager;

    public FcjEventService(JBLContext context, JpsAuthenticatedPeripheral jpsAuthenticatedPeripheral, CompletableFuture<AsyncResult<Map<String, String>>> shiftPromise, HashMap<String, String> extendedSession, JblEventDao jblEventDAO, FcjOptorShiftDao fcjOptorShiftDao, JblTransactionManager jblTransactionManager) {
        this.context = context;
        this.jpsAuthenticatedPeripheral = jpsAuthenticatedPeripheral;
        this.shiftPromise = shiftPromise;
        this.extendedSession = extendedSession;
        this.jblEventDAO = jblEventDAO;
        this.fcjOptorShiftDao = fcjOptorShiftDao;
        this.jblTransactionManager = jblTransactionManager;
    }

    public void invoke() {
        Assert.isTrue(JpsPeripheral.newInstance(jpsAuthenticatedPeripheral.getType()).isFcj(), "fcj device expected");

        this.extendedSession.put(SessionFields.PERIPHERAL_ID, jpsAuthenticatedPeripheral.getPeripheralId());

        if (Strings.isNullOrEmpty(jpsAuthenticatedPeripheral.getPeripheralId())) {
            shiftPromise.complete(Future.succeededFuture(extendedSession));
        } else {
            jblTransactionManager.executeTransaction((conn, transactionBodyResultHandler) -> {
                jblEventDAO.findLastOperatorOperationEvent(context, conn, jpsAuthenticatedPeripheral.getPeripheralId()).thenAccept(eventAsyncResult -> {
                    transactionBodyResultHandler.handle(Future.succeededFuture());
                    if (eventAsyncResult.succeeded()) {
                        if (eventAsyncResult.result() != null && eventAsyncResult.result().getEventSpecCode().equals(SpecCodeEnum.JpsOpOptorLogin.getValue())) {
                            JpsOpOptorLogin jpsOpOptorLogin = JSONUtil.deserialize(eventAsyncResult.result().getJson(), JpsOpOptorLogin.class);
                            if (jpsOpOptorLogin.getError() != null && (jpsOpOptorLogin.getError().equals(JpsOptorLoginError.BeginShift) || jpsOpOptorLogin.getError().equals(JpsOptorLoginError.ResumeShift))) {
                                if (jpsOpOptorLogin.getOperatorData() != null) {
                                    extendedSession.put(SessionFields.USERNAME, jpsOpOptorLogin.getOperatorData().getUsername());
                                }
                                if (jpsOpOptorLogin.getProtocno() != null) {
                                    extendedSession.put(SessionFields.PROTOCOL_NUMBER, "" + jpsOpOptorLogin.getProtocno());
                                }
                                // EBBS-7985 Fix. In some circumstances the shift id on FCJ is reset (maybe after sw update).
                                // The proof is that in fcj_optor_shift table (introduced starting from version 1.26.x) there is registrations
                                // with protocol_number (shift id) greater than the current one used by FCJ. So, due to the fact that
                                // at authentication JBL send to FCJ lastprotocno and FCJ retain it if greater than the used one, we read
                                // also the max per device protocol_number contained into the fcj_optor_shift table and if it's greater than
                                // that acquired reading jbl_event, we use this last one.
                                Future.future(promise -> {
                                    fcjOptorShiftDao.getLast(context, conn, jpsAuthenticatedPeripheral.getPeripheralId())
                                            .onSuccess(fcjOptorShift -> {
                                                if (fcjOptorShift == null) {
                                                    context.getLogger(getClass()).warn("[FcjEventService] Cannot find shift");
                                                    promise.fail("cannot find shift");
                                                    shiftPromise.complete(Future.succeededFuture(extendedSession));
                                                    return;
                                                }
                                                if ((jpsOpOptorLogin.getProtocno() != null && fcjOptorShift.getProtocolNumber() != null && fcjOptorShift.getProtocolNumber() > jpsOpOptorLogin.getProtocno()) ||
                                                    (jpsOpOptorLogin.getProtocno() == null && fcjOptorShift.getProtocolNumber() != null)) {
                                                    extendedSession.put(SessionFields.PROTOCOL_NUMBER, "" + fcjOptorShift.getProtocolNumber());
                                                }
                                                promise.complete();
                                                shiftPromise.complete(Future.succeededFuture(extendedSession));
                                            })
                                            .onFailure(err -> {
                                                shiftPromise.complete(Future.succeededFuture(extendedSession));
                                            });
                                });
                                return;
                            }
                        }
                        shiftPromise.complete(Future.succeededFuture(extendedSession));
                    } else {
                        shiftPromise.complete(Future.failedFuture(eventAsyncResult.cause()));
                    }
                });
            });
        }
    }
}

package hub.ebb.jblcluster.eventservice.mapper;

import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.entity.lpr.JblPlateImageOperation;
import hub.jms.common.model.message.Message;
import hub.jms.common.model.operation.BaseOperation;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created by Stefano.Coletta on 21/11/2016.
 */
public class JmsEvent extends JmsCommonEvent {

    private Set<JblPlateImageOperation> plates = Collections.synchronizedSet(new HashSet());

    private Long jblId;

    public JmsEvent() {
        super();
    }

    public List<JblPlateImageOperation> toPlates() {
        return new ArrayList<>(plates);
    }

    public Long getJblId() {
        return jblId;
    }

    public JmsEvent setJblId(Long jblId) {
        this.jblId = jblId;
        return this;
    }

    public JmsEvent addPlate(JblPlateImageOperation plateImageOperation) {
        if (plateImageOperation != null)
            this.plates.add(plateImageOperation);
        return this;
    }


    public void correlateLicensePlateIdentifier(long identifier, long newIdentifier) {
        if (!CollectionUtils.isEmpty(plates))
            for (JblPlateImageOperation pl : plates) {
                if (identifier == pl.getIdentifier()) {
                    pl.setIdentifier(newIdentifier);
                }
            }
    }

    @Override
    public String toString() {
        return "JmsEvent{" +
                "plates=" + plates +
                ", jblId=" + jblId +
                '}';
    }

    //region FOR SERIALIZATION
    public List<JblPlateImageOperation> getPlates() {
        return toPlates();
    }

    public List<Message> getBaseMessage() {
        return toMessage();
    }

    public List<BaseOperation> getBaseOperation() {
        return toOperation();
    }
    //endregion


}

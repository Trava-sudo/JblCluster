package hub.ebb.jblcluster.eventservice.service.jmsMapper;

import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.exception.JblMapperException;
import hub.jbl.dao.common.JblMapper;
import hub.jbl.entity.parknode.JblParkCounterLimit;
import hub.jbl.entity.parknode.JblParkCounterV2;

import java.util.stream.Collectors;

public class JblParkCounterV2Mapper extends JblMapper<JblParkCounterV2, JblParkCounterV2> {

    @Override
    public JblParkCounterV2 toDto(JBLContext jblContext, JblParkCounterV2 entity) {
        return entity;
    }

    @Override
    public JblParkCounterV2 toEntity(JBLContext jblContext, JblParkCounterV2 dto) throws JblMapperException {
        return new JblParkCounterV2(dto, false);
    }

    @Override
    public JblParkCounterV2 mergeEntity(JBLContext jblContext, JblParkCounterV2 dto, JblParkCounterV2 entity) {
        entity.setShortName(dto.getShortName());
        entity.setName(dto.getName());
        entity.setCounterType(dto.getCounterType());
        entity.setEnabled(dto.isEnabled());
        entity.setNodeId(dto.getNodeId());
        entity.setNoneResetable(dto.isNoneResetable());
        entity.setLowerValueLimit(dto.getLowerValueLimit());
        entity.setUpperValueLimit(dto.getUpperValueLimit());
        entity.setLowerValueLimitActive(dto.isLowerValueLimitActive());
        entity.setUpperValueLimitActive(dto.isUpperValueLimitActive());
        entity.setActionSetValueChangeActive(dto.isActionSetValueChangeActive());
        entity.setActionSetValueChangeId(dto.getActionSetValueChangeId());
        entity.setCounterToBeSent(dto.isCounterToBeSent());
        entity.setIdentifier(dto.getIdentifier());
        entity.setCreatedByMigration(dto.isCreatedByMigration());

        if (dto.counterLimits.isEmpty()) {
            entity.counterLimits.forEach(cl -> cl.setLimitActive(false));
        } else if (entity.counterLimits.isEmpty()) {
            entity.counterLimits.addAll(dto.counterLimits.stream().map(l -> new JblParkCounterLimit(l, false)).collect(Collectors.toList()));
        } else {
            // EBB JMS currently supports only one limit per counter
            entity.counterLimits.get(0).setLimitActive(dto.counterLimits.get(0).isLimitActive());
            entity.counterLimits.get(0).setValue(dto.counterLimits.get(0).getValue());
            entity.counterLimits.get(0).setHysteresis(dto.counterLimits.get(0).getHysteresis());
        }

        return entity;
    }
}

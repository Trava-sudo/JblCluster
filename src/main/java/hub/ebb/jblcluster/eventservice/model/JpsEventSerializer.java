package hub.ebb.jblcluster.eventservice.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import hub.jms.common.model.utils.JSONUtil;

import java.io.IOException;

public class JpsEventSerializer extends JsonSerializer<JblEventExtendedJbl> {


    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }

    @Override
    public void serialize(JblEventExtendedJbl jblEvent, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jblEvent.setJson(JSONUtil.serialize(jblEvent.getJpsEvent()));
        String json = JSONUtil.serialize(jblEvent);
        jsonGenerator.writeString(json);
    }


}

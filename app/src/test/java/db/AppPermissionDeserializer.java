package db;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppPermissionDeserializer extends JsonDeserializer<List<AppPermissionInfo>> {

    private static final String NODE_NAME = "AppPermission";

    @Override
    public List<AppPermissionInfo> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        List<AppPermissionInfo> list = Collections.emptyList();
        JsonNode node = jp.readValueAsTree();
        JsonNode appPermissionNode = node.get(NODE_NAME);
        if (appPermissionNode != null && appPermissionNode.isArray()) {
            ObjectMapper mapper = new ObjectMapper();
            list = Arrays.asList(mapper.convertValue(appPermissionNode, AppPermissionInfo[].class));
        }
        return list;
    }

}

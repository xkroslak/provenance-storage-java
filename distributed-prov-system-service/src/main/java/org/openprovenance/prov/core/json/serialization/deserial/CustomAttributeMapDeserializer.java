package org.openprovenance.prov.core.json.serialization.deserial;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.openprovenance.prov.model.Attribute;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.openprovenance.prov.core.json.serialization.deserial.CustomThreadConfig.JSON_CONTEXT_KEY_NAMESPACE;
import static org.openprovenance.prov.core.json.serialization.deserial.CustomThreadConfig.getAttributes;

/**
  Custom deserializer for a PROV attribute map encoded as:
  {"{qualifiedName}": [{attribute}, {attribute}, ... ], ... }
 
  Why needed:
  - JSON object keys are qualified names that must be resolved using the active Namespace.
  - Attribute parsing depends on the owning element name (the key), which Jackson does not
    provide to standard deserializers for map values.
  This class injects namespace context and the owning element name while building
  a Map<QualifiedName, Set<Attribute>>.
 **/
public class CustomAttributeMapDeserializer extends StdDeserializer<Map> {

    static final ProvFactory pf = new ProvFactory();

    public CustomAttributeMapDeserializer() {
        this(defaultMapType());
    }

    private static JavaType defaultMapType() {
        TypeFactory typeFactory = TypeFactory.defaultInstance();
        return typeFactory.constructMapType(
                Map.class,
                typeFactory.constructType(QualifiedName.class),
                typeFactory.constructCollectionType(Set.class, Attribute.class)
        );
    }

    public CustomAttributeMapDeserializer(JavaType vc) {
        super(vc);
    }

    @Override
    public Map deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
        // Namespace is provided via thread-local config during parsing.
        Namespace ns = getAttributes().get().get(JSON_CONTEXT_KEY_NAMESPACE);
        Map<QualifiedName, Set<Attribute>> result = new HashMap<>();
        JsonNode node = jp.getCodec().readTree(jp);

        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> pair = it.next();
            QualifiedName elementName = ns.stringToQualifiedName(pair.getKey(), pf);
            JsonNode vObj = pair.getValue();
            Iterator<JsonNode> elements = vObj.elements();
            Set<Attribute> set = new HashSet<>();
            while (elements.hasNext()) {
                JsonNode next = elements.next();
                // Each attribute is parsed with the owning element name as context.
                Attribute attr = new CustomAttributeDeserializerWithRootName().deserialize(elementName, next, deserializationContext);
                set.add(attr);
            }
            result.put(elementName, set);
        }

        return result;
    }
}

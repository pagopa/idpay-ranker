package it.gov.pagopa.common.config.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class PageModule extends SimpleModule {
    static final String CONTENT = "content";
    static final String NUMBER = "number";
    static final String SIZE = "size";
    static final String PAGE_NUMBER = "pageNumber";
    static final String PAGE_SIZE = "pageSize";
    static final String TOTAL_ELEMENTS = "totalElements";
    static final String TOTAL = "total";
    static final String SORT = "sort";
    static final String ORDERS = "orders";
    static final String DIRECTION = "direction";
    static final String PROPERTY = "property";

    @Serial
    private static final long serialVersionUID = 1L;

    public PageModule() {
        addDeserializer(Page.class, new PageDeserializer());
        addSerializer(Page.class, new PageSerializer<>());
    }
}

class PageDeserializer extends JsonDeserializer<Page<?>> implements ContextualDeserializer {
    private static final String PAGEABLE = "pageable";

    private JavaType valueType;

    @Override
    public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final CollectionType valuesListType = ctxt.getTypeFactory().constructCollectionType(List.class, valueType);

        List<?> list = new ArrayList<>();
        int pageNumber = 0;
        int pageSize = 0;
        long total = 0;
        Sort sort = Sort.unsorted();

        if (p.isExpectedStartObjectToken()) {
            p.nextToken();
            if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
                String propName = p.getCurrentName();
                do {
                    p.nextToken();
                    switch (propName) {
                        case PageModule.CONTENT:
                            list = ctxt.readValue(p, valuesListType);
                            break;
                        case PAGEABLE:
                            break;
                        case PageModule.NUMBER, PageModule.PAGE_NUMBER:
                            pageNumber = ctxt.readValue(p, Integer.class);
                            break;
                        case PageModule.SIZE, PageModule.PAGE_SIZE:
                            pageSize = ctxt.readValue(p, Integer.class);
                            break;
                        case PageModule.TOTAL_ELEMENTS, PageModule.TOTAL:
                            total = ctxt.readValue(p, Long.class);
                            break;
                        case PageModule.SORT:
                            sort = ctxt.readValue(p, WrappedSort.class);
                            break;
                        default:
                            p.skipChildren();
                            break;
                    }
                } while ((propName = p.nextFieldName()) != null);
            } else {
                ctxt.handleUnexpectedToken(handledType(), p);
            }
        } else {
            ctxt.handleUnexpectedToken(handledType(), p);
        }

        //Note that Sort field of Page is ignored here.
        // Feel free to add more switch cases above to deserialize it as well.
        return new PageImpl<>(list, PageRequest.of(pageNumber, pageSize, sort), total);
    }

    private static class WrappedSort extends Sort {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        protected WrappedSort(@JsonProperty(PageModule.ORDERS) List<WrappedOrder> orders) {
            super(orders.stream().map(Order.class::cast).toList());
        }
    }

    private static class WrappedOrder extends Sort.Order {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public WrappedOrder(@JsonProperty(PageModule.DIRECTION) Sort.Direction direction, @JsonProperty(PageModule.PROPERTY) String property) {
            super(direction, property);
        }
    }

    /**
     * This is the main point here.
     * The PageDeserializer is created for each specific deserialization with concrete generic parameter type of Page.
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        //This is the Page actually
        final JavaType wrapperType = ctxt.getContextualType();
        final PageDeserializer deserializer = new PageDeserializer();
        //This is the parameter of Page
        deserializer.valueType = wrapperType.containedType(0);
        return deserializer;
    }

}

class  PageSerializer<T extends Page<?>> extends JsonSerializer<T> {
    @Override
    public void serialize(T page, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(PageModule.CONTENT, page.getContent());
        jsonGenerator.writeBooleanField("first", page.isFirst());
        jsonGenerator.writeBooleanField("last", page.isLast());
        jsonGenerator.writeNumberField("totalPages", page.getTotalPages());
        jsonGenerator.writeNumberField(PageModule.TOTAL_ELEMENTS, page.getTotalElements());
        jsonGenerator.writeNumberField("numberOfElements", page.getNumberOfElements());

        jsonGenerator.writeNumberField(PageModule.SIZE, page.getSize());
        jsonGenerator.writeNumberField(PageModule.NUMBER, page.getNumber());

        Sort sort = page.getSort();

        jsonGenerator.writeObjectFieldStart(PageModule.SORT);

        jsonGenerator.writeBooleanField("empty", sort.isEmpty());
        jsonGenerator.writeBooleanField("sorted", sort.isSorted());
        jsonGenerator.writeBooleanField("unsorted", sort.isUnsorted());

        jsonGenerator.writeArrayFieldStart(PageModule.ORDERS);

        for (Sort.Order order : sort) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(PageModule.PROPERTY, order.getProperty());
            jsonGenerator.writeStringField(PageModule.DIRECTION, order.getDirection().name());
            jsonGenerator.writeBooleanField("ignoreCase", order.isIgnoreCase());
            jsonGenerator.writeStringField("nullHandling", order.getNullHandling().name());
            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

}

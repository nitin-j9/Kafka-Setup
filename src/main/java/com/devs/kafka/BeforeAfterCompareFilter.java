package com.devs.kafka;

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.transforms.SmtManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.transforms.ExtractField;
import org.apache.kafka.connect.transforms.ReplaceField;
import org.apache.kafka.connect.transforms.Transformation;
import java.util.HashMap;import java.util.Map;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
@Slf4j
public class BeforeAfterCompareFilter<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final Field EXCLUDE_COLUMN_LIST = Field.create("exclude.column.list").withDisplayName("Exclude column list").withType(ConfigDef.Type.LIST).withDescription("Exclude list of column while comparing debezium envelope object");
    private final ExtractField<R> afterDelegate = new ExtractField.Value<>();
    private final ExtractField<R> beforeDelegate = new ExtractField.Value<>();
    private final ReplaceField<R> excludeColumnDelegate = new ReplaceField.Value<>();
    private SmtManager<R> smtManager;private boolean hasExclusionFields;
    @Override
    public void configure(final Map<String, ?> configs) {
        final Configuration config = Configuration.from(configs);
        smtManager = new SmtManager<>(config);
        final Field.Set configFields = Field.setOf(EXCLUDE_COLUMN_LIST);
        if (!config.validateAndRecord(configFields, log::error)) {
            throw new ConnectException("Unable to validate config.");
        }
        Map<String, String> delegateConfig = new HashMap<>();
        delegateConfig.put("field", "before");
        beforeDelegate.configure(delegateConfig);
        delegateConfig = new HashMap<>();
        delegateConfig.put("field", "after");
        afterDelegate.configure(delegateConfig);
        String excludeColumnList = config.getString(EXCLUDE_COLUMN_LIST);
        hasExclusionFields = nonNull(excludeColumnList) && !excludeColumnList.isEmpty();
        delegateConfig = new HashMap<>();
        delegateConfig.put("exclude", excludeColumnList);
        excludeColumnDelegate.configure(delegateConfig);
    }

    @Override
    public R apply(final R record) {
        if (record.value() == null || !smtManager.isValidEnvelope(record)) {
            log.warn("Not a debezium envelope");
            return record;
        }
        R beforeRecord = beforeDelegate.apply(record);
        R afterRecord = afterDelegate.apply(record);
        if (isNull(beforeRecord.value()) || isNull(afterRecord.value())) {
            return record;
        }
        if (hasExclusionFields) {
            beforeRecord = excludeColumnDelegate.apply(beforeRecord);
            afterRecord = excludeColumnDelegate.apply(afterRecord);
        }
        if (beforeRecord.equals(afterRecord)) {
            log.debug("Skipping the record for topic: {} ,key: {} and value: {}", record.topic(), record.key(), record.value());
            return null;
        }
        return record;
    }
    @Override
    public ConfigDef config() {
        return Field.group(new ConfigDef(), null, EXCLUDE_COLUMN_LIST);
    }
    @Override
    public void close() {
        beforeDelegate.close();
        afterDelegate.close();
        excludeColumnDelegate.close();
    }
}
;
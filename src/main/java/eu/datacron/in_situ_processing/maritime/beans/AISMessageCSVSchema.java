package eu.datacron.in_situ_processing.maritime.beans;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.apache.flink.streaming.util.serialization.SerializationSchema;
import org.apache.flink.util.ReflectionUtil;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import eu.datacron.in_situ_processing.common.utils.ReflectionUtils;


/**
 * A serialization schema for the {@link AISMessage}
 * 
 * @author ehab.qadah
 */
public class AISMessageCSVSchema implements SerializationSchema<AISMessage>,
    DeserializationSchema<AISMessage> {

  private static final long serialVersionUID = 4339578918900034257L;
  private static final Logger logger = Logger.getLogger(AISMessageCSVSchema.class.getName());
  private transient JSONObject parsingJsonConfigs;
  private String parsingJsonConfigsStr;

  public AISMessageCSVSchema() {}

  public AISMessageCSVSchema(String parsingJsonConfigsStr) {
    this.parsingJsonConfigsStr = parsingJsonConfigsStr;
    initParsingConfigObject();
  }

  private void initParsingConfigObject() {
    // make sure that parsing config object is initialized
    if (parsingJsonConfigs == null) {
      this.parsingJsonConfigs = new JSONObject(parsingJsonConfigsStr);
    }
  }

  @Override
  public TypeInformation<AISMessage> getProducedType() {
    return TypeExtractor.getForClass(AISMessage.class);

  }

  @Override
  public byte[] serialize(AISMessage element) {
    return element.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public AISMessage deserialize(byte[] aisMessageBytes) {
    // Deserialize the byte array of csv line
    String csvLine = new String(aisMessageBytes, StandardCharsets.UTF_8);
    return parseCSVline(csvLine);

  }

  private AISMessage parseCSVline(String csvLine) {
    initParsingConfigObject();
    AISMessage aisMessage = new AISMessage();
    String delimiter = parsingJsonConfigs.getString("delimiter");
    String[] fieldsValue = csvLine.split(delimiter);

    for (Field field : AISMessage.class.getFields()) {
      String fieldName = field.getName();

      // Get value of the field from the csv line based on its index
      int fieldIndex = parsingJsonConfigs.getInt(fieldName);
      // Casr the string value of the field based on its acutal type
      Object castedFieldValue = ReflectionUtils.getCastedFieldValue(field, fieldsValue[fieldIndex]);
      try {
        // set the value of the field from the csv line using reflection
        field.set(aisMessage, castedFieldValue);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        logger.error(e.getMessage());
      }
    }
    return aisMessage;
  }



  @Override
  public boolean isEndOfStream(AISMessage nextElement) {
    return false;
  }
}

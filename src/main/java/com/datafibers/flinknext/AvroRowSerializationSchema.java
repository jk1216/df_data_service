package com.datafibers.flinknext;

import com.datafibers.util.ConstantApp;
import com.datafibers.util.SchemaRegistryClient;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.util.Utf8;
import org.apache.flink.streaming.util.serialization.SerializationSchema;
import org.apache.flink.types.Row;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;

/**
 * Deserialization schema from AVRO to {@link Row}.
 *
 * <p>Deserializes the <code>byte[]</code> messages as a AVROject and reads
 * the specified fields.
 *
 * <p>Failure during deserialization are forwarded as wrapped IOExceptions.
 */
public class AvroRowSerializationSchema implements SerializationSchema<Row> {

    private static final long serialVersionUID = 4330538776656642780L;
    private static final Logger LOG = Logger.getLogger(AvroRowSerializationSchema.class);
	/**
	 * Output stream to serialize records into byte array.
	 */
	//private transient ByteArrayOutputStream out =  new ByteArrayOutputStream();

	/**
	 * Low-level class for serialization of Avro values.
	 */
	//private EncoderFactory encoderFactory = EncoderFactory.get();
	//private transient BinaryEncoder encoder;
    //private transient Schema schema;
    //private Schema schema;
    protected final Properties properties;
    
    /** Generic Avro Schema reader for the row */
    private transient DatumWriter<Object> writer;

    /** TODO - When schema changes, the Source table does not need to be recreated.*/

    /**
     * Creates a AVRO serializtion schema for the given schema.
     *
     * @param schema Names of AVRO fields to parse.
     */
    //public AvroRowSerializationSchema(Schema schema) {
        //this.schema = schema;
    //}
    public AvroRowSerializationSchema(Properties properties) {
        this.properties = properties;
    }
    @Override
    public byte[] serialize(Row row) {
        try {
            int schemaId = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(ConstantApp.MAGIC_BYTE);
            out.write(ByteBuffer.allocate(ConstantApp.idSize).putInt(schemaId).array());

            //JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
            BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(out, null);
            Schema schema = SchemaRegistryClient.getLatestSchemaFromProperty(properties);
            DatumWriter<Object> writer = new GenericDatumWriter<Object>(schema);

            writer.write(convertToRecord(schema, row), encoder);
            encoder.flush();

            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;

        } catch (IOException t) {
            t.printStackTrace();
        	throw new RuntimeException("Failed to serialize Row.", t);
        }
    }

    /**
     * Converts a (nested) Flink Row into Avro's {@link GenericRecord}.
     * Strings are converted into Avro's {@link Utf8} fields.
     */
    private static Object convertToRecord(Schema schema, Object rowObj) {
    	LOG.info("schema: " + schema);
    	LOG.info("rowObj: " + rowObj);
    	if (rowObj instanceof Row) {
            // records can be wrapped in a union
            if (schema.getType() == Schema.Type.UNION) {
                final List<Schema> types = schema.getTypes();
                if (types.size() == 2 && types.get(0).getType() == Schema.Type.NULL && types.get(1).getType() == Schema.Type.RECORD) {
                    schema = types.get(1);
                }
                else if (types.size() == 2 && types.get(0).getType() == Schema.Type.RECORD && types.get(1).getType() == Schema.Type.NULL) {
                    schema = types.get(0);
                }
                else {
                    throw new RuntimeException("Currently we only support schemas of the following form: UNION[null, RECORD] or UNION[RECORD, NULL] Given: " + schema);
                }
            } else if (schema.getType() != Schema.Type.RECORD) {
                throw new RuntimeException("Record type for row type expected. But is: " + schema);
            }
            final List<Schema.Field> fields = schema.getFields();
            final GenericRecord record = new GenericData.Record(schema);
            final Row row = (Row) rowObj;
            for (int i = 0; i < fields.size(); i++) {
                final Schema.Field field = fields.get(i);
                record.put(field.pos(), convertToRecord(field.schema(), row.getField(i)));
            }
            return record;
        } else if (rowObj instanceof String) {
            return new Utf8((String) rowObj);
        } else {
            return rowObj;
        }
    }

}

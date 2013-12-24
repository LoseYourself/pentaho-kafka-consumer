package com.ruckuswireless.pentaho;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.consumer.ConsumerConfig;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

public class KafkaConsumerMeta extends BaseStepMeta implements StepMetaInterface {

	public static final String[] KAFKA_PROPERTIES_NAMES = new String[] { "group.id", "zookeeper.connect",
			"consumer.id", "socket.timeout.ms", "socket.receive.buffer.bytes", "fetch.message.max.bytes",
			"auto.commit.interval.ms", "queued.max.message.chunks", "rebalance.max.retries", "fetch.min.bytes",
			"fetch.wait.max.ms", "rebalance.backoff.ms", "refresh.leader.backoff.ms", "auto.commit.enable",
			"auto.offset.reset", "consumer.timeout.ms", "client.id", "zookeeper.session.timeout.ms",
			"zookeeper.connection.timeout.ms", "zookeeper.sync.time.ms" };

	private Properties kafkaProperties = new Properties();
	private String topic;
	private String field;

	Properties getKafkaProperties() {
		return kafkaProperties;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public ConsumerConfig createConsumerConfig() {
		return new ConsumerConfig(kafkaProperties);
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
			RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {

		if (topic == null) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages
					.getString("KafkaConsumerMeta.Check.InvalidTopic"), stepMeta));
		}
		if (field == null) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages
					.getString("KafkaConsumerMeta.Check.InvalidField"), stepMeta));
		}
		try {
			new ConsumerConfig(kafkaProperties);
		} catch (IllegalArgumentException e) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, e.getMessage(), stepMeta));
		}
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans trans) {
		return new KafkaConsumerStep(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData() {
		return new KafkaConsumerData();
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleXMLException {

		try {
			topic = XMLHandler.getTagValue(stepnode, "TOPIC");
			field = XMLHandler.getTagValue(stepnode, "FIELD");
			Node kafkaNode = XMLHandler.getSubNode(stepnode, "KAFKA");
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = XMLHandler.getTagValue(kafkaNode, name);
				if (value != null) {
					kafkaProperties.put(name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleXMLException(Messages.getString("KafkaConsumerMeta.Exception.loadXml"), e);
		}
	}

	public String getXML() throws KettleException {
		StringBuilder retval = new StringBuilder();
		if (topic != null) {
			retval.append("    ").append(XMLHandler.addTagValue("TOPIC", topic));
		}
		if (field != null) {
			retval.append("    ").append(XMLHandler.addTagValue("FIELD", field));
		}
		retval.append("    ").append(XMLHandler.openTag("KAFKA")).append(Const.CR);
		for (String name : KAFKA_PROPERTIES_NAMES) {
			String value = kafkaProperties.getProperty(name);
			if (value != null) {
				retval.append("      " + XMLHandler.addTagValue(name, value));
			}
		}
		retval.append("    ").append(XMLHandler.closeTag("KAFKA")).append(Const.CR);
		return retval.toString();
	}

	public void readRep(Repository rep, ObjectId stepId, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleException {
		try {
			topic = rep.getStepAttributeString(stepId, "TOPIC");
			field = rep.getStepAttributeString(stepId, "FIELD");
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = rep.getStepAttributeString(stepId, name);
				if (value != null) {
					kafkaProperties.put(name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleException("KafkaConsumerMeta.Exception.loadRep", e);
		}
	}

	public void saveRep(Repository rep, ObjectId transformationId, ObjectId stepId) throws KettleException {
		try {
			if (topic != null) {
				rep.saveStepAttribute(transformationId, stepId, "TOPIC", topic);
			}
			if (field != null) {
				rep.saveStepAttribute(transformationId, stepId, "FIELD", field);
			}
			for (String name : KAFKA_PROPERTIES_NAMES) {
				String value = kafkaProperties.getProperty(name);
				if (value != null) {
					rep.saveStepAttribute(transformationId, stepId, name, value);
				}
			}
		} catch (Exception e) {
			throw new KettleException("KafkaConsumerMeta.Exception.saveRep", e);
		}
	}

	public void setDefault() {
	}

	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space) throws KettleStepException {
		rowMeta.clear();
		ValueMetaInterface valueMeta = new ValueMeta(field, ValueMetaInterface.TYPE_BINARY);
		rowMeta.addValueMeta(valueMeta);
	}
}
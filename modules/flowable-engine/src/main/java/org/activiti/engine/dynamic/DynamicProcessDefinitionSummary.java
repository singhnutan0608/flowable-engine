package org.activiti.engine.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.DynamicBpmnConstants;

import java.util.HashMap;

/**
 * Pojo class who can be used to check information between {@link org.activiti.engine.DynamicBpmnService#getProcessDefinitionInfo(String)}
 * and {@link org.activiti.bpmn.model.BpmnModel}. Without exposing the internal behavoir of activiti's logic.
 *
 * Created by Pardo David on 5/12/2016.
 */
public class DynamicProcessDefinitionSummary implements DynamicBpmnConstants {
	private static final HashMap<String, PropertiesParser> summaryParsers = new HashMap<String,PropertiesParser>();
	private static final PropertiesParser defaultParser = new DefaultPropertiesParser();
	private BpmnModel bpmnModel;
	private ObjectNode processInfo;
	private ObjectMapper objectMapper;

	static {
		summaryParsers.put(UserTask.class.getSimpleName(),new UserTaskPropertiesParser());
		summaryParsers.put(ScriptTask.class.getSimpleName(),new ScriptTaskPropertiesParser());
	}

	public DynamicProcessDefinitionSummary(BpmnModel bpmnModel, ObjectNode processInfo, ObjectMapper objectMapper) {
		this.bpmnModel = bpmnModel;
		this.processInfo = processInfo;
		this.objectMapper = objectMapper;
	}

	/**
	 * Returns the summary in the following structure:
	 * <pre>
	 * {
	 *     "elementId": (the elements id)
	 *     "elementType": (the elements type)
	 *     "elementSummary": {
	 *         "{@link org.activiti.engine.DynamicBpmnConstants} linked to the elementType": {
	 *             bpmnmodel : (array of strings | string | not provided if empty / blank / null)
	 *             dynamic: (array of strings or string or not provided if blank or empty)
	 *         }
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>
	 *     If no value is found for a given {@link org.activiti.engine.DynamicBpmnConstants} in the {@link BpmnModel} or
	 *     ProcessDefinitionInfo. we don't store an key in the resulting {@link ObjectNode}. Null values should be avoided
	 *     in JSON. Depending on the {@link ObjectMapper} configuration keys with a null value could even be removed when writting to json.
	 * </p>
	 *
	 * <p color="red">
	 *     Currently supported flow elements are:
	 *     <li>
	 *         <ul>UserTask</ul>
	 *         <ul>ScriptTask</ul>
	 *     </li>
	 *     No summary will field will be created for other elements. ElementId, and elementType will be available.
	 * </p>
	 * @param elementId the id of the {@link org.activiti.bpmn.model.FlowElement}.
	 * @return an {@link ObjectNode} with the provided structure.
	 * @throws IllegalStateException if no {@link org.activiti.bpmn.model.FlowElement} is found for the provided id.
	 */
	public ObjectNode getElement(String elementId) throws IllegalStateException{

		FlowElement flowElement = bpmnModel.getFlowElement(elementId);
		if(flowElement == null){
			throw new IllegalStateException("No flow element with id " + elementId + " found in bpmnmodel " + bpmnModel.getMainProcess().getId());
		}

		PropertiesParser propertiesParser = summaryParsers.get(flowElement.getClass().getSimpleName());
		ObjectNode bpmnProperties = getBpmnProperties(elementId, processInfo);
		if(propertiesParser != null){
			return propertiesParser.parseElement(flowElement,bpmnProperties,objectMapper);
		}else{
			//if there is no parser for an element we have to use the default summary parser.
			return defaultParser.parseElement(flowElement,bpmnProperties,objectMapper);
		}
	}


	public ObjectNode getSummary() {
		ObjectNode summary = objectMapper.createObjectNode();

		for(Process process : bpmnModel.getProcesses()){
			for(FlowElement flowElement : process.getFlowElements()){
				summary.set(flowElement.getId(),getElement(flowElement.getId()));
			}
		}

		return summary;
	}

	private ObjectNode getBpmnProperties(String elementId, ObjectNode processInfoNode){
		JsonNode bpmnNode = processInfoNode.get(BPMN_NODE);
		if(bpmnNode != null){
			JsonNode elementNode = bpmnNode.get(elementId);
			if(elementNode == null){
				return objectMapper.createObjectNode();
			}else{
				return (ObjectNode) elementNode;
			}
		}else{
			return objectMapper.createObjectNode();
		}
	}
}

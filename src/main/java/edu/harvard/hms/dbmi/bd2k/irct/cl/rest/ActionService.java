package edu.harvard.hms.dbmi.bd2k.irct.cl.rest;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.servlet.http.HttpSession;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.harvard.hms.dbmi.bd2k.irct.controller.ActionController;
import edu.harvard.hms.dbmi.bd2k.irct.controller.ExecutionController;
import edu.harvard.hms.dbmi.bd2k.irct.controller.JoinController;
import edu.harvard.hms.dbmi.bd2k.irct.controller.ResourceController;
import edu.harvard.hms.dbmi.bd2k.irct.exception.ActionException;
import edu.harvard.hms.dbmi.bd2k.irct.exception.JoinException;
import edu.harvard.hms.dbmi.bd2k.irct.exception.QueryException;
import edu.harvard.hms.dbmi.bd2k.irct.model.join.IRCTJoin;
import edu.harvard.hms.dbmi.bd2k.irct.model.ontology.Entity;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.JoinType;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.PredicateType;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.SelectOperationType;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.SortOperationType;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.SubQuery;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.Field;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.LogicalOperator;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.Resource;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.PersistableException;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;

/**
 * Creates a REST interface for the action service
 * 
 * @author Jeremy R. Easton-Marks
 *
 */
@Path("/actionService")
@RequestScoped
@Named
public class ActionService implements Serializable {
	private static final long serialVersionUID = 2374441778286932013L;

	@Inject
	private ActionController ac;

	@Inject
	private ResourceController rc;

	@Inject
	private ExecutionController ec;

	@Inject
	private JoinController jc;

	@Inject
	private HttpSession session;

	/**
	 * Runs an action using a JSON representation of the Query
	 * 
	 * @param payload
	 *            JSON
	 * @return Result Id
	 * @throws ActionException
	 */
	@POST
	@Path("/run")
	@Produces(MediaType.APPLICATION_JSON)
	public Response runQuery(String payload) {
		JsonObjectBuilder response = Json.createObjectBuilder();

		JsonReader jsonReader = Json.createReader(new StringReader(payload));
		JsonObject jsonQuery = jsonReader.readObject();
		jsonReader.close();
		try {
			ac.createAction();
			convertJsonToAction(jsonQuery);
		} catch (QueryException | JoinException | ActionException e) {
			response.add("status", "Invalid Request");
			response.add("message", e.getMessage());
			return Response.status(400).entity(response.build()).build();
		}

		try {
			response.add("jobId", ec.runExecutable(ac.getRootExecutionNode(),
					(SecureSession) session.getAttribute("secureSession")));
		} catch (PersistableException e) {
			response.add("status", "Error running request");
			response.add("message", "An error occurred running this request");
			return Response.status(400).entity(response.build()).build();
		} catch (ActionException e) {
			response.add("status", "Error running request");
			response.add("message", e.getMessage());
			return Response.status(400).entity(response.build()).build();
		}

		return Response.ok(response.build(), MediaType.APPLICATION_JSON)
				.build();
	}

	private void convertJsonToAction(JsonObject jsonAction)
			throws QueryException, ActionException, JoinException {

		// Is Action type of Query?
		if (jsonAction.containsKey("select") || jsonAction.containsKey("where")) {
			convertJsonQueryToAction(jsonAction);
		} else if (jsonAction.containsKey("resource")) {
			// Is Action type of Process?
			convertJsonProcessToAction(jsonAction);
		} else if (jsonAction.containsKey("joinType")) {
			// Is Action type of IRCT Join
			convertJsonJoinToAction(jsonAction);
		} else {
			// Is action of type unknown
			throw new ActionException("Unknown Action Type");
		}
	}

	

	private void convertJsonJoinToAction(JsonObject jsonJoin) {
		
	}
	
	private void convertJsonProcessToAction(JsonObject jsonProcess) {
		
	}

	private void convertJsonQueryToAction(JsonObject jsonQuery)
			throws QueryException, ActionException, JoinException {

		SubQuery subQuery = null;

		// Convert JSON Selects
		if (jsonQuery.containsKey("select")) {
			JsonArray selectClauses = jsonQuery.getJsonArray("select");
			Iterator<JsonValue> selectIterator = selectClauses.iterator();
			while (selectIterator.hasNext()) {
				addJsonSelectClauseToQuery(subQuery,
						(JsonObject) selectIterator.next());
			}
		}
		// Convert JSON Where
		if (jsonQuery.containsKey("where")) {
			JsonArray whereClauses = jsonQuery.getJsonArray("where");
			Iterator<JsonValue> whereIterator = whereClauses.iterator();
			while (whereIterator.hasNext()) {
				addJsonWhereClauseToQuery(subQuery,
						(JsonObject) whereIterator.next());
			}
		}
		// Convert JSON Sort to Query
		if (jsonQuery.containsKey("sort")) {
			JsonArray sortClauses = jsonQuery.getJsonArray("sort");
			Iterator<JsonValue> sortIterator = sortClauses.iterator();
			while (sortIterator.hasNext()) {
				addJsonSortClauseToQuery(subQuery,
						(JsonObject) sortIterator.next());
			}
		}

		// Convert JSON Join to Query
		if (jsonQuery.containsKey("join")) {
			JsonArray sortClauses = jsonQuery.getJsonArray("join");
			Iterator<JsonValue> sortIterator = sortClauses.iterator();
			while (sortIterator.hasNext()) {
				addJsonJoinClauseToQuery(subQuery,
						(JsonObject) sortIterator.next());
			}
		}
	}

	private void addJsonSelectClauseToQuery(SubQuery sq, JsonObject selectClause)
			throws QueryException, ActionException {
		String path = null;
		String dataType = null;
		if (selectClause.containsKey("field")) {
			path = selectClause.getJsonObject("field").getString("pui");
			if (selectClause.getJsonObject("field").containsKey("dataType")) {
				dataType = selectClause.getJsonObject("field").getString(
						"dataType");
			}
		}

		Entity entity = null;
		Resource resource = null;
		if (path != null && !path.isEmpty()) {
			path = "/" + path;
			path = path.substring(1);
			resource = rc.getResource(path.split("/")[1]);
			if (resource == null) {
				throw new QueryException("Invalid Resource");
			}
			entity = new Entity(path);
			if (dataType != null) {
				entity.setDataType(resource.getDataTypeByName(dataType));
			}
		}

		String alias = null;
		if (selectClause.containsKey("alias")) {
			alias = selectClause.getString("alias");
		}

		String operationName = null;

		SelectOperationType operation = resource
				.getSupportedSelectOperationByName(operationName);

		Map<String, Object> objectFields = new HashMap<String, Object>();
		Map<String, String> fields = new HashMap<String, String>();
		if (selectClause.containsKey("operation")) {
			operationName = selectClause.getString("operation");

			if (operation == null) {
				throw new QueryException("Unsupported Select Operation Type");
			}

			Map<String, Field> clauseFields = new HashMap<String, Field>();
			for (Field field : operation.getFields()) {
				clauseFields.put(field.getPath(), field);
			}

			if (selectClause.containsKey("fields")) {
				JsonObject fieldObject = selectClause.getJsonObject("fields");
				objectFields = getObjectFields(clauseFields, fieldObject);
				fields = getStringFields(clauseFields, fieldObject);
			}
		}

		if ((resource.getSupportedSelectFields() != null)
				&& (!resource.getSupportedSelectFields().isEmpty())) {

			Map<String, Field> clauseFields = new HashMap<String, Field>();
			for (Field field : resource.getSupportedSelectFields()) {
				clauseFields.put(field.getPath(), field);
			}

			if (selectClause.containsKey("fields")) {
				JsonObject fieldObject = selectClause.getJsonObject("fields");
				objectFields = getObjectFields(clauseFields, fieldObject);
				fields = getStringFields(clauseFields, fieldObject);
			}
		}

		ac.addSelectClause(entity, alias, operation, fields, objectFields);
	}

	private void addJsonWhereClauseToQuery(SubQuery sq, JsonObject whereClause)
			throws QueryException, ActionException {
		String path = null;
		String dataType = null;
		if (whereClause.containsKey("field")) {
			path = whereClause.getJsonObject("field").getString("pui");
			if (whereClause.getJsonObject("field").containsKey("dataType")) {
				dataType = whereClause.getJsonObject("field").getString(
						"dataType");
			}
		}

		Entity entity = null;
		Resource resource = null;
		if (path != null && !path.isEmpty()) {
			path = "/" + path;
			path = path.substring(1);
			resource = rc.getResource(path.split("/")[1]);
			entity = new Entity(path);
			if (dataType != null) {
				entity.setDataType(resource.getDataTypeByName(dataType));
			}
		}
		if ((resource == null) || (entity == null)) {
			throw new QueryException("Invalid Path");
		}
		String predicateName = whereClause.getString("predicate");
		String logicalOperatorName = null;
		if (whereClause.containsKey("logicalOperator")) {
			logicalOperatorName = whereClause.getString("logicalOperator");
		}

		PredicateType predicateType = resource
				.getSupportedPredicateByName(predicateName);
		if (predicateType == null) {
			throw new QueryException("Unknown predicate type");
		}

		Map<String, Field> clauseFields = new HashMap<String, Field>();
		for (Field field : predicateType.getFields()) {
			clauseFields.put(field.getPath(), field);
		}

		Map<String, Object> objectFields = new HashMap<String, Object>();
		Map<String, String> fields = new HashMap<String, String>();

		if (whereClause.containsKey("fields")) {
			JsonObject fieldObject = whereClause.getJsonObject("fields");
			objectFields = getObjectFields(clauseFields, fieldObject);
			fields = getStringFields(clauseFields, fieldObject);
		}

		LogicalOperator logicalOperator = null;
		if (logicalOperatorName != null) {
			logicalOperator = resource
					.getLogicalOperatorByName(logicalOperatorName);
			if (logicalOperator == null) {
				throw new QueryException("Unknown logical operator");
			}
		}

		ac.addWhereClause(entity, predicateType, logicalOperator, fields,
				objectFields);

	}

	private void addJsonSortClauseToQuery(SubQuery sq, JsonObject sortClause)
			throws QueryException, ActionException {
		String path = null;
		if (sortClause.containsKey("field")) {
			path = sortClause.getJsonObject("field").getString("pui");
		}

		Entity entity = null;
		Resource resource = null;
		if (path != null && !path.isEmpty()) {
			path = "/" + path;
			path = path.substring(1);
			resource = rc.getResource(path.split("/")[1]);
			if (resource == null) {
				throw new QueryException("Invalid Resource");
			}
			entity = new Entity(path);
		}
		if ((resource == null) || (entity == null)) {
			throw new QueryException("Invalid Path");
		}

		if (!sortClause.containsKey("sortType")) {
			throw new QueryException("No sort type defined");
		}
		String sortName = sortClause.getString("sortType");

		SortOperationType sortType = resource
				.getSupportedSortOperationByName(sortName);
		if (sortType == null) {
			throw new QueryException("Unknown sort type");
		}

		Map<String, Field> clauseFields = new HashMap<String, Field>();
		for (Field field : sortType.getFields()) {
			clauseFields.put(field.getPath(), field);
		}

		Map<String, Object> objectFields = new HashMap<String, Object>();
		Map<String, String> fields = new HashMap<String, String>();

		if (sortClause.containsKey("fields")) {
			JsonObject fieldObject = sortClause.getJsonObject("fields");
			objectFields = getObjectFields(clauseFields, fieldObject);
			fields = getStringFields(clauseFields, fieldObject);
		}

		ac.addSortClause(entity, sortType, fields, objectFields);
	}

	private void addJsonJoinClauseToQuery(SubQuery sq, JsonObject joinClause)
			throws QueryException, ActionException, JoinException {
		String path = null;

		if (joinClause.containsKey("field")) {
			path = joinClause.getJsonObject("field").getString("pui");
		}

		Entity entity = null;
		Resource resource = null;
		if (path != null && !path.isEmpty()) {
			path = "/" + path;
			path = path.substring(1);
			resource = rc.getResource(path.split("/")[1]);
			if (resource == null) {
				throw new QueryException("Invalid Resource");
			}
			entity = new Entity(path);
		}

		String joinName = joinClause.getString("joinType");
		Map<String, Field> clauseFields = new HashMap<String, Field>();
		Map<String, Object> objectFields = new HashMap<String, Object>();
		Map<String, String> fields = new HashMap<String, String>();

		if ((resource == null) || (entity == null)) {
			// Inter resource joins
			IRCTJoin irctJoin = jc.getIRCTJoin(joinName);

			if (irctJoin == null) {
				throw new JoinException("Unknown join type");
			}

			for (Field field : irctJoin.getFields()) {
				clauseFields.put(field.getPath(), field);
			}

			if (joinClause.containsKey("fields")) {
				JsonObject fieldObject = joinClause.getJsonObject("fields");
				objectFields = getObjectFields(clauseFields, fieldObject);
				fields = getStringFields(clauseFields, fieldObject);
			}

			ac.addJoinClause(entity, irctJoin, fields, objectFields);

		} else {
			// Resource join
			JoinType jointType = resource.getSupportedJoinByName(joinName);

			if (jointType == null) {
				throw new QueryException("Unsupported Join Type");
			}

			for (Field field : jointType.getFields()) {
				clauseFields.put(field.getPath(), field);
			}

			if (joinClause.containsKey("fields")) {
				JsonObject fieldObject = joinClause.getJsonObject("fields");
				objectFields = getObjectFields(clauseFields, fieldObject);
				fields = getStringFields(clauseFields, fieldObject);
			}

			ac.addJoinClause(entity, jointType, fields, objectFields);
		}

	}

	private Map<String, Object> getObjectFields(
			Map<String, Field> clauseFields, JsonObject fieldObject)
			throws QueryException {
		Map<String, Object> objectFields = new HashMap<String, Object>();
		for (String key : fieldObject.keySet()) {
			ValueType vt = fieldObject.get(key).getValueType();

			if ((vt == ValueType.ARRAY)) {
				if (clauseFields.containsKey(key)
						&& (clauseFields.get(key).getDataTypes()
								.contains(PrimitiveDataType.ARRAY))) {

					JsonArray array = fieldObject.getJsonArray(key);
					String[] stringArray = new String[array.size()];
					for (int sa_i = 0; sa_i < array.size(); sa_i++) {
						stringArray[sa_i] = array.getString(sa_i);
					}
					objectFields.put(key, stringArray);
				} else {
					throw new QueryException(key
							+ " field does not support arrays.");
				}

			} else if (vt == ValueType.OBJECT) {
				if (clauseFields.containsKey(key)
						&& (clauseFields.get(key).getDataTypes()
								.contains(PrimitiveDataType.SUBQUERY))) {

					// objectFields.put(
					// key,
					// convertJsonToQuery(qc.createSubQuery(),
					// fieldObject.getJsonObject(key)));

				} else {
					throw new QueryException(key
							+ " field does not support subqueries.");
				}
			}
		}

		return objectFields;
	}

	private Map<String, String> getStringFields(
			Map<String, Field> clauseFields, JsonObject fieldObject) {
		Map<String, String> fields = new HashMap<String, String>();
		for (String key : fieldObject.keySet()) {
			ValueType vt = fieldObject.get(key).getValueType();
			if ((vt != ValueType.ARRAY) && (vt != ValueType.OBJECT)) {
				fields.put(key, fieldObject.getString(key));
			}
		}

		return fields;
	}

}

package fr.gumtree.treediff.jdt;

import java.io.File;
import java.io.IOException;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.TreeDelete;
import com.github.gumtreediff.actions.model.TreeInsert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * @author Matias Martinez
 *
 */
public class TreeDiffFormatBuilder {

	boolean storeTrees = false;
	boolean storeMappings = false;

	public TreeDiffFormatBuilder() {
		this(false, false);
	}

	/**
	 * 
	 * @param storeTrees    indicates if it stores the complete Trees
	 * @param storeMappings indicates if it stores the mappings
	 */
	public TreeDiffFormatBuilder(boolean storeTrees, boolean storeMappings) {
		super();
		this.storeTrees = storeTrees;
		this.storeMappings = storeMappings;
	}

	/**
	 * It creates the representation only using the Diff information.
	 * 
	 * @param diff
	 * @return
	 */
	public JsonElement build(Diff diff) {

		return build(buildTree(diff.src.getRoot(), "unknowpath"), buildTree(diff.dst.getRoot(), "unknowpath"), diff,
				null);

	}

	public JsonElement build(File fileLeft, File fileRight, String algorithmName, String algorithmVersion)
			throws IOException {

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().file(fileLeft);
		Tree left = ctxL.getRoot();

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().file(fileRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new SimplifiedChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		return build(diff, left, right, fileLeft, fileRight, algorithmName, algorithmVersion,
				matcher.getClass().getName(), generator.getClass().getName());

	}

	public JsonElement build(Diff diff, Tree left, Tree right, File fileLeft, File fileRight, String algorithmName,
			String algorithmVersion, String matcherName, String editScriptGenerator) throws IOException {

		JsonObject toolInfo = new JsonObject();
		toolInfo.addProperty("name", algorithmName);
		toolInfo.addProperty("version", algorithmVersion);
		toolInfo.addProperty("matcher", matcherName);

		toolInfo.addProperty("editscriptgenerator", editScriptGenerator);

		JsonElement jsonLeft = (storeTrees) ? buildTree(left, fileLeft.getAbsolutePath()) : new JsonObject();
		JsonElement jsonRight = (storeTrees) ? buildTree(right, fileRight.getAbsolutePath()) : new JsonObject();
		return build(jsonLeft, jsonRight, diff, toolInfo);
	}

	/**
	 * It build the JSon of each Tree and then it calls to build to compute the
	 * representation of the edit script
	 * 
	 * @param treeLeft
	 * @param pathFileLeft
	 * @param treeRight
	 * @param pathFileRight
	 * @param diff
	 * @param toolInfo
	 * @param mappingInfo
	 * @return
	 */
	public JsonElement build(Tree treeLeft, String pathFileLeft, Tree treeRight, String pathFileRight, Diff diff,
			JsonElement toolInfo) {

		return build(buildTree(treeLeft, pathFileLeft), buildTree(treeRight, pathFileRight), diff, toolInfo);
	}

	/**
	 * Creates the TreeDiff from the data received as parameter
	 * 
	 * @param fileBefore  the json representation of the left tree
	 * @param fileAfter   the json representation of the right tree
	 * @param diff        the AST diff produced by GumTree
	 * @param toolInfo    the information of the Diff Algorithm used
	 * @param mappingInfo the Json representation of the Mappings
	 * @return
	 */
	public JsonElement build(JsonElement fileBefore, JsonElement fileAfter, Diff diff, JsonElement toolInfo) {

		JsonObject root = new JsonObject();

		root.add("tool-info", toolInfo);

		JsonArray diffs = buildEditScript(diff);
		root.add("diff", diffs);

		root.add("before-file", fileBefore);
		root.add("after-file", fileAfter);

		JsonElement mappingInfo = (storeMappings) ? createMappingJson(diff.mappings) : new JsonObject();
		root.add("mapping", mappingInfo);

		return root;
	}

	public JsonArray buildEditScript(Diff diff) throws IllegalAccessError {
		JsonArray diffs = new JsonArray();

		for (Action iAction : diff.editScript.asList()) {

			JsonElement elementToAdd = null;
			if (iAction instanceof Insert) {
				elementToAdd = createInsert(diff, iAction, "insert-node");
			} else if (iAction instanceof TreeInsert) {
				elementToAdd = createInsert(diff, iAction, "insert-subtree");
			} else if (iAction instanceof Delete) {
				elementToAdd = createDelete(diff, iAction, "delete-node");
			} else if (iAction instanceof TreeDelete) {
				elementToAdd = createDelete(diff, iAction, "delete-subtree");
			} else if (iAction instanceof Move) {
				elementToAdd = createMove(diff, iAction);
			} else if (iAction instanceof Update) {
				elementToAdd = createUpdate(diff, iAction);
			} else {
				throw new IllegalAccessError("Action not recognized: " + iAction.getClass().getCanonicalName());
			}
			if (elementToAdd != null) {
				diffs.add(elementToAdd);
			}
		}
		return diffs;
	}

	public JsonElement buildTree(Tree tree, String path) {

		JsonObject fileJSon = new JsonObject();
		fileJSon.addProperty("path", path);
		fileJSon.add("ast", convertTreeToJSon(tree));
		return fileJSon;
	}

	/**
	 * Creates the JSON representation from a Mapping
	 * 
	 * @param mappings a mapping generated by a diff
	 * @return the json representation of the mapping
	 */
	public JsonElement createMappingJson(MappingStore mappings) {
		JsonArray mappingsJson = new JsonArray();
		for (Mapping oneMap : mappings.asSet()) {

			JsonObject mapJson = new JsonObject();

			mapJson.add("src", createNodeJsonFromTree(oneMap.first));
			mapJson.add("dst", createNodeJsonFromTree(oneMap.second));
			mappingsJson.add(mapJson);

		}

		return mappingsJson;
	}

	private JsonElement createDelete(Diff diff, Action iAction, String type) {
		JsonObject deletedNodeJson = new JsonObject();
		deletedNodeJson.addProperty("type", type);
		JsonElement convertTreeToJSon = (storeTrees) ? convertTreeToJSon(iAction.getNode())
				: convertTreeToJsonSingleNode(iAction.getNode());
		deletedNodeJson.add("node", convertTreeToJSon);

		deletedNodeJson.addProperty("location-before-char-start", iAction.getNode().getPos());
		deletedNodeJson.addProperty("location-before-char-end", iAction.getNode().getEndPos());

		deletedNodeJson.addProperty("node-str", iAction.getNode().getLabel());
		deletedNodeJson.add("meta", null);

		return deletedNodeJson;
	}

	private JsonElement createInsert(Diff diff, Action iAction, String type) {
		JsonObject insertNodeJson = new JsonObject();
		insertNodeJson.addProperty("type", type);

		JsonElement convertTreeToJSon = (storeTrees) ? convertTreeToJSon(iAction.getNode())
				: convertTreeToJsonSingleNode(iAction.getNode());
		insertNodeJson.add("node", convertTreeToJSon);

		insertNodeJson.addProperty("location-after-char-start", iAction.getNode().getPos());
		insertNodeJson.addProperty("location-after-char-end", iAction.getNode().getEndPos());

		insertNodeJson.addProperty("node-str", iAction.getNode().getLabel());
		insertNodeJson.add("meta", null);

		return insertNodeJson;
	}

	/**
	 * Returns a Json object with the information of the node received by parameter.
	 * 
	 * @param nodeT
	 * @return
	 */
	protected JsonObject createNodeJsonFromTree(Tree nodeT) {
		JsonObject node = new JsonObject();

		node.addProperty("node-str", nodeT.getLabel());
		node.addProperty("start", nodeT.getPos());
		node.addProperty("end", nodeT.getEndPos());
		return node;
	}

	private JsonElement createMove(Diff diff, Action iAction) {
		String type = "move-subtree";
		JsonObject movedNodeJson = createMappedNode(diff, iAction, type);

		return movedNodeJson;
	}

	private JsonElement createUpdate(Diff diff, Action iAction) {
		String type = "update-node";
		JsonObject updatedNodeJson = createMappedNode(diff, iAction, type);

		return updatedNodeJson;
	}

	/**
	 * This method creates a node for an AST element than is mapped in the Matching
	 * (updated or moved node)
	 * 
	 * @param diff
	 * @param iAction
	 * @param type
	 * @param insertNodeJson
	 * @return
	 */
	public JsonObject createMappedNode(Diff diff, Action iAction, String type) {

		JsonObject mappedNodeJson = new JsonObject();

		mappedNodeJson.addProperty("type", type);
		JsonElement convertTreeToJSon = (storeTrees) ? convertTreeToJSon(iAction.getNode())
				: convertTreeToJsonSingleNode(iAction.getNode());
		mappedNodeJson.add("node", convertTreeToJSon);

		mappedNodeJson.addProperty("location-before-char-start", iAction.getNode().getPos());
		mappedNodeJson.addProperty("location-before-char-end", iAction.getNode().getEndPos());

		Tree dstMoved = diff.mappings.getDstForSrc(iAction.getNode());
		mappedNodeJson.addProperty("location-after-char-start", dstMoved.getPos());
		mappedNodeJson.addProperty("location-after-char-end", dstMoved.getEndPos());

		mappedNodeJson.addProperty("node-str", dstMoved.getLabel()); // optional
		mappedNodeJson.add("meta", null);

		return mappedNodeJson;
	}

	public JsonObject fromSimpleAction(String type, int startLeft, int endLeft, int startRight, int endRight,
			String label) {
		JsonObject actionJson = new JsonObject();

		actionJson.addProperty("type", type);

		actionJson.addProperty("location-before-char-start", startLeft);
		actionJson.addProperty("location-before-char-end", endLeft);

		actionJson.addProperty("location-after-char-start", startRight);
		actionJson.addProperty("location-after-char-end", endRight);

		actionJson.addProperty("node-str", label);

		return actionJson;
	}

	/**
	 * Creates a Json from a Tree. It includes the children.
	 * 
	 * @param nodeTree the tree to represent
	 * @return the json representation
	 */
	protected JsonElement convertTreeToJSon(Tree nodeTree) {

		JsonObject nodeJson = convertTreeToJsonSingleNode(nodeTree);
		JsonArray children = new JsonArray();
		nodeJson.add("children", children);

		for (Tree child : nodeTree.getChildren()) {
			children.add(convertTreeToJSon(child));

		}
		return nodeJson;
	}

	/**
	 * Creates a Json from a Tree. it does not include children.
	 * 
	 * @param nodeTree
	 * @return
	 */
	public JsonObject convertTreeToJsonSingleNode(Tree nodeTree) {
		JsonObject nodeJson = new JsonObject();
		nodeJson.addProperty("label", nodeTree.getLabel());
		nodeJson.addProperty("type", nodeTree.getType().name);
		return nodeJson;
	}

}

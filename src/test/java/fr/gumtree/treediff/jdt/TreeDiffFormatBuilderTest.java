package fr.gumtree.treediff.jdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
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
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TreeDiffFormatBuilderTest {

	@Test
	public void testInsertNode() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b();\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.b(1);\n" + "    }\n"
				+ "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new ChawatheScriptGenerator(); // No trees
		EditScript computeActions = generator.computeActions(mappings);

		// assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		List<Action> actions = diff.editScript.asList();
		assertTrue(actions.size() > 0);

		System.out.println(actions);
		// assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof Insert).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();
		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("insert-node")
					&& action.getAsJsonObject().get("location-after-char-start").getAsInt() == 56
					&& action.getAsJsonObject().get("location-after-char-end").getAsInt() == 57) {
				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testInsert_Tree() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b();\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.b(a + 2);\n" + "    }\n"
				+ "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new SimplifiedChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		List<Action> actions = diff.editScript.asList();
		assertTrue(actions.size() > 0);

		System.out.println(actions);
		assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof TreeInsert).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();

		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("insert-subtree")
					&& action.getAsJsonObject().get("location-after-char-start").getAsInt() == 56
					&& action.getAsJsonObject().get("location-after-char-end").getAsInt() == 61) {

				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testDeleteNode() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b();\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        \n" + "    }\n" + "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new ChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		// assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		List<Action> actions = diff.editScript.asList();
		assertTrue(actions.size() > 0);

		System.out.println(actions);
		// assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof Delete).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();
		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("delete-node")
					&& action.getAsJsonObject().get("location-before-char-start").getAsInt() == 52
					&& action.getAsJsonObject().get("location-before-char-end").getAsInt() == 58) {
				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testTreeDelete() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b(a+2);\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.b(); \n" + "    }\n"
				+ "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new SimplifiedChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		List<Action> actions = diff.editScript.asList();
		assertTrue(actions.size() > 0);

		System.out.println(actions);
		assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof TreeDelete).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();
		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("delete-subtree")
					&& action.getAsJsonObject().get("location-before-char-start").getAsInt() == 56
					&& action.getAsJsonObject().get("location-before-char-end").getAsInt() == 59) {
				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testTreeMove() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b();  a.c();  a.d();\n"
				+ "    }\n" + "    public static void foo1() {\n" + "    a.c();}\n" + "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.c();  a.d(); \n"
				+ "    }\n"

				+ "    public static void foo1() {\n" + "     a.c(); a.b();}\n" + "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new SimplifiedChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);
		List<Action> actions = computeActions.asList();
		System.out.println(actions);
		assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		assertTrue(actions.size() > 0);

		assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof Move).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();
		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("move-subtree")
					&& action.getAsJsonObject().get("location-before-char-start").getAsInt() == 52
					&& action.getAsJsonObject().get("location-before-char-end").getAsInt() == 58
					&& action.getAsJsonObject().get("location-after-char-start").getAsInt() == 118
					&& action.getAsJsonObject().get("location-after-char-end").getAsInt() == 124) {
				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testUpdateNode() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b(222333);\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.b(10);\n" + "    }\n"
				+ "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new ChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		List<Action> actions = diff.editScript.asList();
		assertTrue(actions.size() > 0);

		System.out.println(actions);
		assertEquals(1, actions.size());

		assertTrue(actions.stream().filter(e -> e instanceof Update).findAny().isPresent());

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();
		JsonElement outJson = builder.build(diff);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

		JsonArray diffs = outJson.getAsJsonObject().get("diff").getAsJsonArray();
		JsonElement elementFound = null;
		for (JsonElement action : diffs) {
			if (action.getAsJsonObject().get("type").getAsString().equals("update-node")
					&& action.getAsJsonObject().get("location-before-char-start").getAsInt() == 56
					&& action.getAsJsonObject().get("location-before-char-end").getAsInt() == 62
					&& action.getAsJsonObject().get("location-after-char-start").getAsInt() == 56
					&& action.getAsJsonObject().get("location-after-char-end").getAsInt() == 58) {
				elementFound = action;
				break;
			}

		}
		assertNotNull(elementFound);

	}

	@Test
	public void testTreeNode() throws IOException {
		String inputLeft = "class Main {\n" + "    public static void foo() {\n" + "        a.b(222333);\n" + "    }\n"
				+ "}\n";
		System.out.println(inputLeft);

		TreeContext ctxL = new JdtTreeGenerator().generateFrom().string(inputLeft);
		Tree left = ctxL.getRoot();
		String inputRight = "class Main {\n" + "    public static void foo() {\n" + "        a.b(10);\n" + "    }\n"
				+ "}";
		System.out.println(inputRight);

		TreeContext ctxR = new JdtTreeGenerator().generateFrom().string(inputRight);
		Tree right = ctxR.getRoot();

		Matcher matcher = new CompositeMatchers.SimpleGumtree();
		MappingStore mappings = matcher.match(left, right);

		EditScriptGenerator generator = new ChawatheScriptGenerator();
		EditScript computeActions = generator.computeActions(mappings);

		assertEquals(1, computeActions.size());

		Diff diff = new Diff(ctxL, ctxR, mappings, computeActions);

		TreeDiffFormatBuilder builder = new TreeDiffFormatBuilder();

		JsonObject toolInfoJson = new JsonObject();
		toolInfoJson.addProperty("tool", "GumTree");

		JsonElement outJson = builder.build(left, "./File1.java", right, "./File2.java", diff, toolInfoJson);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(outJson);

		System.out.println(json);

	}

}

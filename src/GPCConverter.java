
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JOptionPane;

import java.util.HashMap;

public class GPCConverter {

	private static List<String> definedList;
	private static List<String> varList;
	private static List<String> varArrayList;
	private static List<String> mappingCode;
	private static List<String> initCode;
	private static List<String> mainCode;
	private static List<String> comboList;
	private static List<String> functionList;
	private static String dataSegment;
	
	public static String fortmatGPCCode(String s) {
		int braceCount = 0;
		String operatorPattern = "(\\+|\\-|\\*|\\/|\\%|\\&|\\||\\=|\\!|\\^|\\>|\\<)";
		String singleOperatorPattern = "(\\+|\\-|\\*|\\/|\\%|\\&|\\||\\=|\\>|\\<)";
		String newStr = "";
		s = s.replaceAll("\r\n\\s*\r\n\\}", "\r\n\\}"); // replace blank line followed by }
		s = s.replaceAll("\\s*\\}", "\r\n}");
		s = s.replaceAll("\\s*\\{", " {"); // set all { 1 from previous
		s = s.replaceAll(",", ", "); // put space after commas
		s = s.replaceAll("\\)\\s*\\{", ") {");
		s = s.replaceAll("\\belse\\b\\s*\\bif\\b", "else if"); //replace All else\r\nif with else if\r\n
		s = s.replaceAll("\\s*" + singleOperatorPattern + "\\s*", " $1 ");
		s = s.replaceAll("\\s*" + operatorPattern + "\\s*" + operatorPattern + "\\s*" , " $1$2 ");
		s = s.replaceAll("\\/\\*", "\r\n\r\n\\/\\*"); // comments
		s = s.replaceAll("\\*\\/", "\r\n\\*\\/\r\n\r\n"); // comments
		s = s.replaceAll("(\\D)[0]+([\\d]+)", "$1$2");
		for(int i = 0; i < s.length(); i++) {
			char currChar = s.charAt(i);
			if(currChar == '{') braceCount++;
			else if(currChar == '}') {
				if(newStr.charAt(newStr.length()-1) == '\t') 
					newStr = newStr.substring(0 , newStr.length()-1);
				braceCount--;
			}
			
			newStr += ("" + currChar);
			
			if(currChar == '\n') {
				for(int k = 0; k < braceCount; k++) {
					newStr += '\t';
				}
			}
		}
		return newStr;
	}
	
	private static void staticCopyTempGPC(GPC gpc) {
		definedList = gpc.getDefines();
		varList = gpc.getVars();
		varArrayList = gpc.getVarArrays();
		mappingCode = gpc.getMappings();
		initCode = gpc.getInitCode();
		mainCode = gpc.getMainCode();
		comboList = gpc.getCombos();
		functionList = gpc.getFunctions();
		dataSegment = gpc.getDataSegment();
	}
	
	public static void fixGPCErrors(GPC gpc) {
		staticCopyTempGPC(gpc);
		
		replaceKeywords();
		replaceDecimalNumbers();
		replaceFunctionNames();
		replaceComboNames();
		removeUnusedFunctions();
		replaceArrayTricks();
		flattenMutlidimArrays();
		fixInvalidDataDeclaration();
		fixSemicolons();
		
		gpc.setVarArrays(varArrayList);
		gpc.setMappings(mappingCode);
		gpc.setInitCode(initCode);
		gpc.setMainCode(mainCode);
		gpc.setCombos(comboList);
		gpc.setFunctions(functionList);
		gpc.setDataSegment(dataSegment);
	}
	
	public static void convertRemapsToGPC2(GPC gpc) {
		staticCopyTempGPC(gpc);
		
		if(mappingCode.size() == 0) return;
		
		String newCode = "main {//remapping code \r\n";
		for(int i = 0; i < mappingCode.size(); i++) {
			String newMapping = mappingCode.get(i).replaceAll("\\bunmap\\b\\s*\\b(\\w*)\\b", "set_val($1,0)");
				newMapping = newMapping.replaceAll("\\bremap\\b\\s*\\b(\\w*)\\b\\s*->\\s*\\b(\\w*)\\b", "set_val($2,get_val($1))");
			
			newCode += newMapping + (newMapping.endsWith(";") ? "" : ";") + "\r\n";
			mappingCode.set(i, ("//" + mappingCode.get(i)));
		}
		newCode += "}";
		functionList.add(newCode); // must be added to the end of the GPC
		
		gpc.setMappings(mappingCode);
		gpc.setFunctions(functionList);
	}
	
	private static void replaceDecimalNumbers() {
		replaceAllInGPC("(\\d+)\\.\\d+", "$1"); // cut decimal part off
		replaceAllInGPC("([^\\d])\\.\\d+", "$1 0"); // replace decimal only numbers with 0
	}
	
	private static void replaceKeywords() {
		List a[] = { definedList, mappingCode, varList, varArrayList, initCode, mainCode, comboList, functionList };
		List<String> keywords = getKeywords();
		dataSegment = replaceKeywords(dataSegment, keywords);
		for(int i = 0; i < a.length; i++) {
			for(int k = 0; k < a[i].size(); k++) a[i].set(k, replaceKeywords((String)a[i].get(k), keywords));
		}
	}
	
	private static String replaceKeywords(String s, List<String> keywords) {
		for(String str : keywords) s = s.replaceAll("\\b" + str + "\\b", "t1_" + str);
		
		s = s.replaceAll("\\bXB1_PR1\\b", "XB1_P1");
		s = s.replaceAll("\\bXB1_PR2\\b", "XB1_P2");
		s = s.replaceAll("\\bXB1_PL1\\b", "XB1_P3");
		s = s.replaceAll("\\bXB1_PL2\\b", "XB1_P4");
		return s;
	}
	
	private static void replaceComboNames() {
		if(comboList.isEmpty()) return;
		List<String> comboNames = getComboNames();
		String pattern = "(combo_run|combo_running|combo_stop|combo_restart|combo_suspend|combo_suspended|call)\\s*\\(\\s*";
		for(String comboName : comboNames) {
			String cPattern = pattern + comboName + "\\s*\\)";
			String replacePattern = " $1\\(c_" + comboName + "\\)";
			replaceAllInCodeSegments(cPattern, replacePattern);
			replaceAllInList(comboList,"\\s*combo\\s*" + comboName + "\\s*\\{", "combo c_" + comboName + "\\{");
		}
	}
	
	private static void replaceFunctionNames() {
		List<String> functionNames = getFunctionNames();
		for(String functionName : functionNames) {
			String pattern = "\\b" + functionName + "\\b\\s*\\(";
			String replacePattern = " f_" + functionName + "\\(";
			replaceAllInCodeSegments(pattern, replacePattern);
			replaceAllInList(functionList, "\\bint\\b", "");	
		}
	}
	
	private static void flattenMutlidimArrays() {
		for(int i = 0; i < varArrayList.size(); i++) {
			String currArray = varArrayList.get(i);
			if(currArray.matches("(?s)\\s*const\\s+int8\\s+\\w+\\s*\\[.*\\]\\s*\\[.*\\].*")) {
				String arrayName = currArray.replaceAll("(?s)\\s*const\\s+int8\\s+(\\w+)\\s*\\[.*\\]\\s*\\[.*\\].*", "$1");
				String tmp = currArray.substring(0, currArray.indexOf("}"));
				int secondDim = (tmp.length() - tmp.replace(",", "").length()) + 1;				
				//remove extra { } and [][]
				currArray = currArray.replaceAll("\\s*\\[\\s*\\]\\s*\\[\\s*\\]", "[]");
				currArray = currArray.replaceAll("\\{|\\}", "").replaceAll("=", "= {").replaceAll(";", " };");
				varArrayList.set(i, currArray);
				
				String pattern = arrayName + "\\s*\\[(.*?)\\]\\s*\\[(.*?)\\]";
				String replacePattern = arrayName + "[(" + secondDim + " \\* ($1)) + ($2)]";
				replaceAllInCodeSegments(pattern, replacePattern);
			}
		}
	}
	
	private static void fixInvalidDataDeclaration() {
		replaceAllInList(varList, "\\b(\\w*)\\s*\\[\\s*0\\s*]", "$1[1]");
		replaceAllInList(varArrayList, "\\b(\\w*)\\s*\\[\\s*0\\s*]", "$1[1]");
	}
	
	private static void replaceArrayTricks() {
		List<String> varNames = getVariableNames();
		for(String s : varNames) {
			replaceAllInCodeSegments("\\b" + s + "\\b\\s*\\[(.*?)\\]", s+"\\[\\-\\($1\\)\\]");
		}
	}
	
	private static void removeUnusedFunctions() { 
		List<String> functionNames = getFunctionNames();
		List<String> comboNames = getComboNames();
		Map<String, Boolean> usedFunctions = new HashMap<String, Boolean>();
		Map<String, Boolean> usedCombos = new HashMap<String, Boolean>();
		
		boolean change = true;
		// check init and main for used combos/functions
		for(int i = 0; i < functionNames.size(); i++) {
			boolean used = false;
			String pattern = "(?s).*\\b" + functionNames.get(i) + "\\b.*";
			for(int k = 0; k < initCode.size() && !used; k++) used = initCode.get(k).matches(pattern);
			for(int k = 0; k < mainCode.size() && !used; k++) used = mainCode.get(k).matches(pattern);
			usedFunctions.put(functionNames.get(i), used);
		}
		for(int i = 0; i < comboNames.size(); i++) {
			boolean used = false;
			String pattern = "(?s).*\\b(combo_run|call|combo_restart)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b.*";
			for(int k = 0; k < initCode.size() && !used; k++) used = initCode.get(k).matches(pattern);
			for(int k = 0; k < mainCode.size() && !used; k++) used = mainCode.get(k).matches(pattern);
			usedCombos.put(comboNames.get(i), used);
		}
		
		while(change) {
			change = false;
			for(int i = 0; i < usedFunctions.size(); i++) {
				if(usedFunctions.get(functionNames.get(i))) continue;
				String pattern = "(?s).*\\b" + functionNames.get(i) + "\\b.*";
				boolean used = false;
				for(int k = 0; k < comboList.size() && !used; k++) {
					if(usedCombos.get(comboNames.get(k))) used = comboList.get(k).matches(pattern);
				}
				for(int k = 0; k < functionList.size() && !used; k++) {
					if(usedFunctions.get(functionNames.get(k))) used = functionList.get(k).matches(pattern);
				}
				if(used) {
					usedFunctions.put(functionNames.get(i), true);
					change = true;
				}
			}
			
			for(int i = 0; i < usedCombos.size(); i++) {
				if(usedCombos.get(comboNames.get(i))) continue;
				String pattern = "(?s).*\\b(combo_run|call|combo_restart)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b.*";
				boolean used = false;
				for(int k = 0; k < comboList.size() && !used; k++) {
					if(usedCombos.get(comboNames.get(k))) used = comboList.get(k).matches(pattern);
				}
				for(int k = 0; k < functionList.size() && !used; k++) {
					if(usedFunctions.get(functionNames.get(k))) used = functionList.get(k).matches(pattern);
				}
				if(used) {
					usedCombos.put(comboNames.get(i), true);
					change = true;
				}
			}
		}
		
		for(int i = 0; i < functionNames.size(); i++) {
			if(!usedFunctions.get(functionNames.get(i))) functionList.set(i, "/*" + functionList.get(i) + "*/");
		}
		for(int i = 0; i < comboNames.size(); i++) {
			if(!usedCombos.get(comboNames.get(i))) {
				String pattern = "\\b(combo_running|combo_suspended)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b\\s*\\)";
				String pattern2 = "\\b(combo_suspend|combo_stop)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b\\s*\\)\\s*(;|\\s*)";
				replaceAllInCodeSegments(pattern, "FALSE");
				replaceAllInCodeSegments(pattern2, "");
				comboList.set(i, "/*" + comboList.get(i) + "*/");
			}
		}
	}
	
	private static void fixSemicolons() {
		for(int i = 0; i < initCode.size(); i++) initCode.set(i, fixSemicolons(initCode.get(i)));
		for(int i = 0; i < mainCode.size(); i++) mainCode.set(i, fixSemicolons(mainCode.get(i)));
		for(int i = 0; i < comboList.size(); i++) comboList.set(i, fixSemicolons(comboList.get(i)));
		for(int i = 0; i < functionList.size(); i++) functionList.set(i, fixSemicolons(functionList.get(i)));
	}
	
	private static String fixSemicolons(String s) {
		// splits by ( ) { } ; , and var start/end
		List<String> tmp = new ArrayList<String>(Arrays.asList(s.split("\\b|(?=\\))|(?=\\()|(?<=\\))|(?<=\\()|(?=\\;)|(?<=\\;)|(?=\\})|(?<=\\})|(?=\\{)|(?<=\\{)|(?=\\,)|(?<=\\,)")));
		for(int i = tmp.size() - 1; i >= 0; i--) {
			tmp.set(i, tmp.get(i).trim().replaceAll("\\s", ""));
			if(tmp.get(i).isEmpty()) {
				tmp.remove(i);
			}
		}
		return fixSemicolons(tmp);
		//return fixSemicolons(tmp, false, 0, false);
	}
	
	private static String fixSemicolons(List<String> codeBlock) { // ADD CODE TO REMOVE RANDOM TOKENS
		String newStr = "";
		String prevVarName = "";
		boolean prevVar = false, oneLineIf = false;
		int parenCount = 0;
		
		while(!codeBlock.isEmpty()) {
			String fixedStr = codeBlock.get(0);
			if(fixedStr.isEmpty()) { } // Empty line
			else if(fixedStr.matches("(main|function|combo|init)")) { // matches start of codeblock
				fixedStr = fixedStr + " ";
				while(!codeBlock.get(1).equals("{")) {
					fixedStr += codeBlock.get(1) + "";
					codeBlock.remove(1);
				}
				fixedStr += codeBlock.get(1) + "\r\n";
				codeBlock.remove(1);
			}
			else if(fixedStr.matches("\\(")) { // opening paren
				if(parenCount <= 0) parenCount = 0;
				parenCount++;
				prevVar = false;
			}
			else if(fixedStr.matches("\\)")) { // closing paren
				parenCount--;
				prevVar = true;
				if(parenCount == 0) {
					parenCount = -1; // paren just ended
				}
			}
			else if(fixedStr.matches((".*(\\[|\\]).*"))) { // array square brackets
				prevVar = false;
			}
			else if(fixedStr.matches("\\;")) { // semicolon - line end
				if(parenCount <= 0)  { // for loop conditions
					fixedStr +=  "\r\n";
					oneLineIf = false;
				}
				else fixedStr += " ";
				prevVar = false;
			}
			else if(fixedStr.matches(("return"))) { // return statement
				fixedStr += (codeBlock.get(1).trim().replaceAll("\\s", "").matches("\\}") ? ";\r\n" : " ");
				prevVar = false;
				if(oneLineIf) {
					fixedStr = "\r\n\t" + fixedStr;
					oneLineIf = false;
				}
			}
			else if(fixedStr.matches("(for|while|if|else)")) { // containing conditional statements
				if(oneLineIf) fixedStr = " " + fixedStr;
				else if(prevVar) fixedStr = ";\r\n" + fixedStr;
				fixedStr += " ";
				oneLineIf = true;
			}
			else if(fixedStr.matches("\\!")) {
				if(prevVar && parenCount > 0) { // fix stray ! in parentheses
					fixedStr = "";
				}
				
				if(prevVar && parenCount != -1) fixedStr = ";\r\n" + fixedStr; 
				else prevVar = false;
			}
			else if(fixedStr.matches("(\\{|\\})")) { // { } !
				if(fixedStr.equals("{")) {
					if(!oneLineIf) { // remove random { }
						fixedStr = "";
						int i = 1;
						int braceCount = 0;
						while(!codeBlock.get(i).equals("}") || braceCount > 0) { // extra { need to be accounted for while searching for the matching }
							if(codeBlock.get(i).equals("{")) braceCount++;
							else if(codeBlock.get(i).equals("}")) braceCount--;
							i++;
						}
						codeBlock.remove(i);
					}
					
					else {
						fixedStr += "\r\n";
					}
					oneLineIf = false;
				}
				if(fixedStr.equals("}")) {
					fixedStr = (prevVar ? ";" : "") + "\r\n" + fixedStr + "\r\n";
				}
				else if(prevVar && parenCount != -1) fixedStr = ";\r\n" + fixedStr; 
				prevVar = false;
			}
			else if(fixedStr.matches(".*(\\,|\\+|\\-|\\*|\\/|\\%|\\&|\\||\\=|\\!|\\^|\\>|\\<).*")) { // operators
				if(fixedStr.length() > 1) {
					codeBlock.add(1, fixedStr.substring(1));
					fixedStr = "" + fixedStr.charAt(0);
				}
				if(prevVar && oneLineIf && fixedStr.equals("=")) {
					if(codeBlock.get(1).charAt(0) != '=') {
						fixedStr = "==";
					}
				}
				prevVar = false;
			}
			else { // identifiers
				if(oneLineIf && parenCount <= 0) {
					fixedStr = " " + fixedStr;
					oneLineIf = false;
				}
				else if(prevVar) {
					if(parenCount > 0) fixedStr = ""; // test - remove stray code
					if(fixedStr.matches("\\W")) {
						System.out.println(fixedStr);
						fixedStr = ";\r\n";
					}
					if(!fixedStr.matches("^[0-9]*$"))
						fixedStr = ";\r\n" + fixedStr;
					else fixedStr = ""; // remove random numbers
					
					if(!fixedStr.replaceAll("\\W", "").isEmpty()) // new previous var
						prevVarName = fixedStr.replaceAll("\\W", "");
				}
				
				prevVar = true;
			}
			codeBlock.remove(0); // pop first off
			newStr += fixedStr;
		}
		return newStr;
	}
	
	private static List<String> getVariableNames(){
		List<String> varNames = new ArrayList<String>();
		for(int i = 0; i < varList.size(); i++) {
			String varBlock = varList.get(i).trim().replaceAll(",|;", " ");
			varBlock = varBlock.replaceAll("\\s*\\[\\s*", "\\["); // replace all brackets
			varBlock = varBlock.replaceAll("\\s*\\]\\s*", "\\] ");
			varBlock = varBlock.replaceAll("=\\s*", " =");
			String[] varBlockSplit = varBlock.replaceAll("\\s+", " ").split(" ");
			for(int k = 0; k < varBlockSplit.length; k++) {
				if(varBlockSplit[k].equals("int") || varBlockSplit[k].contains("[") || varBlockSplit[k].contains("=")) continue;
				varNames.add(varBlockSplit[k]);
			}
		}
		
		return varNames;
	}
	
	private static List<String> getComboNames() {
		List<String> comboNames = new ArrayList<String>();
		for(String combo : comboList) {
			int lastIdx = combo.indexOf("{") != -1 ? combo.indexOf("{") : combo.length();
			String comboName = combo.substring(combo.indexOf("combo ") + 6, lastIdx).trim();
			comboNames.add(comboName);
		}
		return comboNames;
	}
	
	private static List<String> getFunctionNames() {
		List<String> functionNames = new ArrayList<String>();
		for(String function : functionList) {
			int lastIdx = function.indexOf("(") != -1 ? function.indexOf("(") : function.length();
			String funcName = function.substring(function.indexOf("function ") + 9, lastIdx).trim();
			functionNames.add(funcName);
		}
		return functionNames;
	}
	
	private static void replaceAllInList(List<String> s, String pattern, String replacePattern) {
		for(int i = 0; i < s.size(); i++) s.set(i, s.get(i).replaceAll(pattern, replacePattern));
	}
	
	private static void replaceAllInCodeSegments(String pattern, String replacePattern) {
		replaceAllInList(initCode, pattern, replacePattern);
		replaceAllInList(mainCode, pattern, replacePattern);
		replaceAllInList(comboList, pattern, replacePattern);
		replaceAllInList(functionList, pattern, replacePattern);
	}
	
	private static void replaceAllInGPC(String pattern, String replacePattern) {
		replaceAllInList(definedList, pattern, replacePattern);
		dataSegment = dataSegment.replaceAll(pattern, replacePattern);
		replaceAllInList(varList, pattern, replacePattern);
		replaceAllInList(varArrayList, pattern, replacePattern);
		replaceAllInList(mappingCode, pattern, replacePattern);
		replaceAllInCodeSegments(pattern, replacePattern);
	}
	
	private static List<String> getKeywords() {
		List<String> keywords = new ArrayList<String>();
		try {
			String path = java.net.URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(), "UTF-8");
			File tmpDir = new File(path + "keywords.db");
			Scanner kwSc = new Scanner(tmpDir);
			while(kwSc.hasNextLine()) {
				String str = kwSc.nextLine().trim();
				keywords.add(str);
			}
			kwSc.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Could not open keywords.db in " + ClassLoader.getSystemClassLoader().getResource(".").getPath() + "\r\n" + e.toString());
			System.exit(0);
		}
		return keywords;
	}
	
}

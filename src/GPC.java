import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;

public class GPC {
	
	private List<String> definedList;
	private List<String> varList;
	private List<String> varArrayList;
	private List<String> mappingCode;
	private List<String> initCode;
	private List<String> mainCode;
	private List<String> comboList;
	private List<String> functionList;
	private String dataSegment;
	
	public GPC(String s) {
		GPCReader r = new GPCReader(s);
		definedList = r.getDefines();
		mappingCode = r.getMappings();
		varList = r.getVars();
		varArrayList = r.getVarArrays();
		initCode = r.getInitCode();
		mainCode = r.getMainCode();
		comboList = r.getCombos();
		functionList = r.getFunctions();
		dataSegment = r.getDataSegment();
		fixErrors();
	}
	
	private void replaceComboNames() {
		if(comboList.isEmpty()) return;
		List<String> comboNames = getComboNames();
		String pattern = "(combo_run|combo_running|combo_stop|combo_restart|combo_suspend|combo_suspended|call)\\s*\\(\\s*";
		for(int k = 0; k < comboNames.size(); k++) {
			String cPattern = pattern + comboNames.get(k) + "\\s*\\)";
			String replacePattern = " $1\\(c_" + comboNames.get(k)+ "\\)";
			replaceAllInList(initCode, cPattern, replacePattern);
			replaceAllInList(mainCode, cPattern, replacePattern);
			replaceAllInList(comboList, cPattern, replacePattern);
			replaceAllInList(comboList,"\\s*combo\\s*" + comboNames.get(k) + "\\s*\\{", "combo c_" + comboNames.get(k) + "\\{");
			replaceAllInList(functionList, cPattern, replacePattern);
		}
	}
	
	private List<String> getComboNames() {
		List<String> comboNames = new ArrayList<String>();
		if(!comboList.isEmpty()) {
			for(int i = 0; i < comboList.size(); i++) {
				String combo = comboList.get(i);
				int lastIdx = combo.indexOf("{") != -1 ? combo.indexOf("{") : combo.length();
				String comboName = combo.substring(combo.indexOf("combo ") + 6, lastIdx).trim();
				comboNames.add(comboName);
			}
		}
		return comboNames;
	}
	
	private void replaceKeywords() {
		for(int i = 0; i < initCode.size(); i++) {
			initCode.set(i, replaceKeywords(initCode.get(i)));
		}
		for(int i = 0; i < mainCode.size(); i++) {
			mainCode.set(i, replaceKeywords(mainCode.get(i)));
		}
		for(int i = 0; i < comboList.size(); i++) {
			comboList.set(i, replaceKeywords(comboList.get(i)));
		}
		for(int i = 0; i < functionList.size(); i++) {
			functionList.set(i, replaceKeywords(functionList.get(i)));
		}
	}
	
	private String replaceKeywords(String s) {
		File tmpDir = new File(System.getProperty("user.dir") + "\\keywords.db");
		if(tmpDir.exists()) {
			try {
				Scanner kwSc = new Scanner(tmpDir);
				while(kwSc.hasNextLine()) {
					String str = kwSc.nextLine().trim();
					s = s.replaceAll("\\b" + str + "\\b", "t1_" + str);
				}
				kwSc.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		s = s.replaceAll((":"), (";")); // : = ; in CM lool
		s = s.replaceAll("(\\d+)\\.\\d+", "$1"); // remove decimal from number
		s = s.replaceAll("([^\\d])\\.\\d+", "$1 0"); // replace decimal only numbers with 0
		return s;
	}
	
	private void replaceFunctionNames() {
		if(functionList.isEmpty()) return;
		List<String> functionNames = getFunctionNames();
		for(int k = 0; k < functionNames.size(); k++) {
			String pattern = "\\b" + functionNames.get(k) + "\\b\\s*\\(";
			String replacePattern = " f_" + functionNames.get(k) + "\\(";
			replaceAllInList(initCode, pattern, replacePattern);
			replaceAllInList(mainCode, pattern, replacePattern);
			replaceAllInList(comboList, pattern, replacePattern);
			replaceAllInList(functionList, pattern, replacePattern);
			replaceAllInList(functionList, "\\bint\\b", "");	
		}
	}
	
	private List<String> getFunctionNames() {
		List<String> functionNames = new ArrayList<String>();
		if(!functionList.isEmpty()) {
			for(int i = 0; i < functionList.size(); i++) {
				String function = functionList.get(i);
				int lastIdx = function.indexOf("(") != -1 ? function.indexOf("(") : function.length();
				String funcName = function.substring(function.indexOf("function ") + 9, lastIdx).trim();
				functionNames.add(funcName);
			}
		}
		return functionNames;
	}
	
	private void flattenMutlidimArrays() {
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
				replaceAllInList(initCode, pattern, replacePattern);
				replaceAllInList(mainCode, pattern, replacePattern);
				replaceAllInList(comboList, pattern, replacePattern);
				replaceAllInList(functionList, pattern, replacePattern);
			}
		}
	}
	
	private void removeUnusedFunctions() { 
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
			String pattern = "(?s).*\\b(combo_run|call)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b.*";
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
			
			for(int i = 0; i < usedFunctions.size(); i++) {
				if(!usedFunctions.get(functionNames.get(i))) functionList.set(i, "");
			}
			for(int i = 0; i < usedCombos.size(); i++) {
				if(!usedCombos.get(comboNames.get(i))) {
					String pattern = "\\b(combo_running|combo_suspended)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b\\s*\\)";
					String pattern2 = "\\b(combo_suspend|combo_stop)\\b\\s*\\(\\s*\\b" + comboNames.get(i) + "\\b\\s*\\)\\s*(;|\\s*)";
					replaceAllInList(mainCode, pattern, "FALSE");
					replaceAllInList(mainCode, pattern2, "");
					replaceAllInList(initCode, pattern, "FALSE");
					replaceAllInList(initCode, pattern2, "");
					replaceAllInList(comboList, pattern, "FALSE");
					replaceAllInList(comboList, pattern2, "");
					replaceAllInList(functionList, pattern, "FALSE");
					replaceAllInList(functionList, pattern2, "");
					comboList.set(i, "");
				}
			}
		}
	}
	
	private void fixSemicolons() {
		for(int i = 0; i < initCode.size(); i++) {
			initCode.set(i, fixSemicolons(initCode.get(i)));
		}
		for(int i = 0; i < mainCode.size(); i++) {
			mainCode.set(i, fixSemicolons(mainCode.get(i)));
		}
		for(int i = 0; i < comboList.size(); i++) {
			comboList.set(i, fixSemicolons(comboList.get(i)));
		}
		for(int i = 0; i < functionList.size(); i++) {
			functionList.set(i, fixSemicolons(functionList.get(i)));
		}
	}
	
	private String fixSemicolons(String s) {
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
	
	private String fixSemicolons(List<String> codeBlock) { // ADD CODE TO REMOVE RANDOM TOKENS
		String newStr = "";
		boolean prevVar = false, oneLineIf = false;
		int parenCount = 0;
		while(!codeBlock.isEmpty()) {
			String fixedStr = codeBlock.get(0);
			if(fixedStr.isEmpty()) { } // Empty line
			else if(fixedStr.matches("(main|function|combo|init)")) { // matches start of stuff
				fixedStr = fixedStr + " ";
				while(!codeBlock.get(1).equals("{")) {
					fixedStr += codeBlock.get(1) + "";
					codeBlock.remove(1);
				}
				fixedStr += codeBlock.get(1) + "\r\n";
				codeBlock.remove(1);
			}
			else if(fixedStr.matches("\\(")) {
				if(parenCount <= 0) parenCount = 0;
				parenCount++;
				prevVar = false;
			}
			else if(fixedStr.matches((".*(\\[|\\]).*"))) {
				prevVar = false;
			}
			else if(fixedStr.matches("\\)")) {
				parenCount--;
				prevVar = true;
				if(parenCount == 0) {
					parenCount = -1; // paren just ended
				}
			}
			else if(fixedStr.matches("\\;")) {
				if(parenCount <= 0)  { // for loop conditions
					fixedStr +=  "\r\n";
					oneLineIf = false;
				}
				else fixedStr += " ";
				prevVar = false;
			}
			else if(fixedStr.matches(("return"))) {
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
			else if(fixedStr.matches("(\\{|\\}|\\!)")) { // { } !
				if(fixedStr.equals("{")) {
					if(!oneLineIf) { // remove random { }
						fixedStr = "";
						int i = 1;
						while(!codeBlock.get(i).equals("}")) {
							i++;
						}
						codeBlock.remove(i);
					}
					else fixedStr += "\r\n";
					oneLineIf = false;
				}
				if(fixedStr.equals("}")) {
					fixedStr = (prevVar ? ";" : "") + "\r\n" + fixedStr + "\r\n";
				}
				else if(prevVar && parenCount != -1) fixedStr = ";\r\n" + fixedStr;
				prevVar = false;
			}
			else if(fixedStr.matches(".*(\\,|\\+|\\-|\\*|\\/|\\%|\\&|\\||\\=|\\!|\\^|\\>|\\<).*")) { 
				if(fixedStr.length() > 1) {
					codeBlock.add(1, fixedStr.substring(1));
					fixedStr = "" + fixedStr.charAt(0);
				}
				prevVar = false;
			}
			else {
				if(oneLineIf && parenCount <= 0) {
					fixedStr = " " + fixedStr;
					oneLineIf = false;
				}
				else if(prevVar) {
					if(fixedStr.matches("\\W")) {
						System.out.println(fixedStr);
						fixedStr = ";\r\n";
					}
					if(!fixedStr.matches("^[0-9]*$"))
						fixedStr = ";\r\n" + fixedStr;
					else fixedStr = ""; // remove random numbers
				}
				
				prevVar = true;
			}
			codeBlock.remove(0); // pop first off
			newStr += fixedStr;
		}
		return newStr;
	}
	
	// fix negative consts cm -> t1
	
	// convert for loops to while loops cm -> t1
	
	private void fixErrors() {
		replaceKeywords();
		replaceFunctionNames();
		replaceComboNames();
		removeUnusedFunctions();
		flattenMutlidimArrays();
		fixSemicolons();
	}
	
	@Override
	public String toString() {
		String str = "";
		String dEndLine = "\r\n\r\n";
		str += String.join("\r\n", definedList) + ((definedList.isEmpty()) ? "" : dEndLine);
		str += dataSegment + ((dataSegment.length() == 0) ? "" : dEndLine);
		str += String.join("\r\n", mappingCode) + ((mappingCode.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", varArrayList) + ((varArrayList.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", varList) + ((varList.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", initCode) + ((initCode.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", mainCode) + ((mainCode.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", comboList) + ((comboList.isEmpty()) ? "" : dEndLine);
		str += String.join("\r\n", functionList) + ((functionList.isEmpty()) ? "" : dEndLine);
		str = fortmatCode(str);
		return str;
	}
	
	public static String fortmatCode(String s) {
		int braceCount = 0;
		String newStr = "";
		s = s.replaceAll("\r\n\\s*\r\n\\}", "\r\n\\}"); // replace blank line followed by }
		s = s.replaceAll("\\s*\\{", " {"); // set all { 1 from previous
		s = s.replaceAll(",", ", "); // put space after commas
		s = s.replaceAll("\\)\\s*\\{", ") {");
		s = s.replaceAll("\\belse\\b\\s*\\bif\\b", "else if"); //replace All else\r\nif with else if\r\n
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
	
	public void replaceAllInList(List<String> s, String pattern, String replace) {
		for(int i = 0; i < s.size(); i++) s.set(i, s.get(i).replaceAll(pattern, replace));
	}
	
}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class GPC {
	
	private String gpcFilePath;
	
	private List<String> definedList;
	private List<String> varList;
	private List<String> mappingCode;
	private List<String> initCode;
	private List<String> mainCode;
	private List<String> comboList;
	private List<String> functionList;
	private String dataSegment;
	
	private static Scanner gpcReader;
	private static int readIdx;
	private static int codeBlockBraceCount;
	
	public GPC(String s) {
		definedList = new ArrayList<String>();
		varList = new ArrayList<String>();
		mappingCode = new ArrayList<String>();
		initCode = new ArrayList<String>();
		mainCode = new ArrayList<String>();
		comboList = new ArrayList<String>();
		functionList = new ArrayList<String>();
		dataSegment = "";
		gpcFilePath = s;
		
		readAll();
	}
	
	public String getRawGPCCode() {
		String rawCode = "";
		try {
			rawCode = new String(Files.readAllBytes(Paths.get(gpcFilePath)), StandardCharsets.UTF_8);
			rawCode = removeComments(rawCode);
			rawCode = replaceKeywords(rawCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rawCode;
	}
	
	private void readAll() {
		String rawGPC = getRawGPCCode();
		gpcReader = new Scanner(rawGPC);
		while(gpcReader.hasNextLine()) {
			String currLine = readNextLine();
			while(readIdx < currLine.length()) {
				currLine = currLine.substring(readIdx).trim();
				readIdx = 0;
				if(currLine.startsWith("define "))  definedList.add(parseDefine(currLine));
				else if(currLine.startsWith("unmap") || currLine.startsWith("map") || currLine.startsWith("remap")) mappingCode.add(parseMapping(currLine));
				else if(currLine.startsWith("int ")) varList.add(parseVarType(currLine));
				else if(currLine.startsWith("init")) initCode.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("main")) mainCode.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("combo ")) comboList.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("function ")) functionList.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("data")) dataSegment = parseCodeBlock(currLine, '(', ')') + ";"; // will cause ; to trigger other
				
				else { // unknown line
					System.out.println("Other: " + currLine);
					readIdx = currLine.length();
				}
				
				if(readIdx < currLine.length()) {
					//System.out.println(currLine);
				}
			}
		}
		fixErrors();
		gpcReader.close();
	}
	
	private static String readNextLine() {
		String str = "";
		do {
			str = trimSpacing(gpcReader.nextLine());
		} while(gpcReader.hasNextLine() && str.isEmpty());
		readIdx = 0;
		return str;
	}
	
	private static String parseDefine(String s) {
		String parsedStr = "";
		int i = 0;
		while(i < s.length()) {
			readIdx++;
			if(s.charAt(i) == ';') break;
			parsedStr += "" + s.charAt(i);
			i++;
		}
		if(parsedStr.trim().endsWith(",")) {
			String tmp = parsedStr.trim();
			tmp = tmp + readNextLine();
			i = -i; // set it to negative, so it subtracts the number of chars previously read in prev lines
			parsedStr = parseDefine(tmp);
		}
		return (parsedStr + (!parsedStr.endsWith(";") ? ";" : "")).replaceAll(",", ";\r\ndefine ");
	}
	
	private static String parseMapping(String s) { 
		String parsedStr = "";
		int i = 0;
		while(i < s.length()) {
			readIdx++;
			if(s.charAt(i) == ';') break;
			parsedStr += "" + s.charAt(i);
			i++;
		}
		// add support for multiline remaps?
		return parsedStr + (!parsedStr.endsWith(";") ? ";" : "");
	}
	
	private static String parseVarType(String s) {
		String parsedStr = "";
		int i = 0;
		while(i < s.length()) {
			readIdx++;
			if(s.charAt(i) == ';') break;
			parsedStr += "" + s.charAt(i);
			i++;
		}
		if(parsedStr.trim().endsWith(",")) {
			String tmp = parsedStr.trim() + "\r\n\t" + readNextLine();
			i = -i; // set it to negative, so it subtracts the number of chars previously read in prev lines
			parsedStr = parseVarType(tmp);
		}
		return parsedStr + (!parsedStr.endsWith(";") ? ";" : "");
	}
	
	private static String parseCodeBlock(String s, char blockSetStart, char blockSetEnd) {
		String parsedStr = "";
		int i = 0;
		while(i < s.length()) {
			readIdx++;
			parsedStr += "" + s.charAt(i);
			if(s.charAt(i) == blockSetStart) {
				codeBlockBraceCount++;
			}
			else if(s.charAt(i) == blockSetEnd) {
				codeBlockBraceCount--;
				if(codeBlockBraceCount <= 0) {
					//if(s.charAt(s.length() - 1) == ';') readIdx++;
					codeBlockBraceCount = -1;
					break;
				}
			}
			i++;
		}
		if(codeBlockBraceCount != -1) {
			String tmp = parsedStr + "\r\n" + readNextLine();
			i = -i; // set it to negative, so it subtracts the number of chars previously read in prev lines
			codeBlockBraceCount = 0;
			parsedStr = parseCodeBlock(tmp, blockSetStart, blockSetEnd);
		}
		codeBlockBraceCount = 0;
		return parsedStr;
	}

	private static String replaceKeywords(String s) {
		File tmpDir = new File(System.getProperty("user.dir") + "\\keywords.db");
		if(tmpDir.exists()) {
			try {
				Scanner kwSc = new Scanner(tmpDir);
				while(kwSc.hasNextLine()) {
					String str = trimSpacing(kwSc.nextLine());
					s = s.replaceAll("\\b" + str + "\\b", "t1_" + str);
				}
				kwSc.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		s = s.replaceAll("\\s*combo\\s+(\\w+)\\W+\\{", "combo $1 \\{");
		s = s.replaceAll((":"), (" ")); // : = ; in CM lool
		s = s.replaceAll("main\\s*\\{", "main\\{");
		s = s.replaceAll("init\\s*\\{", "init\\{");
		s = s.replaceAll("data\\s*\\(", "data\\(");
		s = s.replaceAll("\\bdefine\\b", "\r\ndefine");
		s = s.replaceAll("\\bint\\b", "\r\nint");
		s = s.replaceAll("\\binit\\b", "\r\ninit");
		s = s.replaceAll("\\bmain\\b", "\r\nmain");
		s = s.replaceAll("\\bcombo\\b", "\r\ncombo");
		s = s.replaceAll("\\bfunction\\b", "\r\nfunction");
		s = s.replaceAll("\\bdata\\b", "\r\ndata");
		s = s.replaceAll("\\s*;", ";");
		return s;
	}
	
	private void replaceComboNames() {
		if(comboList.isEmpty()) return;
		List<String> comboNames = getComboNames();
		String pattern = "(combo_run|combo_running|combo_stop|combo_restart|call)\\s*\\(\\s*";
		for(int i = 0; i < initCode.size(); i++) {
			for(int k = 0; k < comboNames.size(); k++)
				initCode.set(i, initCode.get(i).replaceAll(pattern + comboNames.get(k) + "\\s*\\)", " $1\\(c_" + comboNames.get(k)+ "\\)"));
		}
		for(int i = 0; i < mainCode.size(); i++) {
			for(int k = 0; k < comboNames.size(); k++)
				mainCode.set(i, mainCode.get(i).replaceAll(pattern + comboNames.get(k) + "\\s*\\)", " $1\\(c_" + comboNames.get(k)+ "\\)"));
		}
		for(int i = 0; i < comboList.size(); i++) {
			for(int k = 0; k < comboNames.size(); k++) {
				comboList.set(i, comboList.get(i).replaceAll(pattern + comboNames.get(k) + "\\s*\\)", " $1\\(c_" + comboNames.get(k) + "\\)"));
				comboList.set(i, comboList.get(i).replaceAll("\\s*combo\\s*" + comboNames.get(k) + "\\s*\\{", "combo c_" + comboNames.get(k) + "\\{"));
			}
		}
		for(int i = 0; i < functionList.size(); i++) {
			for(int k = 0; k < comboNames.size(); k++)
				functionList.set(i, functionList.get(i).replaceAll(pattern + comboNames.get(k) + "\\s*\\)", " $1\\(c_" + comboNames.get(k)+ "\\)"));
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
	
	private void replaceFunctionNames() {
		if(functionList.isEmpty()) return;
		List<String> functionNames = getFunctionNames();
		
		for(int i = 0; i < initCode.size(); i++) {
			for(int k = 0; k < functionNames.size(); k++)
				initCode.set(i, initCode.get(i).replaceAll("\\b\\s*" + functionNames.get(k) + "\\b\\s*\\(", " f_" + functionNames.get(k) + "\\("));
		}
		for(int i = 0; i < mainCode.size(); i++) {
			for(int k = 0; k < functionNames.size(); k++)
				mainCode.set(i, mainCode.get(i).replaceAll("\\b\\s*" + functionNames.get(k) + "\\b\\s*\\(", " f_" + functionNames.get(k) + "\\("));
		}
		for(int i = 0; i < comboList.size(); i++) {
			for(int k = 0; k < functionNames.size(); k++)
				comboList.set(i, comboList.get(i).replaceAll("\\b\\s*" + functionNames.get(k) + "\\b\\s*\\(", " f_" + functionNames.get(k) + "\\("));
			}
		for(int i = 0; i < functionList.size(); i++) {
			for(int k = 0; k < functionNames.size(); k++)
				functionList.set(i, functionList.get(i).replaceAll("\\b\\s*" + functionNames.get(k) + "\\b\\s*\\(", " f_" + functionNames.get(k) + "\\("));	
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
		//String[] k = ss.split("\\b|(?=\\)?)|(?=\\(?)");
		// splits by ( ) { } ; , and var start/end
		List<String> tmp = new ArrayList<String>(Arrays.asList(s.split("\\b|(?=\\))|(?=\\()|(?<=\\))|(?<=\\()|(?=\\;)|(?<=\\;)|(?=\\})|(?<=\\})|(?=\\{)|(?<=\\{)|(?=\\,)|(?<=\\,)")));
		//for(int i = 0; i < tmp.size(); i++)
			//System.out.println(i + " " + tmp.get(i).trim().replaceAll("\\s*", ""));
		return fixSemicolons(tmp, false, false, 0, false);
	}
	
	//Strictly recursive helper function
	private String fixSemicolons(List<String> codeBlock, boolean prevOp, boolean prevVar, int parenCount, boolean oneLineIf) {
		if(codeBlock.isEmpty()) return "";
		String fixedStr = codeBlock.get(0).trim().replaceAll("\\s", "");
		
		if(fixedStr.isEmpty()) { } // Empty line
		else if(fixedStr.matches("\\s*(main|function|combo|init)\\s*")) { // matches start of stuff
			fixedStr = fixedStr + " ";
			if(fixedStr.equals("combo ")) {
				fixedStr = fixedStr + codeBlock.get(2) ;
				codeBlock.remove(2);
			}
		}
		else if(fixedStr.matches("\\(")) {
			if(parenCount <= 0) parenCount = 0;
			parenCount++;
			prevVar = false;
			prevOp = true;
		}
		else if(fixedStr.matches((".*(\\[|\\]).*"))) {
			prevVar = false;
			prevOp = false;
		}
		else if(fixedStr.matches("\\)")) {
			parenCount--;
			prevVar = true;
			prevOp = false;
			if(parenCount == 0) {
				parenCount = -1; // paren just ended
			}
		}
		else if(fixedStr.matches("\\;")) {
			fixedStr = fixedStr + "\r\n";
			prevVar = false;
			prevOp = true;
		}
		else if(fixedStr.matches(("return"))) {
			fixedStr += " ";
			prevVar = false;
			prevOp = false;
		}
		else if(fixedStr.matches("\\s*(if|else)\\s*")) { // if statement
			if(prevVar) fixedStr = ";\r\n" + fixedStr;
			fixedStr += " ";
			oneLineIf = true;
		}
		else if(fixedStr.matches("(\\{|\\}|\\!)")) { // { } !
			if(fixedStr.equals("{")) {
				oneLineIf = false;
				fixedStr += "\r\n";
			}
			if(fixedStr.equals("}")) {
				fixedStr = (prevVar ? ";" : "") + "\r\n" + fixedStr + "\r\n";
			}
			else if(prevVar && parenCount != -1) fixedStr = ";\r\n" + fixedStr;
			prevVar = false;
			prevOp = true;
		}
		else if(fixedStr.matches(".*(\\,|\\+|\\-|\\*|\\/|\\&|\\||\\=|\\!|\\^|\\>|\\<).*")) { 
			if(fixedStr.length() > 1) {
				codeBlock.add(1, fixedStr.substring(1));
				fixedStr = "" + fixedStr.charAt(0);
			}
			prevOp = true;
			prevVar = false;
		}
		else {
			if(oneLineIf && parenCount <= 0) {
				fixedStr = "\r\n\t" + fixedStr;
				oneLineIf = false;
			}
			else if(prevVar) {
				if(!fixedStr.matches("^[0-9]*$"))
					fixedStr = ";\r\n" + fixedStr;
				else fixedStr = "";
			}
			
			prevVar = true;
			prevOp = false;
		}
		codeBlock.remove(0); // pop first off
		//System.out.print(fixedStr);
		return fixedStr + fixSemicolons(codeBlock, prevOp, prevVar, parenCount, oneLineIf);
	}
	
	private void fixErrors() {
		replaceFunctionNames();
		replaceComboNames();
		fixSemicolons();
	}
	
	public String toString() {
		String str = "";
		str += String.join("\r\n", definedList) + "\r\n\r\n";
		str += String.join("\r\n", mappingCode) + "\r\n\r\n";
		str += String.join("\r\n", varList) + "\r\n\r\n";
		str += String.join("\r\n", initCode) + "\r\n\r\n";
		str += String.join("\r\n", mainCode) + "\r\n\r\n";
		str += String.join("\r\n", comboList) + "\r\n\r\n";
		str += String.join("\r\n", functionList) + "\r\n\r\n";
		str += dataSegment;
		fixSemicolons("");
		return str;
	}

	private static String removeComments(String code) {
	    StringBuilder newCode = new StringBuilder();
	    try (StringReader sr = new StringReader(code)) {
	        boolean inBlockComment = false;
	        boolean inLineComment = false;
	        boolean out = true;

	        int prev = sr.read();
	        int cur;
	        for(cur = sr.read(); cur != -1; cur = sr.read()) {
	            if(inBlockComment) {
	                if (prev == '*' && cur == '/') {
	                    inBlockComment = false;
	                    out = false;
	                }
	            } else if (inLineComment) {
	                if (cur == '\r') { // start untested block
	                    sr.mark(1);
	                    int next = sr.read();
	                    if (next != '\n') {
	                        sr.reset();
	                    }
	                    newCode.append("\r\n");
	                    inLineComment = false;
	                    out = false; // end untested block
	                } 
					else if (cur == '\n') {
						newCode.append("\n");
	                    inLineComment = false;
	                    out = false;
	                }
	            } 
				else {
	                if (prev == '/' && cur == '*') {
	                    sr.mark(1); // start untested block
	                    //int next = sr.read();
						// if (next != '*') 
	                        inBlockComment = true; // tested line (without rest of block)
	                    //
	                    sr.reset(); // end untested block
	                }
					else if (prev == '/' && cur == '/') {
	                    inLineComment = true;
	                } 
					else if (out){
	                    newCode.append((char)prev);
	                } 
					else {
	                    out = true;
	                }
	            }
	            prev = cur;
	        }
	        if (prev != -1 && out && !inLineComment) {
	            newCode.append((char)prev);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    return newCode.toString();
	}
	
	private static String trimSpacing(String s) {
		if(s.isEmpty()) return "";
		return s.trim().replaceAll("(\\s+)"," ");
	}
	
}

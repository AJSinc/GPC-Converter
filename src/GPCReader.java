import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GPCReader {
	
	private String gpcFilePath;
	
	private List<String> definedList;
	private List<String> varList;
	private List<String> varArrayList;
	private List<String> mappingCode;
	private List<String> initCode;
	private List<String> mainCode;
	private List<String> comboList;
	private List<String> functionList;
	private String dataSegment;
	
	private static Scanner gpcReader;
	private static int readIdx;
	private static int codeBlockBraceCount;
	
	public GPCReader(String s) {
		definedList = new ArrayList<String>();
		mappingCode = new ArrayList<String>();
		varList = new ArrayList<String>();
		varArrayList = new ArrayList<String>();
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
			rawCode = replaceFormatting(rawCode);
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
				else if(currLine.startsWith("const ")) varArrayList.add((parseCodeBlock(currLine, '{', '}') + ";").replaceAll("\\bbyte\\b", "int8"));	
				else if(currLine.startsWith("init")) initCode.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("main")) mainCode.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("combo ")) comboList.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("function ")) functionList.add(parseCodeBlock(currLine, '{', '}'));
				else if(currLine.startsWith("data")) dataSegment = parseCodeBlock(currLine, '(', ')') + ";"; // will cause ; to trigger other
				else { // unknown line
					int idx = currLine.indexOf(" ");
					System.out.println("Other: " + currLine);
					readIdx = currLine.length();
					if(idx != -1) {
						readIdx = idx+1;
					}
				}
			}
		}
		gpcReader.close();
	}
	
	private String readNextLine() {
		String str = "";
		do {
			str = trimSpacing(gpcReader.nextLine());
		} while(gpcReader.hasNextLine() && str.isEmpty());
		readIdx = 0;
		return str;
	}
	
	private String parseDefine(String s) {
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
	
	private String parseMapping(String s) { 
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
	
	private String parseVarType(String s) {
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
	
	private String parseCodeBlock(String s, char blockSetStart, char blockSetEnd) {
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
	
	private String removeComments(String code) {
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
	
	private static String replaceFormatting(String s) {
		s = s.replaceAll("\\s*combo\\s+(\\w+)\\W+\\{", "combo $1 \\{");
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
		s = s.replaceAll((":"), (";")); // : = ; in CM
		s = s.replaceAll("\\s*;", ";");
		return s;
	}
	
	private static String trimSpacing(String s) {
		if(s.isEmpty()) return "";
		return s.trim().replaceAll("(\\s+)"," ");
	}
	
	public List<String> getDefines() {
		return definedList;
	}
	
	public List<String> getMappings() {
		return mappingCode;
	}
	
	public List<String> getVars() {
		return varList;
	}
	
	public List<String> getVarArrays() {
		return varArrayList;
	}
	
	public List<String> getInitCode() {
		return initCode;
	}
	
	public List<String> getMainCode() {
		return mainCode;
	}
	
	public List<String> getCombos() {
		return comboList;
	}
	
	public List<String> getFunctions() {
		return functionList;
	}
	
	public String getDataSegment() {
		return dataSegment;
	}
	
}


public class CodeFormatter {
	
	public static String fortmatCode(String s) {
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
		s = s.replaceAll("(\\W\\s*)\\-\\s*(\\w)", "$1 -$2"); // formats - (negative) symbols to attach to number/var name
		s = s.replaceAll("(\\w)\\s*([\\+\\+|\\-\\-])", "$1$2"); // attach ++ and -- back to vars
		s = s.replaceAll("\\s*\\;", ";"); // remove space between line end
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
	
}

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GPCtoGPC2 {
	
	public static void main(String args[]) throws IOException {
		File dir = new File(System.getProperty("user.dir"));
		File[] files = dir.listFiles((d, name) -> name.endsWith(".gpc"));
		for(int i = 0; i < files.length; i++) {
			if(!files[i].getName().contains(".gpc2"))
				convert(files[i]);
		}
	}
	
	public static void convert(File gpcFile) throws FileNotFoundException, IOException {
		GPC newGPC = new GPC(gpcFile.getCanonicalPath());
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gpcFile.getName().substring(0, (gpcFile.getName().length() - 3)) + "gpc2.gpc"), "utf-8"));
		writer.write("#pragma METAINFO(\"" + gpcFile.getName() +  "\", 1, 0, \"Buffy's GPC Converter\"))\r\n");
		writer.write(
				"#include <titanone.gph>\r\n\r\n");/* +
				
				"#undef set_val\r\n" + 
				"#undef wait\r\n" + 
				"#define set_val(a,b) set_val((uint8)a,(int16)b);\r\n" + 
				"#define wait(a) ;wait((int)a);\r\n" + 
				"#define IF(a) if(a)\r\n\r\n");
				*/
		
		writer.write(newGPC.toString());
		writer.close();
	}
	
	
	

	
}

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GPCtoGPC2 {
	
	public static void main(String args[]) throws IOException {
		File gpcFile = chooseFile();
		if(gpcFile != null && gpcFile.exists()) {
			convert(gpcFile);
		}
		System.exit(0);
	}
	
	public static File chooseFile() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error : " + e.toString());
			return null;
		}
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("GPC Script files", "gpc");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) 
			return chooser.getSelectedFile();
		return null;
	}
	
	public static void convert(File gpcFile) throws FileNotFoundException, IOException {
		GPC newGPC = new GPC(gpcFile.getCanonicalPath());
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gpcFile.getCanonicalPath().substring(0, (gpcFile.getCanonicalPath().length() - 3)) + "gpc2.gpc"), "utf-8"));
		writer.write("#pragma METAINFO(\"" + gpcFile.getName() +  "\", 1, 0, \"Buffy's GPC Converter v0.19\"))\r\n");
		writer.write("#include <titanone.gph>\r\n\r\n\r\n");
		
		writer.write(newGPC.toString());
		writer.close();
		//JOptionPane.showMessageDialog(null, "Script successfully converted.");
	}
	
}

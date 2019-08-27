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
		System.out.print("");
		File[] gpcFile = chooseFile();
		if(gpcFile != null && gpcFile.length > 0) {
			for(int i = 0; i < gpcFile.length; i++) { 
				try {
					convert(gpcFile[i]);
				}
				catch(Exception e) {
					JOptionPane.showMessageDialog(null, "Error converting script (" + gpcFile[i].getCanonicalPath() + ").\r\n"
							+ "Please check the file and try again.\r\n\r\n" 
							+ e.toString());
				}
			}
		}
		System.exit(0);
	}
	
	public static File[] chooseFile() {
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
		chooser.setMultiSelectionEnabled(true);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("GPC Script files", "gpc");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) 
			return chooser.getSelectedFiles();
		return null;
	}
	
	public static void convert(File gpcFile) throws FileNotFoundException, IOException {
		GPC gpc = new GPC(gpcFile.getCanonicalPath());
		GPCConverter.fixGPCErrors(gpc);
		String newCode = GPCConverter.fortmatGPCCode(gpc.toString()).replaceAll("\\bdiscord.gg\\/.+?\\b", "discord.gg");
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gpcFile.getCanonicalPath().substring(0, (gpcFile.getCanonicalPath().length() - 3)) + "Titan_Two.gpc"), "utf-8"));
		writer.write("#pragma METAINFO(\"" + gpcFile.getName() +  "\", 1, 0, \"Buffy's GPC Converter v0.25r5\")\r\n");
		writer.write("#include <titanone.gph>\r\n\r\n\r\n");
		writer.write(gpc.getCommentBlock() + "\r\n\r\n");
		writer.write(newCode);
		writer.close();
		//JOptionPane.showMessageDialog(null, "Script successfully converted.");
	}
	
	
}
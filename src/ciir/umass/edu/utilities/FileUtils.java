/*===============================================================================
 * Copyright (c) 2010-2015 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.utilities;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;


/**
 * This class provides some file processing utilities such as read/write files, obtain files in a
 * directory...
 * @author Van Dang
 * @version 1.3 (July 29, 2008)
 */
public class FileUtils {
	/**
	 * Read the content of a file.
	 * @param filename The file to read.
	 * @param encoding The encoding of the file.
	 * @return The content of the input file.
	 */
	@Nonnull
	public static String read(String filename, String encoding) {
		try {
			return new String(Files.readAllBytes(new File(filename).toPath()), encoding);
		} catch(Exception e) {
			throw new RankLibError("Couldn't read file="+filename, e);
		}
	}

	@Nonnull
	public static List<String> readLine(String filename, String encoding) {
		try {
			File fp = new File(filename);
			return Files.readAllLines(fp.toPath(), Charset.forName(encoding));
		} catch (IOException e) {
			throw new RankLibError("Couldn't read lines from file="+filename);
		}
	}

	/**
	 * Write a text to a file.
	 * @param filename The output filename.
	 * @param encoding The encoding of the file.
	 * @param strToWrite The string to write.
	 */
	public static void write(String filename, String encoding, String strToWrite) {
		try (BufferedWriter out = new BufferedWriter(
			          new OutputStreamWriter(new FileOutputStream(filename), encoding))) {
			out.write(strToWrite);
		} catch(Exception e) {
			System.err.println("Attempted to save: \"\"\""+strToWrite+"\"\"\" to file: "+filename);
			throw new RankLibError("Couldn't write file="+filename);
		}
	}
	/**
	 * Get all file (non-recursively) from a directory.
	 * @param directory The directory to read.
	 * @return A list of filenames (without path) in the input directory.
	 */
	@Nonnull
	public static String[] getAllFiles(String directory)
	{
		File dir = new File(directory);
		String[] fns = dir.list();
		if(fns == null) {
			return new String[] {};
		}
		return fns;
	}
	/**
	 * Get all file (non-recursively) from a directory.
	 * @param directory The directory to read.
	 * @return A list of filenames (without path) in the input directory.
	 */
	@Nonnull
	public static List<String> getAllFiles2(String directory) {
		return Arrays.asList(getAllFiles(directory));
	}

	@Nonnull
	public static String getFileName(String pathName) {
		File fp = new File(pathName);
		return fp.getName();
	}
	@Nonnull
	public static String makePathStandard(String directory) {
		try {
			File fp = new File(directory);
			return fp.getCanonicalPath();
		} catch (IOException e) {
			throw new RankLibError(e);
		}
	}
}

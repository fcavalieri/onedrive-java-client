package com.wouterbreukink.onedrive.tests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

public class TestHelper {
	
	private static final String FILECONTENT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\n";

	public static void generateTestData(File testDataFolder) throws IOException
	{
		generateTestData(testDataFolder, "fileA.txt", 1 * 1024);
		generateTestData(testDataFolder, "/folderA/fileB.txt", 2 * 1024);
		generateTestData(testDataFolder, "/folderB/fileC.txt", 4 * 1024);
		generateTestData(testDataFolder, "/folderC/folderD/folderE/folderF/fileD.txt", 6 * 1024 *1024);
	}
	
	public static void generateTestData(File testDataFolder, String testFilePath, long fileSize) throws IOException
	{
		File testFile = new File(testDataFolder.getAbsolutePath() + "/" + testFilePath);
		
		if (testFile.exists())
			throw new IOException("Test file " + testFile.getAbsolutePath() + " already exists");
		
		testFile.getParentFile().mkdirs();
		testFile.createNewFile();
        FileWriter writer = new FileWriter(testFile);

        for (int length = 0; length <= fileSize; length += FILECONTENT.length())
            writer.write(FILECONTENT);            
        
        writer.flush();
        writer.close();
	}
	
	public static void removeTestDataFolder(File testDataFolder) throws IOException
	{
		if (testDataFolder.exists())
		{
			if (testDataFolder.isFile())
				throw new IOException("Test data folder " + testDataFolder.getAbsolutePath() + " exists and is a file");
			else
			{
				FileUtils.deleteDirectory(testDataFolder);
			}
		}		
	}

	public static void assertEqualsFolders(Path firstRoot, Path secondRoot) throws IOException, TestException 
	{
		for (Path first : (Iterable<Path>) Files.walk(firstRoot).filter(Files::isRegularFile)::iterator)
		{
			Path second = Paths.get(secondRoot.toAbsolutePath().toString(), firstRoot.relativize(first).toString());
			checkPath(first, second);
		}
		for (Path second : (Iterable<Path>) Files.walk(secondRoot).filter(Files::isRegularFile)::iterator)
		{
			Path first = Paths.get(firstRoot.toAbsolutePath().toString(), secondRoot.relativize(second).toString());
			if (!first.toFile().exists())
				throw new TestException(first.toAbsolutePath() + " is unexpected.");
		}
	}
	
	public static void checkPath(Path first, Path second) throws IOException, TestException
	{
		if (!first.toFile().exists())
			throw new TestException(first.toAbsolutePath() + " is missing.");
		if (!second.toFile().exists())
			throw new TestException(second.toAbsolutePath() + " is missing.");
		
		if (!FileUtils.contentEquals(first.toFile(), second.toFile()))
			throw new TestException(first.toAbsolutePath() + " differs from " + second.toAbsolutePath());
	}

	public static void assertEncryptedFolder(Path downloadFolder) throws IOException, TestException 
	{
		String[] plainTextNames = { "fileA.txt", "fileB.txt", "fileC.txt", "fileD.txt", "folderA",
                                    "folderA", "folderB", "folderC", "folderD", "folderE", "folderF"};
		
		for (Path downloadedFilePath : (Iterable<Path>) Files.walk(downloadFolder).filter(Files::isRegularFile)::iterator)
		{
			Path relativePath = downloadFolder.relativize(downloadedFilePath);			
			for (String plainTextName : plainTextNames)
			{
				if (relativePath.toString().contains(plainTextName))
					throw new TestException("Path " + relativePath.toString() + " has not been encrypted");
			}
			
			if (FileUtils.readFileToString(downloadedFilePath.toFile()).contains(FILECONTENT))
				throw new TestException("Content of " + relativePath.toString() + " has not been encrypted");
		}	
	}
}

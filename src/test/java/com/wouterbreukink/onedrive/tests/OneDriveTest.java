package com.wouterbreukink.onedrive.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wouterbreukink.onedrive.Main;

public class OneDriveTest 
{
	static Path testFolder;
	static Path sourceFolder;
	static Path downloadFolder;
	static Path emptyFolder;
	static String remoteFolder = "onedrive-unit-tests";
	
	@BeforeClass
	public static void oneTimeSetUp() throws IOException 
	{
		testFolder = Files.createTempDirectory("onedrive-java-client");
		sourceFolder = Paths.get(testFolder.toAbsolutePath().toString(), "source-data");
		downloadFolder = Paths.get(testFolder.toAbsolutePath().toString(), "downloaded-data");
		emptyFolder = Paths.get(testFolder.toAbsolutePath().toString(), "empty-folder");
		
		sourceFolder.toFile().mkdirs();
		emptyFolder.toFile().mkdirs();
		
		TestHelper.generateTestData(sourceFolder.toFile());
		System.out.println("Test folder: " + testFolder);
		System.out.println("Source folder: " + sourceFolder);
		System.out.println("Download folder: " + downloadFolder);
		
        
	}
	
	@Before
	public void setUp() throws Exception
	{
		System.out.println("Clearing remote folder: " + remoteFolder);
		System.out.println("Clearing download folder: " + downloadFolder);		
		FileUtils.deleteDirectory(downloadFolder.toFile());
		downloadFolder.toFile().mkdirs();
		
		String[] parameters = 
			{
	          "--direction", "up",
	          "--recursive",
	          "--local", emptyFolder.toAbsolutePath().toString(),
	          "--remote", remoteFolder, 
	          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
	          "--log-level", "7",
	          "--threads", "1",
	          "--tries", "10"
			};
		
		Main.main(parameters);
	}
	
	@AfterClass
    public static void oneTimeTearDown() throws Exception 
	{
		TestHelper.removeTestDataFolder(testFolder.toFile());
    }
	
	@Test
	public void testUpDownNoEncryption() throws Exception 
	{
		System.out.println("Uploading test data");
		
		String[] parametersUp = 
		{
          "--direction", "up",
          "--recursive",
          "--local", sourceFolder.toAbsolutePath().toString(),
          "--remote", remoteFolder, 
          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
          "--log-level", "7",
          "--threads", "1",
          "--tries", "10"
		};
		
		Main.main(parametersUp);
		
		System.out.println("Downloading test data");
		String[] parametersDown = 
			{
	          "--direction", "down",
	          "--recursive",
	          "--local", downloadFolder.toAbsolutePath().toString(),
	          "--remote", remoteFolder, 
	          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
	          "--log-level", "7",
	          "--threads", "1",
	          "--tries", "10"
			};
		
		Main.main(parametersDown);
		
		TestHelper.assertEqualsFolders(sourceFolder, downloadFolder);
	}
	
	@Test
	public void testUpDownEncryption() throws Exception
	{
        System.out.println("Uploading test data");
		
		String[] parametersUp = 
		{
          "--direction", "up",
          "--recursive",
          "--local", sourceFolder.toAbsolutePath().toString(),
          "--remote", remoteFolder, 
          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
          "--log-level", "7",
          "--threads", "1",
          "--tries", "10",
          "--encryption-key", "password"
		};
		
		Main.main(parametersUp);
		
		System.out.println("Downloading test data (without decrypting)");
		String[] parametersDown = 
			{
	          "--direction", "down",
	          "--recursive",
	          "--local", downloadFolder.toAbsolutePath().toString(),
	          "--remote", remoteFolder, 
	          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
	          "--log-level", "7",
	          "--threads", "1",
	          "--tries", "10"
			};
		
		Main.main(parametersDown);
		
		TestHelper.assertEncryptedFolder(downloadFolder);
		
		FileUtils.deleteDirectory(downloadFolder.toFile());
		downloadFolder.toFile().mkdirs();
		
		System.out.println("Downloading test data (decrypting)");
		String[] parametersDownDec = 
			{
	          "--direction", "down",
	          "--recursive",
	          "--local", downloadFolder.toAbsolutePath().toString(),
	          "--remote", remoteFolder, 
	          "--keyfile", Paths.get(System.getProperty("user.home"),".onedrive-java-client", "onedrive.key").toAbsolutePath().toString(),
	          "--log-level", "7",
	          "--threads", "1",
	          "--tries", "10",
	          "--encryption-key", "password"
			};
		
		Main.main(parametersDownDec);
		
		TestHelper.assertEqualsFolders(sourceFolder, downloadFolder);
	}

	
	
}

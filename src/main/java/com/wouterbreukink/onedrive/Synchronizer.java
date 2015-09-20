package com.wouterbreukink.onedrive;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;
import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.OneDriveProvider;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.filesystem.FileSystemProvider;
import com.wouterbreukink.onedrive.tasks.CheckTask;
import com.wouterbreukink.onedrive.tasks.Task;
import com.wouterbreukink.onedrive.tasks.TaskReporter;

public class Synchronizer 
{
	public static void synchronize(Logger log) throws IOException, InterruptedException
	{
		// Initialise the OneDrive authorisation
		AuthorisationProvider authoriser;
		try {
			authoriser = AuthorisationProvider.FACTORY.create(getCommandLineOpts().getKeyFile());
			authoriser.getAccessToken();
		} catch (OneDriveAPIException ex) {
			log.error("Unable to authorise client: " + ex.getMessage());
			log.error("Re-run the application with --authorise");
			return;
		}
		// Initialise the providers
		OneDriveProvider api;
		FileSystemProvider fileSystem;
		if (getCommandLineOpts().isDryRun()) {
			log.warn("This is a dry run - no changes will be made");
			api = OneDriveProvider.FACTORY.readOnlyApi(authoriser);
			fileSystem = FileSystemProvider.FACTORY.readOnlyProvider();
		} else {
			api = OneDriveProvider.FACTORY.readWriteApi(authoriser);
			fileSystem = FileSystemProvider.FACTORY.readWriteProvider();
		}

		// Report on progress
		TaskReporter reporter = new TaskReporter();

		// Get the primary drive
		Drive primary = api.getDefaultDrive();

		// Report quotas
		log.info(String.format("Using drive with id '%s' (%s). Usage %s of %s (%.2f%%)",
				primary.getId(),
				primary.getDriveType(),
				readableFileSize(primary.getQuota().getUsed()),
				readableFileSize(primary.getQuota().getTotal()),
				((double) primary.getQuota().getUsed() / primary.getQuota().getTotal()) * 100));

		// Check the given root folder
		OneDriveItem rootFolder = api.getPath(getCommandLineOpts().getRemotePath());

		if (!rootFolder.isDirectory()) {
			log.error(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
			return;
		}

		File localRoot = new File(getCommandLineOpts().getLocalPath());

		log.info(String.format("Local folder '%s'", localRoot.getAbsolutePath()));
		log.info(String.format("Remote folder '<onedrive>%s'", rootFolder.getFullName()));

		// Start synchronisation operation at the root
		final TaskQueue queue = new TaskQueue();
		queue.add(new CheckTask(new Task.TaskOptions(queue, api, fileSystem, reporter), rootFolder, localRoot));

		// Get a bunch of threads going
		ExecutorService executorService = Executors.newFixedThreadPool(getCommandLineOpts().getThreads());

		for (int i = 0; i < getCommandLineOpts().getThreads(); i++) {
			executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						//noinspection InfiniteLoopStatement
						while (true) {
							Task taskToRun = null;
							try {
								taskToRun = queue.take();
								taskToRun.run();
							} finally {
								if (taskToRun != null) {
									queue.done(taskToRun);
								}
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
		}

		queue.waitForCompletion();
		log.info("Synchronisation complete");
		reporter.report();
	}
}

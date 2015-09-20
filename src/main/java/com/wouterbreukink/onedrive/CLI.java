package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.OneDriveProvider;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.filesystem.FileSystemProvider;
import com.wouterbreukink.onedrive.tasks.CheckTask;
import com.wouterbreukink.onedrive.tasks.Task;
import com.wouterbreukink.onedrive.tasks.TaskReporter;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;
import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;

/***
 * OneDrive Java Client
 * Copyright (C) 2015 Wouter Breukink
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
public class CLI {

    private static final Logger log = LogManager.getLogger(CLI.class.getName());

    public static void main(String[] args) throws Exception 
    {
        // Parse command line args
        try {
            CommandLineOpts.initialise(args);
        } catch (ParseException ex) {
            log.error("Unable to parse command line arguments - " + ex.getMessage());
            CommandLineOpts.printHelp();
            return;
        }

        if (getCommandLineOpts().help()) {
            CommandLineOpts.printHelp();
            return;
        }

        if (getCommandLineOpts().version()) {
            String version = getCommandLineOpts().getClass().getPackage().getImplementationVersion();
            log.info("onedrive-java-client version " + (version != null ? version : "DEVELOPMENT"));
            return;
        }

        // Initialise a log file (if set)
        if (getCommandLineOpts().getLogFile() != null) {
            String logFileName = LogUtils.addFileLogger(getCommandLineOpts().getLogFile());
            log.info(String.format("Writing log output to %s", logFileName));
        }

        if (getCommandLineOpts().isAuthorise()) {
            AuthorisationProvider.FACTORY.printAuthInstructions();
            return;
        }

        if (getCommandLineOpts().getLocalPath() == null
                || getCommandLineOpts().getRemotePath() == null
                || getCommandLineOpts().getDirection() == null) {
            log.error("Must specify --local, --remote and --direction");
            CommandLineOpts.printHelp();
            return;
        }

        Synchronizer.synchronize(log);
    }
}

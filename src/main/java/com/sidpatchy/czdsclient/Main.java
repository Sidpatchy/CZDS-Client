package com.sidpatchy.czdsclient;

import com.sidpatchy.czdsclient.IO.CZDSClient;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    private static CZDSClient client;

    // For Commandline Arguments
    private static Options options = new Options();
    private static Option helpOption;
    private static Option versionOption;
    private static Option debugOption;
    private static Option usernameOption;
    private static Option passwordOption;
    private static Option getAllZonesOption;
    private static Option specifyZoneOption;
    private static Option filePathOption;
    private static Option showApprovedTLDsOption;

    private static String username;
    private static String password;
    private static boolean debugMode = false;

    private static CommandLineParser parser = new DefaultParser();
    private static HelpFormatter formatter = new HelpFormatter();

    public static void main(String[] args) throws IOException {
        setLogLevel(Level.INFO);

        usernameOption = new Option("u", "username", true, "ICANN CZDS Username");
        passwordOption = new Option("p", "password", true, "ICANN CZDS Password");
        debugOption = new Option("d", "debug", false, "Enable debug mode");
        helpOption = new Option("h", "help", false, "Show help");
        versionOption = new Option("v", "version", false, "Show version");
        getAllZonesOption = new Option("a", "all", false, "Download all zones");
        specifyZoneOption = new Option("z", "zone", true, "Specify a zone file to download");
        filePathOption = new Option("f", "path", true, "Specify which directory zone files should be downloaded to -- defaults to './Downloads/'");
        showApprovedTLDsOption = new Option("s", "show-approved", false, "Lists all TLDs you are approved to access");

        options.addOption(usernameOption);
        options.addOption(passwordOption);
        options.addOption(debugOption);
        options.addOption(helpOption);
        options.addOption(versionOption);
        options.addOption(getAllZonesOption);
        options.addOption(specifyZoneOption);
        options.addOption(filePathOption);
        options.addOption(showApprovedTLDsOption);

        if (args.length == 0) {
            formatter.printHelp("CZDS-Client", options);
        }

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.fatal("Error while parsing commandline options", e);
            throw new RuntimeException(e);
        }

        if (cmd.hasOption('d')) {
            logger.info("Debug mode active.");
            debugMode = true;
            setLogLevel(Level.DEBUG);
        }

        // Store the commandline arguments
        username = cmd.getOptionValue('u');
        password = cmd.getOptionValue('p');

        logger.debug("Username: " + username);
        logger.debug("Password: " + password);

        client = new CZDSClient(username, password);

        if (cmd.hasOption('v')) {
            InputStream input = Main.class.getClassLoader().getResourceAsStream("project.properties");
            Properties properties = new Properties();
            properties.load(input);
            System.out.println(properties.getProperty("version"));
            System.exit(0);
        }

        if (cmd.hasOption('h')) {
            formatter.printHelp("ZoneFile-Tools", options);
            System.exit(0);
        }

        if (cmd.hasOption('f')) {
            client.getDownloader().setDownloadPath(cmd.getOptionValue('f'));
            logger.debug("Download path update to " + cmd.getOptionValue('f'));
        }

        if (cmd.hasOption('s')) {
            List<String> approvedTLDs = client.getDownloader().getApprovedTLDs().join();
            System.out.println("Approved TLDs:");
            for (String tld : approvedTLDs) {System.out.println("   - " + tld);}
        }

        if (cmd.hasOption('a')) {
            client.getDownloader().downloadAllApprovedZoneFiles().join();
            System.exit(0);
        }

        if (cmd.hasOption('z')) {
            client.getDownloader().downloadZoneFile(cmd.getOptionValue('z')).join();
            System.exit(0);
        }

        System.exit(0);
    }

    private static void setLogLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static void setPrivateField(Object obj, String fieldName, String value) throws Exception {
        logger.debug("Field Name: " + fieldName + ", Value: " + value);

        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true); // Make the private field accessible
        field.set(obj, value);     // Set the value
    }
}
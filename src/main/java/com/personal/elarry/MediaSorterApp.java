package com.personal.elarry;

import com.personal.elarry.config.ApplicationProperties;
import com.personal.elarry.config.CRLFLogConverter;
import com.personal.elarry.extended.util.MediaSorter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import tech.jhipster.config.DefaultProfileUtil;
import tech.jhipster.config.JHipsterConstants;

@SpringBootApplication
@EnableConfigurationProperties({ ApplicationProperties.class })
public class MediaSorterApp implements CommandLineRunner{

    private static final Logger log = LoggerFactory.getLogger(MediaSorterApp.class);

    private final Environment env;

    public MediaSorterApp(Environment env) {
        this.env = env;
    }

    /**
     * Initializes MediaSorter.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="https://www.jhipster.tech/profiles/">https://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        ) {
            log.error(
                "You have misconfigured your application! It should not run " + "with both the 'dev' and 'prod' profiles at the same time."
            );
        }
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)
        ) {
            log.error(
                "You have misconfigured your application! It should not " + "run with both the 'dev' and 'cloud' profiles at the same time."
            );
        }
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MediaSorterApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
        String serverPort = env.getProperty("server.port");
        String contextPath = Optional
            .ofNullable(env.getProperty("server.servlet.context-path"))
            .filter(StringUtils::isNotBlank)
            .orElse("/");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            "\n----------------------------------------------------------\n\t" +
            "Application '{}' is running! Access URLs:\n\t" +
            "Local: \t\t{}://localhost:{}{}\n\t" +
            "External: \t{}://{}:{}{}\n\t" +
            "Profile(s): \t{}\n----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles()
        );
    }


    private static final String SOURCE_FOLDER = "/Volumes/Transcend/2023 MOVIE";
    private static final String DESTINATION_FOLDER = "/Volumes/Transcend/NEW_2023_MOVIE";
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\([0-9]{4}\\)");

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int threadCount=10;
        String sourceFolder= StringUtils.EMPTY;
        String destinationFolder= StringUtils.EMPTY;
        Boolean moveFile =false;

        while (true) {
            System.out.println("Enter source folder (default "+sourceFolder+") (or 'end' to quit):");
            String sourceFolderSrc = scanner.nextLine().trim();
            if(sourceFolderSrc.isBlank() && sourceFolder.isBlank()){
                continue;
            }
            sourceFolder =sourceFolderSrc.isEmpty() ? sourceFolder : sourceFolderSrc.trim();
            if ("end".equalsIgnoreCase(sourceFolder)) {
                break;
            }

            System.out.println("Enter destination folder (default "+destinationFolder+"):");
            String destinationFolderSrc = scanner.nextLine().trim();
            if(destinationFolderSrc.isBlank() && destinationFolder.isBlank()){
                continue;
            }
            destinationFolder =destinationFolderSrc.isEmpty() ? destinationFolder : destinationFolderSrc.trim();

            System.out.println("Enter number of threads (default "+threadCount+"):");
            String threadCountStr = scanner.nextLine().trim();
            
            try {
                threadCount = threadCountStr.isEmpty() ? threadCount : Integer.parseInt(threadCountStr);
                if (threadCount <= 0) {
                    throw new IllegalArgumentException("Thread count must be positive.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid thread count format. Using default (10).");
                threadCount = 10;
            }

            System.out.println("Do you want to Move the File Y/N (default "+ (moveFile?"Y":"N") +"):");
            String moveFileStr = scanner.nextLine().trim();
            moveFile = moveFileStr.isEmpty() ? moveFile : moveFileStr.toUpperCase().trim().equalsIgnoreCase("Y")?true:false;

            try {
                MediaSorter.sortMedia(sourceFolder, destinationFolder, threadCount,moveFile);
            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
        }

        scanner.close();
    }

}

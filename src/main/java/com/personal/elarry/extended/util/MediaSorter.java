package com.personal.elarry.extended.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;

@Slf4j
public class MediaSorter implements CommandLineRunner{
    //private static final String SOURCE_FOLDER = "/Volumes/Transcend/2023 MOVIE";
    //private static final String DESTINATION_FOLDER = "/Volumes/Transcend/NEW_2023_MOVIE";
    //private static final Pattern YEAR_PATTERN = Pattern.compile("\\([0-9]{4}\\)");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(?<!\\d)(?:19|20)\\d{2}\\b");

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
                sortMedia(sourceFolder, destinationFolder, threadCount,moveFile);
            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
        }

        scanner.close();
    }


    public static void sortMedia(String sourceFolder, String destinationFolder, int threadCount,boolean moveFile) throws IOException {
        File sourceDir = new File(sourceFolder);
        File destinationDir = new File(destinationFolder);

        // Validate input folders
        if (!sourceDir.exists()) {
            log.info("Source folder does not exist.");
            return;
        }

        if (!sourceDir.isDirectory()) {
            log.info("Source path is not a directory.");
            return;
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs(); // Create destination folder if it doesn't exist
        }

        if (!sourceDir.exists()) {
            log.info("Source folder " + sourceDir.getAbsolutePath() + " does not exist.");
            return;
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs(); // Create destination folder if it doesn't exist
        }

        File[] files = sourceDir.listFiles();

        if (files == null) {
            log.info("Source folder is empty.");
            return;
        }
        for (File file : files) {
            log.info("+++"+file.getAbsolutePath());
        }

        // Sort files by extracted year or last modified date
        Arrays.sort(files, new Comparator<File>() {
            private long lastModified;

            @Override
            public int compare(File file1, File file2) {
                log.info("Comparing "+file1.getAbsolutePath()+" and "+ file2.getAbsolutePath());
                int year1 = extractYear(file1);
                int year2 = extractYear(file2);

                if (year1 != -1 && year2 != -1) {
                    return year1 - year2; // Sort by extracted year if available
                } else {
                    long millisecondsInYear = (long) (365 * 24 * 60 * 60 * 1000);
                    if(year1 != -1){
                        lastModified = file2.lastModified();
                        year2 = (int) (lastModified / millisecondsInYear);
                        return year1 - year2; 
                    }else if(year2 != -1){
                        lastModified = file1.lastModified();
                        year1 = (int) (lastModified / millisecondsInYear);
                        return year1 - year2; 
                    }else{
                        log.info(file1.getAbsolutePath()+" was modified "+file1.lastModified());
                        log.info(file2.getAbsolutePath()+" was modified "+file2.lastModified());
                        return Long.compare(file1.lastModified(), file2.lastModified()); // Sort by last modified date
                    }
                }
            }
        });
        for (File file : files) {
            log.info("After+++"+file.getAbsolutePath());
        }
        int count= 0;
        final int total= files.length;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (File file : files) {
            count++;
            final int counta= count;

            try{
                executorService.submit(() -> {
                    try {
                        // Simulate processing by sleeping for 2 seconds
                        log.info("Start processing " + counta);
                        //Thread.sleep(processingTimeMillis);


                        log.info(String.format("%s out of %s for file %s",counta,total,file));
                        int year = extractYear(file);
                        String destinationPath;

                        if (year != -1) {
                            destinationPath = Paths.get(destinationFolder, String.valueOf(year)).toString();
                        } else {
                            destinationPath = destinationFolder; // Use default destination if year can't be extracted
                        }

                        File destinationPath2 = new File(destinationPath);
                        if (!destinationPath2.exists()) {
                            destinationPath2.mkdirs();
                        }


                        Path source = file.toPath();
                        Path destination = Paths.get(destinationPath, file.getName());

                        //  Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        log.info(counta+(moveFile? ">>Moveing " : ">>Copying " )+ file.getName() + " to " + destination);
                        // Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        try {
                            if(file.isDirectory()) {
                                if (destination.toFile().exists()) {
                                    FileUtils.deleteDirectory(destination.toFile()); // Delete existing directory
                                }
                                if(moveFile){
                                    FileUtils.moveDirectory(file, destination.toFile());
                                }else{
                                    FileUtils.copyDirectory(file, destination.toFile());
                                }
                                log.info("Successfully copied directory from " + file + " to " + destination);
                            }else{
                                if (destination.toFile().exists()) {
                                    FileUtils.forceDelete(destination.toFile()); // Delete existing file
                                }
                                if(moveFile){
                                    FileUtils.moveFile(file, destination.toFile());
                                }else{
                                    FileUtils.copyFile(file, destination.toFile());
                                }
                                log.info("Successfully copied file from " + file + " to " + destination);
                            }

                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                        //System.out.println("Moved " + file.getName() + " to " + destination);





                        log.info("After Sleep " + counta);
                        // Decrement the count when processing is complete
                        //latch.countDown();
                        log.info("Done processing " + counta);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }catch (Exception ex){
                log.error(ex.getMessage(), ex);
            }

        }
         // Wait for all tasks to complete
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
        }

        log.info("All media files have been sorted successfully.");

        
    }

    private static int extractYearOld(File file) {
        Matcher matcher = YEAR_PATTERN.matcher(file.getName());
        try {
            if (matcher.find()) {
                //String yearStr = matcher.group(1).substring(1, 5); // Extract year substring
                String yearStr = matcher.group(0).substring(1, 5); // Extract year substring
                try {
                    log.info(">>>>"+file.getName()+":::"+yearStr);
                    return Integer.parseInt(yearStr);
                } catch (NumberFormatException e) {
                    log.info("Error parsing year from filename: " + file.getName());
                    return -1;
                }
            }
        }catch (Exception e){
            log.info(file.getName());
            log.error(e.getMessage(), e);
        }
        return -1; // Year not found in filename
    }

    private static int extractYear(File file) {
        Matcher matcher = YEAR_PATTERN.matcher(file.getName());
        if (matcher.find()) {
            String yearStr = matcher.group(0); // Extract the entire matched year
            try {
                log.info(new Date().toString()+">>>>"+file.getName()+":::"+yearStr);
                return Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                System.err.println(new Date().toString()+"Error parsing year from filename: " + file.getName());
                return -1;
            }
        }
        return -1; // Year not found in filename
    }
    
}

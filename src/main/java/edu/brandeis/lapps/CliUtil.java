package edu.brandeis.lapps;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CliUtil {

    public static class ProcessorThread extends Thread {

        BrandeisService service;
        File inf;
        Path outf;

        public ProcessorThread(BrandeisService service, File inFname, Path outFname) {
            this.service = service;
            this.inf = inFname;
            this.outf = outFname;
        }
        public void run() {
            try {
                System.out.println("processing " + this.inf.getName());
                Files.write(this.outf, processInputStream(service, new FileInputStream(this.inf)).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void processDirectory(BrandeisService tool, Path inDir, String annName) throws IOException {
        processDirectory(tool, inDir, annName, 8);
    }

    public static void processDirectory(BrandeisService tool, Path inDir, String annName, int threadpoolsize) throws IOException {
        Path outDir = Paths.get(inDir.toString(),
                annName + DateTimeFormatter.ofPattern("-yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC).format(Instant.now()));
        Files.createDirectory(outDir);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadpoolsize);
        for (File lifFile : Objects.requireNonNull(inDir.toFile().listFiles((dir, name) -> name.charAt(0) != '.' && name.endsWith(".lif")))) {
            Path outFile = outDir.resolve(Paths.get(lifFile.getName()).getFileName());
            ProcessorThread th = new ProcessorThread(tool, lifFile, outFile);
            executor.execute(th);
        }
    }

    public static String processInputStream(BrandeisService tool, InputStream is) throws IOException {
        ByteArrayOutputStream toString = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];
        int length;
        while ((length = is.read(buffer)) != -1) {
            toString.write(buffer, 0, length);
        }
        String inString = toString.toString("UTF-8");
        return tool.execute(inString);
    }

}

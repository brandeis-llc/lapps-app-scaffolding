package edu.brandeis.lapps;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CliUtil {

    public static void processDirectory(BrandeisService tool, Path inDir, String annName) throws IOException {
        Path outDir = Paths.get(inDir.toString(),
                annName + DateTimeFormatter.ofPattern("-yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC).format(Instant.now()));
        Files.createDirectory(outDir);
        for (File lifFile : Objects.requireNonNull(inDir.toFile().listFiles((dir, name) -> name.charAt(0) != '.' && name.endsWith(".lif")))) {
            Path outFile = outDir.resolve(Paths.get(lifFile.getName()).getFileName());
            java.nio.file.Files.write(outFile, processInputStream(tool, new FileInputStream(lifFile)).getBytes());
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

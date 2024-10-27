package idetector.util;

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.StreamReader;
import idetector.config.GlobalConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;


@Slf4j
public class FileUtils {


    public static void getTargetDirectoryJarFiles(String target, boolean checkFatJar, Map<String, String> jarPaths, Map<String, String> daoPaths, Map<String,String> packMap, boolean isExtractLib) throws IOException {
        Path path = Paths.get(target).toAbsolutePath();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Invalid jar path: " + path);
        }

        if(path.toFile().isFile()){
            String filename = path.toFile().getName();
            String fileMd5 = FileUtils.getFileMD5(path.toFile());
            if(filename.endsWith(".class")){
                jarPaths.put(fileMd5, path.getParent().toString());
            }else if (filename.endsWith(".jar") || filename.endsWith(".war")){

                if (filename.endsWith(".jar")) {
                    jarPaths.put(fileMd5, path.toAbsolutePath().toString());
                }
                unpackWarOrJarFiles(checkFatJar, path, filename, jarPaths, daoPaths, packMap, isExtractLib);
            }
        }else{
            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                    if (path.toString().contains(".git")) {
                        return FileVisitResult.CONTINUE;
                    }
                    String filename = path.getFileName().toString();
                    String fileMd5 = FileUtils.getFileMD5(path.toFile());
                    if(filename.endsWith(".class")) {
                        jarPaths.put(fileMd5, path.getParent().toString());
                    } else if(filename.endsWith(".jar") || filename.endsWith(".war")){

                        if (filename.endsWith(".jar")) {
                            jarPaths.put(fileMd5, path.toAbsolutePath().toString());
                        }
                        unpackWarOrJarFiles(checkFatJar, path, filename, jarPaths, daoPaths, packMap, isExtractLib);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }


    public static boolean isFatJar(Path path){
        String file = path.toAbsolutePath().toString();
        try {
            JarFile jarFile = new JarFile(path.toFile());
            if(jarFile.getEntry("WEB-INF") != null
                || jarFile.getEntry("BOOT-INF") != null){
                return true;
            }else{
                return false;
            }
        }
        catch (Exception e) {
            log.error("Something error with func.dealFatJar <{}>, just add this jar", file);
            return false;
        }
    }


    public static String getMapperLoc(Path baseDir, String targetDir) {
        try{
            String[] resultPath = new String[1];
            if(baseDir.resolve(targetDir).toFile().exists()){
                Files.walkFileTree(baseDir.resolve(targetDir), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString();
                        if (fileName.equals("application.properties")) {
                            log.debug("Found application configuration file {}", fileName);
                            Properties props = new Properties();
                            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                                props.load(inputStream);
                            }
                            String mapperLocations = props.getProperty("mybatis.mapper-locations");
                            resultPath[0] = mapperLocations;
                            return FileVisitResult.TERMINATE;
                        } else if (fileName.equals("application")) {
                            log.debug("Found application configuration file {}", fileName);
                            Yaml yaml = new Yaml();
                            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                                Map<String, Object> obj = yaml.load(inputStream);
                                Map<String, Object> mybatis = (Map<String, Object>) obj.get("mybatis");
                                if (mybatis == null) {
                                    mybatis = (Map<String, Object>) obj.get("mybatis-plus");
                                }
                                String mapperLocations = (String) mybatis.get("mapper-locations");
                                resultPath[0] = mapperLocations;
                                return FileVisitResult.TERMINATE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            String mapperLoc = resultPath[0];
            if (mapperLoc == null) {
                return "";
            }
            if (mapperLoc.contains(":")) {
                mapperLoc = mapperLoc.split(":")[1];
            }
            mapperLoc = new File(targetDir, mapperLoc).toString();
            return mapperLoc;
        }
        catch (Exception e) {
            log.debug(e.toString());
            return "";
        }
    }

    public static void unpackWarOrJarFiles(Boolean checkFatJar, Path path, String filename, Map<String, String> jarPaths, Map<String, String> daoPaths, Map<String,String> packMap, boolean isExtractLib) throws IOException {


        Path tmpDir = registerTempDirectory(filename);

        extract(path, tmpDir);


        if (path.toString().endsWith(".jar") || path.toString().endsWith(".war"))
            findPackages(path.toAbsolutePath().toString(), packMap);


        String mapperLoc = getMapperLoc(tmpDir, "BOOT-INF/classes");
        if (mapperLoc.equals("")) {
            daoPaths.putAll(findDaoTargets(tmpDir, "*.xml"));
        } else {
            daoPaths.putAll(findDaoTargets(tmpDir, mapperLoc));
        }


        if(!checkFatJar || !isFatJar(path)){
            String fileMd5 = FileUtils.getFileMD5(path.toFile());

            jarPaths.putIfAbsent(fileMd5, path.toAbsolutePath().toString());
            return;
        }


        for (Map.Entry<String, String> entry: findLibTargets(tmpDir, "BOOT-INF/lib", packMap, isExtractLib).entrySet()) {
            jarPaths.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry: findLibTargets(tmpDir, "WEB-INF/lib", packMap, isExtractLib).entrySet()) {
            jarPaths.putIfAbsent(entry.getKey(), entry.getValue());
        }

        String bootInfClassesDir = String.join(File.separator, tmpDir.toString(), "BOOT-INF/classes");
        String webInfClassesDir = String.join(File.separator, tmpDir.toString(), "WEB-INF/classes");

        if(new File(bootInfClassesDir).exists()){
            jarPaths.put(filename+"_bootinf_classes_"+ RandomStringUtils.randomAlphanumeric(5),
                    bootInfClassesDir);
        }
        if(new File(webInfClassesDir).exists()){
            jarPaths.put(filename+"_webinf_classes_"+ RandomStringUtils.randomAlphanumeric(5),
                    webInfClassesDir);
        }
    }


    public static Map<String, String> findLibTargets(Path baseDir, String targetDir, Map<String,String> packMap, boolean isExtractLib) throws IOException {
        Map<String, String> paths = new HashMap<>();
        if(baseDir.resolve(targetDir).toFile().exists()){
            Files.walkFileTree(baseDir.resolve(targetDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filepath = file.toAbsolutePath().toString();
                    String fileMd5 = FileUtils.getFileMD5(filepath);
                    if(filepath.endsWith(".jar")){
                        if (isExtractLib) {

                            findPackages(filepath, packMap);
                        }
                        paths.put(fileMd5, filepath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return paths;
    }


    public static void findPackages(String jarPath, Map<String,String> packMap) throws IOException {
        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String word = entry.getName().split("\\.")[0];
                word = word.split("\\$")[0];
                String[] items = word.split("/");
                String className = items[0];
                Integer length = items.length > 3 ? 3: items.length;
                for (int i = 1; i < length; i++) {
                    className += "." + items[i];
                }
                packMap.putIfAbsent(className, jarPath);
            }
        }
        jarFile.close();
    }


    public static Map<String, String> findDaoTargets(Path baseDir, String virtualPath) throws IOException {
        Map<String, String> paths = new HashMap<>();
        if (fileExists(new File(baseDir.toString(), virtualPath).getPath())) {
            String filePath = new File(baseDir.toString(), virtualPath).getPath();
            String fileMd5 = FileUtils.getFileMD5(filePath);
            paths.put(fileMd5, filePath);
            return paths;
        }

        String targetDir = "";
        String[] fileItems = virtualPath.split(File.separator.replace("\\", "\\\\").replace("/", "\\\\"));
        if (fileItems.length > 1) {
            for (int i = 0; i < fileItems.length - 1; i++) {
                if (!fileItems[i].contains("*")) {
                    targetDir += fileItems[i] + File.separator;
                }
            }
        }
        String matchPattern = virtualPath.replace("\\", "\\\\").replace("/", "\\\\").replace(".", "\\.").replace("*",".*");
        if(baseDir.resolve(targetDir).toFile().exists()){
            Files.walkFileTree(baseDir.resolve(targetDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = baseDir.relativize(file.toAbsolutePath()).toString();
                    if (relativePath.matches(matchPattern)) {
                        String filePath = file.toAbsolutePath().toString();
                        String fileMd5 = FileUtils.getFileMD5(filePath);
                        paths.put(fileMd5, filePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return paths;
    }

    public static void extract(Path jarPath, Path tmpDir) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarPath))) {
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                Path fullPath = tmpDir.resolve(jarEntry.getName());
                if (!jarEntry.isDirectory()
                    && (jarEntry.getName().endsWith(".class")
                        || jarEntry.getName().endsWith(".jar")
                        || jarEntry.getName().endsWith(".yml")
                        || jarEntry.getName().endsWith(".properties")
                        || jarEntry.getName().endsWith(".xml"))) {
                    Path dirName = fullPath.getParent();
                    if (dirName == null) {
                        throw new IllegalStateException("Parent of item is outside temp directory.");
                    }
                    if (!Files.exists(dirName)) {
                        Files.createDirectories(dirName);
                    }
                    try (OutputStream outputStream = Files.newOutputStream(fullPath)) {
                        copy(jarInputStream, outputStream);
                    }
                }
            }
        }
    }

    /**
     * Copy inputStream to outputStream. Neither stream is closed by this method.
     */
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[4096];
        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, n);
        }
    }

    public static boolean fileExists(String path){
        File file = new File(path);
        return file.exists();
    }


    public static Object getJsonContent(String path, Class<?> type){
        File file = new File(path);
        if(!file.exists()) return null;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            return GlobalConfiguration.GSON.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void putJsonContent(String path, Object data){
        File file = new File(path);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(GlobalConfiguration.GSON.toJson(data));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Path registerTempDirectory(String directory) throws IOException {

        final Path tmpDir = Files.createTempDirectory(directory);
        // Delete the temp directory at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteDirectory(tmpDir);
            } catch (IOException e) {
                log.error("Error cleaning up temp directory " + tmpDir.toString(), e);
            }
        }));

        return tmpDir;
    }

    /**
     * Recursively delete the directory root and all its contents
     * @param root Root directory to be deleted
     */
    public static void deleteDirectory(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void createDirectory(String path){
        File file = new File(path);
        if(file.mkdirs()){
            log.info("Create directory {} success!", path);
        }
    }

    public static String getWinPath(String path){
        if(JavaVersion.isWin()){
            path = "/"+path.replace("\\", "/");
        }
        return path;
    }

    public static String getFileMD5(String filepath){
        return getFileMD5(new File(filepath));
    }

    public static String getFileMD5(File file){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(file.getName().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

}

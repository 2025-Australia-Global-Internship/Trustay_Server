package com.maritel.trustay.util;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class FileUtils {

    // 바이트 변환
    public static long convertByte(long size, String format) {
        /* Byte Kb Mb Gb Tb*/
        long K = 1024;
        long M = K * K;
        long G = M * M;
        long T = G * G;
        long resultByte = 0;

        if (format != null) {

            if (format.toUpperCase().equals("KB")) {
                resultByte = size * K;
            } else if (format.toUpperCase().equals("MB")) {
                resultByte = size * M;
            } else if (format.toUpperCase().equals("GB")) {
                resultByte = size * G;
            } else if (format.toUpperCase().equals("TB")) {
                resultByte = size * T;
            }
        } else {
            resultByte = size;
        }
        return resultByte;
    }

    // 확장자
    public static String extension(String contentType) {

        // org.apache.tika core library
        MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
        MimeType extension = null;
        try {
            extension = mimeTypes.forName(contentType);
        } catch (MimeTypeException e) {
            throw new RuntimeException(e.getMessage());
        }
        return extension.getExtension();
    }

    // 확장자
    public static String filename(String url) {

        return url.substring(url.lastIndexOf("/"));
    }

    // 확장자 유효성 검사 (엑셀 파일)
    public static boolean isExcelExt(String extension) {
        String[] possibleExts = {"xls", "xlsx"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 확장자 유효성 검사 (이미지 파일)
    public static boolean isImageExt(String extension) {
//    String[] possibleExts = {"gif", "jpeg", "jpg", "png", "svg", "blob"};
        String[] possibleExts = {"jpeg", "jpg", "png"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 확장자 유효성 검사 (이미지 파일)
    public static boolean isFaviconExt(String extension) {
        String[] possibleExts = {"ico"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 확장자 유효성 검사 (일반 파일)
    public static boolean isFileExt(String extension) {
        String[] possibleExts = {"gif", "jpeg", "jpg", "png", "svg", "blob", "ppt", "pdf", "zip", "xlsx", "xls", "mp4", "ogg", "mpeg"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 사업자등록증 확장자 유효성 검사 (일반 파일)
    public static boolean isBusiFileExt(String extension) {
        String[] possibleExts = {"jpeg", "jpg", "png", "pdf"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 확장자 유효성 검사 (비디오 파일)
    // • MPEG-4 (.mp4, .m4a)
    public static boolean isMP4Ext(String extension) {
        String[] possibleExts = {"mp4", "m4a"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHtmlExt(String extension) {
        String possibleExts = "html";

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }


        if (extension.toLowerCase().equals(extension.toLowerCase())) {
            return true;
        }

        return false;
    }

    // 확장자 유효성 검사 (아이콘 파일)
    public static boolean isIconExt(String extension) {
        String[] possibleExts = {"ico"};

        if (extension.startsWith(".")) {
            extension = extension.replace(".", "");// '.'remove
        }

        for (String possibleExt : possibleExts) {
            if (possibleExt.toLowerCase().equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 랜덤 파일이름
    public static String getRandomFileName(String prefix, String extension) {
        String date = new SimpleDateFormat("yymmddhhmmss").format(new Date());
        return prefix + date + new Random().nextInt(100000) + extension;
    }

    // 경로
    public static String getDirectory(String... tails) {

        StringBuffer path = new StringBuffer();

        if (tails.length != 0 && !tails[0].startsWith("/")) {
            path.append(File.separator);
        }

        for (String directory : tails) {
            path.append(directory + File.separator);
        }
        return path.toString();
    }

    // 리소스 파일 경로 (스프링)
    public static String getResourcePath(String... tails) {
        return "src/main/resources" + FileUtils.getDirectory(tails);
    }

    // 파일 경로 (톰캣)
    public static String getRootPath() {
        return System.getProperty("catalina.home") + File.separator + "webapps" + File.separator;
    }

    public static String getUserHomePath() {
        return System.getProperty("user.home") + File.separator;
    }

    // VM Server 파일 업로드 경로  /usr/share/nginx/html/images/{앱이름}
    public static String getVmUploadPath(String dirName) {
        return File.separator + "usr" + File.separator + "share" + File.separator + "nginx" + File.separator + "html" + File.separator + "images" + File.separator + dirName + File.separator;
    }

    public static InputStream getInputStream(URL url) throws IOException {
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        return con.getInputStream();
    }

    // OS X (mac)은 파일명을 NFD 방식으로 입력한다.
    public static String getFileNameNFC(String str) {

        String result = str;

        if (Normalizer.isNormalized(str, Normalizer.Form.NFD)) {
            result = Normalizer.normalize(str, Normalizer.Form.NFC);
        }
        return result;
    }

    public static String getFileString(String path) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getStreamString(InputStream inputStream) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static List<String> listFilesAndFolders(String path) {
        List<String> listFilesAndFolders = new ArrayList<>();
        File directory = new File(path);
        for (File file : directory.listFiles()) {
            listFilesAndFolders.add(file.getName());
        }
        return listFilesAndFolders;
    }

    public static List<String> listFiles(String path) {
        List<String> listFiles = new ArrayList<>();
        File directory = new File(path);
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                listFiles.add(file.getName());
            }
        }
        return listFiles;
    }

    public static List<String> listFolders(String path) {
        List<String> listFiles = new ArrayList<>();
        File directory = new File(path);
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                listFiles.add(file.getName());
            }
        }
        return listFiles;
    }


    public static String getFileText(String path) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException ie) {
            return null;
        }
    }

    public static void hasDirectoryAndMkDir(String directory) {
        File file = new File(directory);
        try {
            boolean exist = file.exists();
            if (!exist) {
                boolean mkdir = file.mkdir();
                System.out.println(mkdir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String getDisposition(String filename, String browser) throws Exception {
        String dispositionPrefix = "attachment;filename=";
        String encodedFilename = null;

        if (browser.equals("MSIE")) {

            encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        } else if (browser.equals("Firefox")) {

            encodedFilename = new String(filename.getBytes("UTF-8"), "8859_1");

        } else if (browser.equals("Opera")) {

            encodedFilename = new String(filename.getBytes("UTF-8"), "8859_1");
        } else if (browser.equals("Chrome")) {

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < filename.length(); i++) {
                char c = filename.charAt(i);
                if (c > '~') {
                    sb.append(URLEncoder.encode("" + c, "UTF-8"));
                } else {
                    sb.append(c);
                }
            }
            encodedFilename = sb.toString();
        } else {
            throw new RuntimeException("Not supported browser");
        }

        return dispositionPrefix + encodedFilename;
    }

    public static String getUrlEmbedVideo(String linkUrl) {
        final String startWithVimeo = "https://vimeo.com/channels/staffpicks/";
        final String startWithYouTube = "https://youtu.be/";
        StringBuffer result = new StringBuffer();


        if (linkUrl.startsWith(startWithVimeo)) {
            result.append("https://player.vimeo.com/video/");
            result.append(linkUrl.substring(startWithVimeo.length(), linkUrl.length()));
        } else if (linkUrl.startsWith(startWithYouTube)) {
            result.append("https://www.youtube.com/embed/");
            result.append(linkUrl.substring(startWithYouTube.length(), linkUrl.length()));
        } else {
            return linkUrl;
        }

        return result.toString();

    }

}

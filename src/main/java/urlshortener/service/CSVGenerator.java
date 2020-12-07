package urlshortener.service;

import org.springframework.web.multipart.MultipartFile;

import urlshortener.domain.ShortURL;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.File;

import urlshortener.service.ShortURLService;

public class CSVGenerator {

    public static boolean checkCSV(String url) {
        if(url.matches("^(https|http)://[A-Za-z0-9_\\.\\-~/]+$")) {
            return true;
        }
        return false;
    }

    public static String readLine(String line) {
        if(checkCSV(line)) {
            return line;
        } else {
            return line + ",,Debe ser una URI http o https válida";
        }
    }

    public static ArrayList<String> readCSV(MultipartFile file) {
        ArrayList<String> lines = new ArrayList<String>();
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                ByteArrayInputStream inputFilestream = new ByteArrayInputStream(bytes);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputFilestream));
                String line = "";
                int i = 0;
                while ((line = br.readLine()) != null && i < 500) {
                    lines.add(readLine(line));
                    ++i;
                }
                br.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return lines;
    }

    public static ArrayList<Object> writeCSV(ArrayList<String> lines, String ip, ShortURLService sus) {
        ArrayList<Object> pair = new ArrayList<Object>();
        File f = new File("ShortURL.csv");
        try (FileWriter fw = new FileWriter(f)) {
            ArrayList<ShortURL> sh = new ArrayList<ShortURL>();
	        for(String l : lines) {
                if(l.contains(",,")) {
                    fw.write(l + "\n");
                } else {
                    ShortURL su = sus.save(l, "", ip);
                    String aux = su.getUri().toString();
                    fw.write(l + "," + aux.substring(0, aux.lastIndexOf("/")) + "/sh.html?id=" + su.getHash() + ",\n");
                    sh.add(su);
                }
            }
            pair.add(sh);
        } catch(Exception e) {
            e.printStackTrace();
        }
        pair.add(f);
        return pair;
    }

}
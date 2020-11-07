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
                    lines.add(line);
                    ++i;
                }
                br.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return lines;
    }

    public static File writeCSV(ArrayList<String> lines, String ip, ShortURLService sus) {
        File f = new File("ShortURL.csv");
        try (FileWriter fw = new FileWriter(f)) {
	        for(String l : lines) {
                ShortURL su = sus.save(l, "", ip);
                fw.write(l + "," + su.getUri().toString() + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return f;
    }

}
package org.osate2.bayes.dtbn.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/** File operation **/
public class FileOp {

    /** Read lines form file **/
    public static ArrayList<String> readLines(String path) throws IOException {
        FileInputStream inputStream = new FileInputStream(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        ArrayList<String> lines = new ArrayList<>();
        String str;
        while((str = bufferedReader.readLine()) != null) {
			lines.add(str);
		}

        // close
        inputStream.close();
        bufferedReader.close();
        return lines;
    }

    /** Write file **/
    public static void writeFile(String path, String content) throws IOException {
        File file = new File(path);

        // if file doesn't exist, create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.close();
    }

}

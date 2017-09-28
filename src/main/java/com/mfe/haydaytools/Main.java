package com.mfe.haydaytools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by chenmin on 2017/3/30.
 */
public class Main {
    private static Logger log;
    private static JsonFactory jsonFactory = new JsonFactory();

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.getConfiguration().getRootLogger().setLevel(System.getProperty("logLevel", "info").equals("debug") ? Level.DEBUG : Level.INFO);
        log = LogManager.getLogger(Main.class);
        //System.out.println("args.len:"+args.length);
        //System.out.println(args);
        String jsonFile=null;
        String destFile=null;
        if(args.length>0){
            jsonFile=args[0];
        }
        if(args.length>1){
            destFile=args[1];
        }
        genXls(jsonFile, null==destFile ? "./gen.csv" : destFile);
    }


    private static void genXls(String jsonFile, String filename) throws IOException {
        String line=null;
        Path dest=Paths.get(filename);
        Files.deleteIfExists(dest);
        Files.createFile(dest);
        BufferedWriter writer=Files.newBufferedWriter(dest, Charset.forName("utf-8"), StandardOpenOption.APPEND);
        //wirte title
        writer.write("No.,Name,Level,barnCur,barnMax,siloCur,siloMax");
        writer.newLine();

        if(null!=jsonFile){
            ObjectMapper mapper = new ObjectMapper();
            File from = new File(jsonFile);
            TypeReference<HashMap<String,Object>> typeRef
                    = new TypeReference<HashMap<String,Object>>() {};

            HashMap<String,Object> o = mapper.readValue(from, typeRef);
            //System.out.println("Got " + o);
            //System.out.println("Clients:"+o.get("clients"));
            List<Map<String, Object>> arr=(List<Map<String, Object>>)(o.get("clients"));
            int i=1;
            for(Map<String, Object> pp : arr){
                //System.out.println("name:"+pp.get("name"));
                line=genLine(Paths.get((String)(pp.get("name"))));
                //System.out.println(line);
                if(null!=line){
                    line=""+(i++)+","+line;
                    writer.write(line);
                    writer.newLine();
                }
            }
            writer.close();
            return;
        }
        DirectoryStream.Filter<Path> account_dir_filter = new DirectoryStream.Filter<Path>() {

            public boolean accept(Path path) throws IOException {
                //System.out.println("getFileName:"+path.getFileName());
                return (Files.isDirectory(path) && !path.getFileName().toString().startsWith("info"));
            }
        };
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("."), account_dir_filter)) {

            int i=1;
            for (Path file : ds) {
                line=genLine(file);
                //System.out.println(line);
                if(null!=line){
                    line=""+(i++)+","+line;
                    writer.write(line);
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static String genLine(Path dir) throws IOException {
        Path levelF=dir.resolve("level.txt");
        Path sizeF=dir.resolve("storageSize.json");
        if (Files.notExists(levelF) || Files.notExists(sizeF)){
            //System.out.println("\""+dir.getFileName() +"\" is not valid");
            return null;
        }
        String level=new String(Files.readAllBytes(levelF));
        JsonParser jsonParse = jsonFactory.createParser(sizeF.toFile());
        StringBuilder rlt= new StringBuilder(dir.getFileName() + "," + level + ",");
        while(jsonParse.nextToken() != JsonToken.END_OBJECT){
            String fieldName = jsonParse.getCurrentName();
            if("barnCur".equals(fieldName) || "barnMax".equals(fieldName) || "siloCur".equals(fieldName)){
                jsonParse.nextToken();
                //System.out.println(jsonParse.getIntValue());
                rlt.append(jsonParse.getIntValue()).append(",");
            }
            if("siloMax".equals(fieldName)){
                jsonParse.nextToken();
                //System.out.println(jsonParse.getIntValue());
                rlt.append(jsonParse.getIntValue());
                jsonParse.close();
                return rlt.toString();
            }
        }
        jsonParse.close();
        return rlt.toString();
    }
}

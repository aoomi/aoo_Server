package com.ddm.server.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class BinLogAnalysiser {

    private static Map<String, Map<Long, Long>> analysie = new TreeMap<>();
    private static int timestamp = 0;

    public static void main(String[] args) throws Exception {
        File root = new File("./");
        long count = 0;
        long filecount = 0;
        for (File file : root.listFiles()) {
            if (!file.getName().endsWith(".txt")) {
                continue;
            }
            filecount++;
            System.out.println("analysie file:" + file.getName() + "   count:" + filecount);
            try (BufferedReader buffered = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                String line = null;
                while ((line = buffered.readLine()) != null) {
                    record(line);
                    count++;
                    if (count % 100_0000 == 0) {
                        System.out.println("analysise count:" + count);
                    }
                }
            }
        }
        System.out.println("analysise over, total:" + count);
        System.out.println("start output to analysie.out...");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (var output = Files.newBufferedWriter(new File("analysie.out").toPath(), StandardCharsets.UTF_8)) {

            for (Entry<String, Map<Long, Long>> pair : analysie.entrySet()) {
                int amount = pair.getValue().values().stream().mapToInt(x -> x.intValue()).sum();
                StringBuilder head = new StringBuilder();
                StringBuilder line = new StringBuilder();
                head.append(pair.getKey()).append('\t');
                line.append(amount).append('\t');
                for (Entry<Long, Long> entry : pair.getValue().entrySet()) {
                    head.append(sdf.format(new Date(entry.getKey() * 1000))).append("\t");
                    line.append(entry.getValue()).append("\t");
                    amount += entry.getValue();
                }
                head.append('\n');
                line.append('\n');
                output.write(head.toString());
                output.write(line.toString());
            }
        }
        System.out.println("output to analysie.out over!");
    }

    private static void record(String line) {
        String start = null;
        if (line.startsWith("SET TIMESTAMP=")) {
            timestamp = Integer.parseInt(line.replace("SET TIMESTAMP=", "").replace("/*!*/;", ""));
            return;
        } else if (line.startsWith("update")) {
            String[] split = line.split(" ", 3);
            start = split[1].replace("`", "").replace(",", "") + ' ' + split[0]; // update table set ****
        } else if (line.startsWith("insert")) {
            String[] split = line.split(" ", 4);
            start = split[2].replace("(`id`", "").replace("(`ID`", "").replace("`", "").replace(",", "") + ' ' + split[0];// insert-into-table(xxxx)-xxxx
        } else if (line.startsWith("delete")) {
            String[] split = line.split(" ", 4);
            start = split[2].replace("`", "") + ' ' + split[0];// delete from table where
        }
        if (start == null) {
            return;// 本行不是sql
        }
        Map<Long, Long> detail = analysie.get(start);
        if (detail == null) {
            analysie.put(start, detail = new TreeMap<>());
        }
        long time = (long) timestamp;
        Long before = detail.get(time);
        detail.put(time, before == null ? 1L : (before + 1L));
    }
}
